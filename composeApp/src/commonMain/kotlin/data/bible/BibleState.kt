package data.bible

import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import data.config.CloudSessionManager
import data.GeminiModel
import data.analytics.AnalyticsTracker
import data.appPrefs.ReadingSession
import data.notifications.ScriptureDeepLink
import data.notifications.bookNumberForUsfm
import data.notifications.usfmForBookNumber
import data.plans.ActivePlanReadingContext
import email.kevinphillips.biblebible.BuildKonfig
import email.kevinphillips.biblebible.db.SelectReadingHistory
import email.kevinphillips.biblebible.isDesktopPlatform
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.Channel
import kotlinx.datetime.LocalDate
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
internal object BibleState {
    val RELEASE_BUILD: Boolean get() = !BuildKonfig.ENABLE_DIAGNOSTIC_LOGGING
    const val DEFAULT_BIBLE_ID = "kjv"
    val singleChapterBooksOrdinal = setOf("31", "57", "63", "64", "65")

    private val pentateuch = setOf(1, 2, 3, 4, 5)
    private val historicalBooks = setOf(6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)
    private val poetryAndWisdom = setOf(18, 19, 20, 21, 22)
    private val majorProphets = setOf(23, 24, 25, 26, 27)
    private val minorProphets = setOf(28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39)
    private val gospels = setOf(40, 41, 42, 43)
    private val acts = setOf(44)
    private val paulineEpistles = setOf(45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
    private val generalEpistles = setOf(58, 59, 60, 61, 62, 63, 64, 65)
    private val apocalypticLiterature = setOf(66)

    // Brightened jewel-tone palette harmonized with the navy base for crisp readability
    private val lightGreen = Color(0xFF3E8E89)    // Pentateuch — Luminous Seafoam Teal
    private val lightBlue = Color(0xFF3B76B0)     // Historical — Bright Sapphire Blue
    private val yellow = Color(0xFFB59442)        // Poetry & Wisdom — Warm Sunlit Gold
    private val orange = Color(0xFFB37044)        // Major Prophets — Bright Copper Amber
    private val lightPurple = Color(0xFF7B5FA8)   // Minor Prophets — Vibrant Royal Plum
    private val red = Color(0xFFB04F66)           // Gospels — Bright Crimson Rose
    private val lightRed = Color(0xFFC46854)      // Acts — Warm Terracotta Coral
    private val darkBlue = Color(0xFF4A6BB0)      // Pauline Epistles — Bright Cobalt
    private val darkGreen = Color(0xFF458C6E)     // General Epistles — Vibrant Emerald
    private val darkPurple = Color(0xFF724B99)    // Revelation — Luminous Amethyst
    private val defaultColor = Color(0xFF4F7294)  // Default — Bright Slate Blue

    @Composable
    fun getBibleBookColor(ordinal: Int): Color = when (ordinal) {
        in pentateuch -> lightGreen
        in historicalBooks -> if (sortAZ) MaterialTheme.colors.primary else lightBlue
        in poetryAndWisdom -> if (sortAZ) MaterialTheme.colors.primary else yellow
        in majorProphets -> if (sortAZ) MaterialTheme.colors.primary else orange
        in minorProphets -> if (sortAZ) MaterialTheme.colors.primary else lightPurple
        in gospels -> red
        in acts -> lightRed
        in paulineEpistles -> if (sortAZ) MaterialTheme.colors.primary else darkBlue
        in generalEpistles -> if (sortAZ) MaterialTheme.colors.primary else darkGreen
        in apocalypticLiterature -> if (sortAZ) MaterialTheme.colors.primary else darkPurple
        else -> defaultColor
    }

    var bottomSheetViewCount by mutableStateOf(0)
    var isFirstLaunch by mutableStateOf(true)
    var showHomePage by mutableStateOf(true)
    var showBiblicalShorts by mutableStateOf(false)
    var isSearchActive by mutableStateOf(false)
    var showReadingPlans by mutableStateOf(false)
    var selectedShortsCategory by mutableStateOf("All")
    var autoDemoTourEnabled by mutableStateOf(false)
    var activePlanReading by mutableStateOf<ActivePlanReadingContext?>(null)
    var activeReaderScrollProgress by mutableStateOf(0f)

    var pendingDeepLink by mutableStateOf<ScriptureDeepLink?>(null)
        private set
    var navigationGeneration by mutableStateOf(0)
        private set
    var pendingTargetVerse by mutableStateOf<Int?>(null)
        private set
    var pendingTargetBookId by mutableStateOf<String?>(null)
        private set
    var pendingTargetChapter by mutableStateOf<Int?>(null)
        private set

    var highlightedVerseBookId by mutableStateOf<String?>(null)
        private set
    var highlightedVerseChapter by mutableStateOf<Int?>(null)
        private set
    var highlightedVerseStart by mutableStateOf<Int?>(null)
        private set
    var highlightedVerseEnd by mutableStateOf<Int?>(null)
        private set

    fun enqueueDeepLink(link: ScriptureDeepLink) {
        pendingDeepLink = link
    }

    fun clearPendingTargetVerse() {
        pendingTargetVerse = null
        pendingTargetBookId = null
        pendingTargetChapter = null
    }

    fun clearHighlightedVerse() {
        highlightedVerseBookId = null
        highlightedVerseChapter = null
        highlightedVerseStart = null
        highlightedVerseEnd = null
    }

    fun focusScriptureVerse(
        bookId: String,
        chapter: Int,
        verseStart: Int,
        verseEnd: Int = verseStart,
    ) {
        val safeChapter = chapter.coerceAtLeast(1)
        val safeStart = verseStart.coerceAtLeast(1)
        val safeEnd = verseEnd.coerceAtLeast(safeStart)
        highlightedVerseBookId = bookId
        highlightedVerseChapter = safeChapter
        highlightedVerseStart = safeStart
        highlightedVerseEnd = safeEnd
        pendingTargetVerse = safeStart
        pendingTargetBookId = bookId
        pendingTargetChapter = safeChapter
    }

    internal suspend fun consumePendingDeepLink(
        fetchDailyVerseData: suspend (String) -> DailyVerseData? = { version ->
            runCatching { fetchDailyVerse(version).data }.getOrNull()
        },
    ): Boolean {
        val link = pendingDeepLink ?: return false
        pendingDeepLink = null

        val targetRef = if (link.fetchDailyIfMissing) {
            val requestedVersion = link.version ?: selectedVersion
            val daily = runCatching { fetchDailyVerseData(requestedVersion) }.getOrNull()
            if (daily != null && daily.chapter >= 1) {
                val usfm = daily.bookUsfm?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
                    ?: usfmForBookNumber(daily.bookNumber)
                    ?: "JHN"
                ScriptureDeepLink(
                    bookUsfm = usfm,
                    bookNumber = daily.bookNumber,
                    chapter = daily.chapter.coerceAtLeast(1),
                    verse = daily.verse.coerceAtLeast(1),
                    version = link.version,
                )
            } else {
                ScriptureDeepLink(
                    bookUsfm = "JHN",
                    bookNumber = 43,
                    chapter = 3,
                    verse = 16,
                    version = link.version,
                )
            }
        } else {
            link
        }

        if (!targetRef.version.isNullOrBlank()) {
            val versions = bibleVersions.data
            if (versions.isEmpty() || versions.any { it.abbreviation.equals(targetRef.version, ignoreCase = true) }) {
                updateSelectedVersion(targetRef.version)
            }
        }

        val books = bibleBooks.data.orEmpty()
        val resolvedUsfm = targetRef.bookUsfm.ifBlank { usfmForBookNumber(targetRef.bookNumber) ?: "JHN" }
        val resolvedNumber = targetRef.bookNumber ?: bookNumberForUsfm(resolvedUsfm)
        val matchedBook = books.find {
            it.bookId.equals(resolvedUsfm, ignoreCase = true)
        } ?: resolvedNumber?.let { num ->
            books.getOrNull(num - 1)
        } ?: BookData(
            id = resolvedUsfm,
            name = resolvedNumber?.let { bibleBooksFull.getOrNull(it - 1) } ?: resolvedUsfm,
            chapters = listOf(Chapter(bookId = "$resolvedUsfm.intro")) +
                (1..targetRef.chapter.coerceAtLeast(1)).map { ch ->
                    Chapter(
                        name = "$resolvedUsfm.$ch",
                        bookId = resolvedUsfm,
                        number = ch.toString(),
                        position = ch,
                    )
                },
        )

        returnToShortsOnBack = false
        closeBiblicalShorts()
        restoreReadingLocation(matchedBook, targetRef.chapter.coerceAtLeast(1))
        focusScriptureVerse(
            bookId = matchedBook.bookId,
            chapter = targetRef.chapter.coerceAtLeast(1),
            verseStart = targetRef.verse.coerceAtLeast(1),
            verseEnd = targetRef.verse.coerceAtLeast(1),
        )
        AnalyticsTracker.trackBookSelected(
            bookId = matchedBook.bookId,
            bookName = matchedBook.name ?: resolvedUsfm,
            source = "votd_notification",
        )
        return true
    }

    fun openBiblicalShorts(category: String = "All") {
        if (!CloudSessionManager.biblicalShortsEnabled) return
        selectedShortsCategory = category
        showBiblicalShorts = true
        AnalyticsTracker.track(
            "shorts_feed_opened",
            "biblical_shorts",
            mapOf("category" to category),
        )
    }

    fun closeBiblicalShorts() {
        if (showBiblicalShorts) {
            AnalyticsTracker.track(
                "shorts_feed_closed",
                "biblical_shorts",
                mapOf("category" to selectedShortsCategory),
            )
        }
        showBiblicalShorts = false
    }

    val snackBarChannel = Channel<String>(capacity = 1)

    @OptIn(ExperimentalMaterialApi::class)
    val bottomSheetScaffoldState
        @Composable
        get() = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed),
        )

    var bibleVersions by mutableStateOf(BibleVersions())
        private set

    fun updateBibleVersions(newVersions: BibleVersions) {
        bibleVersions = BibleVersions(data = newVersions.data)
    }

    private var _selectedVersion: MutableState<String> = mutableStateOf("")
        get() {
            if (field.value.isEmpty()) {
                field.value = bibleVersions.data.find {
                    it.abbreviation?.contains("KJV") == true
                }?.abbreviation ?: "KJV"
            }
            return field
        }
    val selectedVersion: String by _selectedVersion

    internal fun updateSelectedVersion(version: String? = null) {
        Napier.v("updateSelectedVersion: $version", tag = "AP8243")
        _selectedVersion.value = normalizedTranslation(version ?: selectedVersion)
    }

    var bibleBooks by mutableStateOf(BibleBooksResponse())
        private set

    val uiBooks: BibleBooksResponse
        get() = if (!sortAZ) {
            bibleBooks
        } else {
            bibleBooks.copy(data = bibleBooks.data?.sortedBy { it.name })
        }

    internal fun updateBooks(newBooks: BibleBooksResponse) {
        bibleBooks = newBooks
    }

    var sortAZ by mutableStateOf(false)
        internal set

    val selectedSortType get() = if (sortAZ) "A-Z" else "OT-NT"

    val fontSizeOptions = if (isDesktopPlatform()) {
        listOf(16f, 20f, 24f, 28f, 32f, 36f, 40f, 44f, 48f, 52f, 56f, 60f, 64f, 68f, 72f)
    } else {
        listOf(16f, 18f, 20f, 22f, 24f, 26f, 28f, 30f)
    }

    var selectedFontSize by mutableStateOf(22f)

    var selectedBook by mutableStateOf(BookData())
        private set

    var selectedChapter by mutableStateOf("")
        private set

    internal fun updateSelectedChapter(chapter: String? = null) {
        Napier.v("updateSelectedChapter: $chapter", tag = "BB2452")
        val resolved = chapter ?: "${selectedBook.bookId}.1"
        val resolvedChapterNum = resolved.substringAfter('.').toIntOrNull()
        if (highlightedVerseBookId != null &&
            (!highlightedVerseBookId.equals(selectedBook.bookId, ignoreCase = true) ||
                (resolvedChapterNum != null && highlightedVerseChapter != resolvedChapterNum))
        ) {
            clearHighlightedVerse()
        }
        selectedChapter = resolved
    }

    fun updateSelectedBook(newBook: BookData) {
        if (newBook.bookId != selectedBook.bookId) {
            dailyVerseSwipeOrigin = null
            GeminiModel.cancelPendingSummary()
            if (highlightedVerseBookId != null &&
                !highlightedVerseBookId.equals(newBook.bookId, ignoreCase = true)
            ) {
                clearHighlightedVerse()
            }
        }
        selectedBook = newBook
    }

    private var dailyVerseSwipeOrigin by mutableStateOf<Pair<String, Int>?>(null)
    internal val hasDailyVerseSwipeOrigin: Boolean get() = dailyVerseSwipeOrigin != null

    internal fun returnsHomeFromDailyVerse(book: BookData, chapter: Int): Boolean =
        dailyVerseSwipeOrigin == (book.bookId to chapter)

    internal fun clearDailyVerseSwipeOriginIfMoved(book: BookData, chapter: Int) {
        if (hasDailyVerseSwipeOrigin && !returnsHomeFromDailyVerse(book, chapter)) {
            dailyVerseSwipeOrigin = null
        }
    }

    internal fun restoreReadingLocation(book: BookData, chapter: Int, fromHomeDailyVerse: Boolean = false) {
        // Explicit navigation jumps (home/history/deep links/Shorts Read Now) always start in scripture view
        GeminiModel.cancelPendingSummary(preserveShowSummary = false)
        updateSelectedBook(book)
        dailyVerseSwipeOrigin = if (fromHomeDailyVerse) book.bookId to chapter else null
        updateSelectedChapter("${book.bookId}.$chapter")
        val bookId = getBookOrdinal(book.remoteKey)
        val loaded = bibleChapter
        if (loaded?.bookId != bookId || loaded.chapterId != chapter || loaded.text == null ||
            !loaded.version.equals(selectedVersion, ignoreCase = true)
        ) {
            bibleChapter = BibleChapterUIState(
                bookId = bookId,
                chapterId = chapter,
                chapterList = book.chapterListBookData.orEmpty().mapNotNull { it.number?.toLongOrNull() },
            )
        }
        showHomePage = false
        navigationGeneration += 1
    }

    internal fun discardReadingLocation() {
        bibleChapter = BibleChapterUIState()
        clearPendingTargetVerse()
        clearHighlightedVerse()
        GeminiModel.cancelPendingSummary()
        showHomePage = true
    }

    var bibleChapter by mutableStateOf<BibleChapterUIState?>(BibleChapterUIState())
        private set

    fun updateBibleChapter(
        newChapter: List<BibleChapter>,
        chapterCount: ChapterCount?,
        version: String,
    ) {
        val cleanVerses = newChapter.filter { verse ->
            val cleanText = verse.t?.trim().orEmpty()
            cleanText.isNotEmpty() && cleanText != "[]"
        }
        bibleChapter = cleanVerses.firstOrNull()?.b?.toInt()?.let { bookId ->
            BibleChapterUIState(
                id = cleanVerses.firstOrNull()?.id,
                bookId = bookId,
                chapterId = cleanVerses.firstOrNull()?.c?.toInt(),
                text = if (version.uppercase() == "SVD") {
                    cleanVerses.joinToString("\n") { "${it.v} ${it.t}" }
                } else {
                    cleanVerses.joinToString(" ") { "[${it.v}] ${it.t}" }
                },
                chapterList = chapterCount?.chapterCount?.let { count ->
                    (1..count).toList()
                },
                version = version,
                verses = cleanVerses,
            )
        }
    }

    fun getBookOrdinal(chapterNumber: String): Int {
        val name = chapterNumber.substringBefore(".")
        return bibleBooks.data
            ?.indexOfFirst { it.bookId.substringBefore(".") == name }
            ?.takeIf { it >= 0 }
            ?.plus(1)
            ?: bookNumberForUsfm(name)
            ?: 1
    }

    var readingHistory by mutableStateOf<List<ReadingHistoryUIState>?>(null)
        private set

    fun updateReadingHistory(newHistory: List<SelectReadingHistory>) {
        readingHistory = newHistory.map {
            if (it.b == null || it.c == null) return@map ReadingHistoryUIState()
            val ordinal = it.b.toIntOrNull()
            ReadingHistoryUIState(
                bookName = ordinal?.let { ord -> bibleBooks.data?.getOrNull(ord - 1)?.name },
                chapterId = it.c.toInt(),
                date = transformDate(it.DATE),
                bookOrdinal = ordinal,
            )
        }
    }

    private fun transformDate(dateString: String): String {
        val localDate = LocalDate.parse(dateString)
        return localDate.month.name.lowercase().replaceFirstChar { it.uppercase() } +
            " " + localDate.day + ", " + localDate.year
    }

    var returnToShortsOnBack by mutableStateOf(false)
    var returnToPlansOnBack by mutableStateOf(false)
    var resumeShortIdOnReturn by mutableStateOf<String?>(null)

    fun resolveBookData(
        bookId: String,
        bookNumber: Int? = null,
        fallbackName: String? = null,
        minChapters: Int = 1,
    ): BookData {
        val cleanUsfm = bookId.trim().uppercase()
        val resolvedOrdinal = bookNumber ?: bookNumberForUsfm(cleanUsfm)
        val books = bibleBooks.data.orEmpty()
        return books.find { it.bookId.equals(cleanUsfm, ignoreCase = true) }
            ?: resolvedOrdinal?.let { ord -> books.getOrNull(ord - 1) }
            ?: BookData(
                id = cleanUsfm,
                name = fallbackName ?: resolvedOrdinal?.let { bibleBooksFull.getOrNull(it - 1) } ?: cleanUsfm,
                chapters = listOf(Chapter(bookId = "$cleanUsfm.intro")) +
                    (1..minChapters.coerceAtLeast(1)).map { ch ->
                        Chapter(
                            name = "$cleanUsfm.$ch",
                            bookId = cleanUsfm,
                            number = ch.toString(),
                            position = ch,
                        )
                    },
            )
    }

    fun openSpotlightSearch() {
        isSearchActive = true
        showReadingPlans = false
        AnalyticsTracker.track("spotlight_search_opened", "search")
    }

    fun closeSpotlightSearch() {
        isSearchActive = false
    }

    fun openReadingPlans() {
        isSearchActive = false
        showBiblicalShorts = false
        showReadingPlans = true
        AnalyticsTracker.track("reading_plans_opened", "reading_plans")
    }

    fun exitReadingPlansToHome() {
        showReadingPlans = false
        returnToPlansOnBack = false
        activePlanReading = null
        activeReaderScrollProgress = 0f
        showHomePage = true
        ReadingSession.goHome()
    }

    fun openPlanReading(context: ActivePlanReadingContext) {
        val ref = context.reading
        val targetBook = resolveBookData(
            bookId = ref.bookId,
            bookNumber = ref.bookNumber,
            fallbackName = ref.bookName,
            minChapters = ref.chapter,
        )
        isSearchActive = false
        showReadingPlans = false
        showBiblicalShorts = false
        returnToShortsOnBack = false
        returnToPlansOnBack = true
        activePlanReading = context
        activeReaderScrollProgress = 0f
        restoreReadingLocation(targetBook, ref.chapter.coerceAtLeast(1))
        if (ref.verseStart != null && ref.verseStart >= 1) {
            focusScriptureVerse(
                bookId = targetBook.bookId,
                chapter = ref.chapter.coerceAtLeast(1),
                verseStart = ref.verseStart,
                verseEnd = (ref.verseEnd ?: ref.verseStart).coerceAtLeast(ref.verseStart),
            )
        } else {
            clearHighlightedVerse()
            clearPendingTargetVerse()
        }
        AnalyticsTracker.track(
            name = "reading_plan_passage_opened",
            category = "reading_plans",
            properties = mapOf(
                "planId" to context.planId,
                "day" to context.dayNumber.toString(),
                "readingId" to ref.id,
            ),
        )
    }

    val onHomeClick: () -> Unit = {
        dailyVerseSwipeOrigin = null
        GeminiModel.cancelPendingSummary()
        Napier.v("onHomeClick", tag = "BB2452")
        AnalyticsTracker.track("home_top_bar_click", "navigation")
        returnToShortsOnBack = false
        returnToPlansOnBack = false
        resumeShortIdOnReturn = null
        activePlanReading = null
        activeReaderScrollProgress = 0f
        isSearchActive = false
        showReadingPlans = false
        clearHighlightedVerse()
        clearPendingTargetVerse()
        showBiblicalShorts = false
        showHomePage = true
        ReadingSession.goHome()
    }

    internal fun exitBiblicalShortsToHome() {
        closeBiblicalShorts()
        GeminiModel.cancelPendingSummary()
        returnToShortsOnBack = false
        returnToPlansOnBack = false
        resumeShortIdOnReturn = null
        activePlanReading = null
        activeReaderScrollProgress = 0f
        isSearchActive = false
        showReadingPlans = false
        clearHighlightedVerse()
        clearPendingTargetVerse()
        showBiblicalShorts = false
        showHomePage = true
        ReadingSession.goHome()
    }

    internal fun navigateBackFromReader() {
        GeminiModel.cancelPendingSummary()
        if (returnToShortsOnBack) {
            returnToShortsOnBack = false
            clearHighlightedVerse()
            clearPendingTargetVerse()
            showHomePage = true
            ReadingSession.goHome()
            AnalyticsTracker.track("reader_back_to_shorts", "navigation")
            openBiblicalShorts()
        } else if (returnToPlansOnBack) {
            returnToPlansOnBack = false
            activePlanReading = null
            activeReaderScrollProgress = 0f
            clearHighlightedVerse()
            clearPendingTargetVerse()
            showHomePage = true
            showReadingPlans = true
            ReadingSession.goHome()
            AnalyticsTracker.track("reader_back_to_plans", "reading_plans")
        } else {
            onHomeClick()
        }
    }

    internal fun updateErrorSnackBar(error: String) {
        Napier.v("snackBarChannel :: updateErrorSnackBar: $error", tag = "BB2452")
        AnalyticsTracker.track(
            "snackbar_error_shown",
            "error",
            mapOf("message" to error.take(160)),
        )
        snackBarChannel.trySend(error)
    }
}
