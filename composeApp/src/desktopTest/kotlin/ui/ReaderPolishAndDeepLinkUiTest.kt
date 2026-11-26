package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import data.bible.BibleBooksResponse
import data.bible.BibleChapter
import data.bible.BibleChapterUIState
import data.bible.BibleState
import data.bible.BibleVersions
import data.bible.BookData
import data.bible.Chapter
import data.bible.DailyVerse
import data.bible.DailyVerseData
import data.bible.DailyVerseRepository
import data.bible.ReadingHistoryUIState
import data.bible.json.BUNDLED_BIBLE_BOOKS_JSON
import data.config.CloudFeatureFlagsDto
import data.config.CloudRuntimeConfigDto
import data.config.CloudSessionManager
import email.kevinphillips.biblebible.db.SelectReadingHistory
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ui.configs.ChapterTopBar
import ui.configs.DRAWER_AUTO_DISMISS_MS

@OptIn(ExperimentalTestApi::class, ExperimentalMaterialApi::class)
class ReaderPolishAndDeepLinkUiTest {

    @AfterTest
    fun tearDown() {
        CloudSessionManager.resetForTesting()
        BibleState.sortAZ = false
        BibleState.showHomePage = true
        BibleState.clearPendingTargetVerse()
        BibleState.clearHighlightedVerse()
    }

    @Test
    fun deepLinkCentersTargetVerseAndUsesGoldTextWithoutBackgroundFill() = runComposeUiTest {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        BibleState.updateBooks(books)
        val psalms = books.data!!.first { it.bookId == "PSA" }
        BibleState.updateSelectedBook(psalms)

        val chapterText = (1..40).joinToString(" ") { v ->
            "[$v] Blessed are the undefiled in the way, who walk in the law of the Lord verse $v."
        }
        val chapter = BibleChapterUIState(
            bookId = 19,
            chapterId = 119,
            text = chapterText,
            version = "KJV",
        )
        BibleState.focusScriptureVerse(bookId = "PSA", chapter = 119, verseStart = 20, verseEnd = 20)

        lateinit var scrollState: ScrollState
        lateinit var readingState: ScriptureReadingState
        setContent {
            BibleBibleTheme {
                scrollState = rememberScrollState()
                readingState = rememberScriptureReadingState()
                Box(modifier = Modifier.size(360.dp, 400.dp)) {
                    BibleScriptures(
                        chapters = chapter,
                        scrollState = scrollState,
                        selectedFontSize = 20f,
                        onFontSizeChanged = {},
                        readingState = readingState,
                    )
                }
            }
        }

        mainClock.advanceTimeBy(120)
        waitForIdle()

        runOnIdle {
            assertNull(BibleState.pendingTargetVerse)
            val layout = requireNotNull(readingState.layout)
            val verseStartIdx = chapterText.indexOf("[20]")
            val line = layout.getLineForOffset(verseStartIdx)
            val verseScreenMidY = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f +
                readingState.contentInset - scrollState.value
            val viewportMidY = layout.size.height.coerceAtLeast(1)
            // Viewport box is 400dp tall at 1f density on Skiko (~400px)
            assertTrue(scrollState.value > 0, "Should scroll down to verse 20")
            assertTrue(abs(verseScreenMidY - 200f) < 60f, "Expected verse 20 near center (~200px), got $verseScreenMidY (viewport=${viewportMidY})")
        }

        val textNode = onNodeWithTag("ScriptureText").fetchSemanticsNode()
        val annotated = textNode.config[SemanticsProperties.Text].first()
        val goldSpans = annotated.spanStyles.filter { it.item.color == Color(0xFFFBD38D) }
        assertTrue(goldSpans.isNotEmpty(), "Expected gold text SpanStyle on deep-linked verse")
        assertTrue(
            goldSpans.all { it.item.background == Color.Unspecified },
            "Gold verse highlight must not apply a background box fill"
        )
    }

    @Test
    fun chapterTopBarShowsFullBookNameAndRemovesBibleBibleCenterMarketing() = runComposeUiTest {
        val chronBook = BookData(
            id = "2CH",
            name = "2 Chronicles",
            abbreviation = "2CH",
            chapters = listOf(
                Chapter(bookId = "2CH.intro"),
                Chapter(bookId = "2CH.1", number = "1", position = 1),
            ),
        )
        BibleState.updateSelectedBook(chronBook)

        setContent {
            BibleBibleTheme {
                ChapterTopBar(
                    showHomePage = false,
                    onBackClick = {},
                    showBottomSheet = {},
                )
            }
        }

        onNodeWithText("2 Chronicles").assertIsDisplayed()
        onNodeWithText("2 Chron").assertDoesNotExist()
        onNodeWithTag("chapter_top_bar_emblem").assertDoesNotExist()
        onNodeWithText("BibleBible").assertDoesNotExist()
    }

    @Test
    fun bookGridSectionSpacersFollowOtNtOrderAndFeatureFlag() = runComposeUiTest {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        CloudSessionManager.applyRuntimeConfig(
            CloudRuntimeConfigDto(
                features = CloudFeatureFlagsDto(bookSectionPaddingDp = 12)
            )
        )
        BibleState.sortAZ = false

        setContent {
            BibleBibleTheme {
                Box(modifier = Modifier.size(412.dp, 915.dp)) {
                    BibleBookList(bookData = books.data)
                }
            }
        }

        waitForIdle()
        onNodeWithTag("book_section_spacer_0").assertExists()

        runOnIdle { BibleState.sortAZ = true }
        waitForIdle()
        onNodeWithTag("book_section_spacer_0").assertDoesNotExist()
    }

    @Test
    fun tabletBookGridSplitsOldAndNewTestamentsBalancedThreeByThree() = runComposeUiTest {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        CloudSessionManager.applyRuntimeConfig(
            CloudRuntimeConfigDto(
                features = CloudFeatureFlagsDto(
                    bookSectionPaddingDp = 12,
                    bookGridTabletLayout = "split_testaments",
                )
            )
        )
        BibleState.sortAZ = false

        setContent {
            BibleBibleTheme {
                Box(modifier = Modifier.size(1200.dp, 860.dp)) {
                    BibleBookList(bookData = books.data)
                }
            }
        }

        waitForIdle()
        onNodeWithTag("tablet_split_testaments_grid").assertIsDisplayed()
        onNodeWithText("Old Testament").assertIsDisplayed()
        onNodeWithText("New Testament").assertIsDisplayed()
        onNodeWithTag("split_old_testament_spacer_0").assertExists()
    }

    @Test
    fun settingsBottomSheetHas20sTimeoutAndNavigatesOnHistoryTap() = runComposeUiTest {
        assertEquals(20_000L, DRAWER_AUTO_DISMISS_MS)
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        BibleState.updateBooks(books)
        BibleState.showHomePage = true
        BibleState.updateReadingHistory(
            listOf(SelectReadingHistory(DATE = "2026-11-23", b = "43", c = "3"))
        )

        lateinit var scaffoldState: androidx.compose.material.BottomSheetScaffoldState
        setContent {
            BibleBibleTheme {
                scaffoldState = androidx.compose.material.rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberBottomSheetState(BottomSheetValue.Expanded)
                )
                Box(modifier = Modifier.size(420.dp, 680.dp)) {
                    androidx.compose.material.BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        sheetPeekHeight = 0.dp,
                        sheetContent = {
                            ui.configs.BottomSheetConfigView(
                                bibleVersionsList = BibleVersions(),
                                showAISummary = false,
                                bottomSheetState = scaffoldState.bottomSheetState,
                                autoDismissDelayMs = 0L,
                                onInitialLoad = {},
                            )
                        },
                    ) {
                        Box(modifier = Modifier.size(420.dp, 680.dp))
                    }
                }
            }
        }

        waitForIdle()
        onNodeWithTag("reading_history_item_0").assertIsDisplayed().performClick()
        mainClock.advanceTimeBy(300)
        waitForIdle()

        runOnIdle {
            assertFalse(BibleState.showHomePage)
            assertEquals("JHN", BibleState.selectedBook.bookId)
            assertEquals("JHN.3", BibleState.selectedChapter)
            assertFalse(scaffoldState.bottomSheetState.isExpanded)
        }
    }

    @Test
    fun literarySerifRendersParagraphsAndRedLetterWhenEnabledAndFallsBackWithoutParagraphs() = runComposeUiTest {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        BibleState.updateBooks(books)
        val john = books.data!!.first { it.bookId == "JHN" }
        BibleState.updateSelectedBook(john)
        CloudSessionManager.applyRuntimeConfig(
            CloudRuntimeConfigDto(
                features = CloudFeatureFlagsDto(
                    readerLayoutStyle = "literary_serif",
                    readerRedLetterEnabled = true,
                )
            )
        )

        val proseChapter = BibleChapterUIState(
            bookId = 43,
            chapterId = 3,
            version = "KJV",
            text = "[1] There was a man. [2] The same came. [3] Jesus answered, Verily I say.",
            verses = listOf(
                BibleChapter(id = "1", b = "43", c = "3", v = "1", t = "There was a man.", p = false),
                BibleChapter(id = "2", b = "43", c = "3", v = "2", t = "The same came.", p = false),
                BibleChapter(
                    id = "3",
                    b = "43",
                    c = "3",
                    v = "3",
                    t = "Jesus answered, Verily I say.",
                    p = true,
                    r = listOf(listOf(16, 29)),
                ),
            ),
        )

        setContent {
            BibleBibleTheme {
                Box(modifier = Modifier.size(380.dp, 500.dp)) {
                    BibleScriptures(
                        chapters = proseChapter,
                        scrollState = rememberScrollState(),
                        selectedFontSize = 20f,
                        onFontSizeChanged = {},
                    )
                }
            }
        }

        waitForIdle()
        val proseNode = onNodeWithTag("ScriptureText").fetchSemanticsNode()
        val proseAnnotated = proseNode.config[SemanticsProperties.Text].first()
        // Verses 1 and 2 share paragraph 1 (single space join), verse 3 starts paragraph 2 (\n\n break)
        assertTrue(proseAnnotated.text.contains("1\u00A0There was a man. 2\u00A0The same came.\n\n3\u00A0Jesus answered, Verily I say."))
        val redSpans = proseAnnotated.spanStyles.filter { it.item.color == Color(0xFFF48B8B) }
        assertEquals(1, redSpans.size)
        assertEquals("Verily I say.", proseAnnotated.text.substring(redSpans[0].start, redSpans[0].end))

        // Chapter with no paragraph markers falls back to verse-per-line cleanly
        val psalmFallback = buildScriptureContent(
            chapters = BibleChapterUIState(
                bookId = 19,
                chapterId = 23,
                version = "KJV",
                verses = listOf(
                    BibleChapter(id = "1", b = "19", c = "23", v = "1", t = "The LORD is my shepherd."),
                    BibleChapter(id = "2", b = "19", c = "23", v = "2", t = "He maketh me to lie down."),
                ),
            ),
            activeVersion = "KJV",
            selectedFontSize = 22f,
            layoutStyle = "literary_serif",
            verseGapDp = 14,
            redLetterEnabled = false,
            highlightStart = null,
            highlightEnd = null,
        )!!
        assertEquals("1\u00A0The LORD is my shepherd.\n2\u00A0He maketh me to lie down.", psalmFallback.text.text)
    }

    @Test
    fun aiLockedChapterLoadingShowsAiCardInsteadOfScriptureShimmer() = runComposeUiTest {
        mainClock.autoAdvance = false
        val deutBook = BookData(
            id = "DEU",
            name = "Deuteronomy",
            chapters = (1..3).map { ch ->
                Chapter(name = "DEU.$ch", bookId = "DEU", number = ch.toString(), position = ch)
            },
        )
        BibleState.updateSelectedBook(deutBook)

        setContent {
            BibleBibleTheme {
                Box(modifier = Modifier.size(412.dp, 860.dp)) {
                    BibleScripturesPager(
                        chapters = BibleChapterUIState(
                            bookId = 5,
                            chapterId = null,
                            chapterList = listOf(1, 2, 3),
                            version = "KJV",
                            text = null,
                        ),
                        bibleVersion = "KJV",
                        selectedBook = deutBook,
                        isAISummaryLoading = true,
                        showAISummary = true,
                        bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
                            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
                        ),
                        loadChapter = { _, _, _, _ -> },
                        initialShowGestureTutorial = true,
                        onDismissGestureTutorial = {},
                    )
                }
            }
        }

        mainClock.advanceTimeBy(120)
        onNodeWithTag("ai_summary_loading_view").assertIsDisplayed()
        onNodeWithTag("scripture_shimmer_loading").assertDoesNotExist()
        onNodeWithTag("reader_gesture_tutorial_card").assertDoesNotExist()
    }

    @Test
    fun readerGestureTutorialCardDisplaysAndDismissesOnGotItTap() = runComposeUiTest {
        val deutBook = BookData(
            id = "DEU",
            name = "Deuteronomy",
            chapters = (1..3).map { ch ->
                Chapter(name = "DEU.$ch", bookId = "DEU", number = ch.toString(), position = ch)
            },
        )
        BibleState.updateSelectedBook(deutBook)
        var dismissedCount = 0

        setContent {
            BibleBibleTheme {
                Box(modifier = Modifier.size(412.dp, 860.dp)) {
                    BibleScripturesPager(
                        chapters = BibleChapterUIState(
                            bookId = 5,
                            chapterId = 2,
                            chapterList = listOf(1, 2, 3),
                            version = "KJV",
                            text = "[1] Then we turned, and took our journey into the wilderness.",
                            verses = listOf(
                                BibleChapter(
                                    id = "1",
                                    b = "5",
                                    c = "2",
                                    v = "1",
                                    t = "Then we turned, and took our journey into the wilderness.",
                                )
                            ),
                        ),
                        bibleVersion = "KJV",
                        selectedBook = deutBook,
                        isAISummaryLoading = false,
                        showAISummary = false,
                        bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
                            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
                        ),
                        loadChapter = { _, _, _, _ -> },
                        initialShowGestureTutorial = true,
                        onDismissGestureTutorial = { dismissedCount++ },
                    )
                }
            }
        }

        waitForIdle()
        onNodeWithTag("reader_gesture_tutorial_card").assertIsDisplayed()
        onNodeWithText("Pinch to zoom").assertIsDisplayed()
        onNodeWithText("Double-tap text").assertIsDisplayed()

        onNodeWithTag("reader_tutorial_dismiss_btn").performClick()
        mainClock.advanceTimeBy(300)
        waitForIdle()

        runOnIdle { assertEquals(1, dismissedCount) }
        onNodeWithTag("reader_gesture_tutorial_card").assertDoesNotExist()
    }
    @Test
    fun firstChapterBackSwipeRevealsHomeAndCommitsOnlyAfterRelease() = runComposeUiTest {
        lateinit var backSwipe: SwipeTransition
        var backCount = 0
        setContent {
            val scope = rememberCoroutineScope()
            backSwipe = remember(scope) { SwipeTransition(scope) }
            Box(
                Modifier.size(400.dp, 400.dp)
                    .onSizeChanged { backSwipe.extent = it.width }
                    .readerRightBackSwipe(
                        canStart = { true },
                        onDrag = backSwipe::drag,
                        onFinish = { backSwipe.finish(it) { backCount++ } },
                    )
                    .testTag("reader_back_swipe"),
            )
        }

        val reader = onNodeWithTag("reader_back_swipe")
        reader.performTouchInput { swipe(center, Offset(width * 0.25f, centerY)) }
        runOnIdle { assertEquals(0, backCount) }
        reader.performTouchInput { swipe(center, Offset(width * 0.6f, centerY)) }
        runOnIdle {
            assertEquals(0, backCount)
            assertEquals(0f, backSwipe.offset)
        }
        reader.performTouchInput {
            down(Offset(width * 0.2f, centerY))
            moveTo(Offset(width * 0.65f, centerY))
        }
        runOnIdle {
            assertTrue(backSwipe.offset > 0f)
            assertEquals(0, backCount)
        }
        reader.performTouchInput { up() }
        runOnIdle { assertEquals(1, backCount) }
    }

    @Test
    fun edgeNavigationPreviewsOnTheFirstSwipeAndCancelsShortReleases() = runComposeUiTest {
        var atGridEnd = false
        var wordsOpened = 0
        var chaptersOpened = 0
        var homeOpened = 0
        lateinit var transition: SwipeTransition
        setContent {
            val scope = rememberCoroutineScope()
            transition = remember(scope) { SwipeTransition(scope) }
            Box(
                Modifier.size(400.dp, 400.dp)
                    .onSizeChanged { transition.extent = it.height }
                    .homeSwipeToWords { wordsOpened++ }
                    .verticalEdgeTurns(
                        upward = {
                            if (atGridEnd) EdgeTurn(transition, onCommit = { chaptersOpened++ }) else null
                        },
                        downward = { EdgeTurn(transition, onCommit = { homeOpened++ }) },
                    )
                    .testTag("edge_navigation_gestures"),
            )
        }
        val reader = onNodeWithTag("edge_navigation_gestures")
        reader.performTouchInput { swipe(centerRight - Offset(12f, 0f), centerLeft + Offset(12f, 0f)) }
        runOnIdle { assertEquals(1, wordsOpened) }
        reader.performTouchInput { swipe(bottomCenter - Offset(0f, 12f), topCenter + Offset(0f, 12f)) }
        runOnIdle {
            assertEquals(0, chaptersOpened)
            atGridEnd = true
        }

        reader.performTouchInput {
            down(center)
            moveTo(center - Offset(0f, height * 0.1f))
        }
        runOnIdle {
            assertTrue(transition.progress > 0f)
            assertEquals(0, chaptersOpened)
        }
        reader.performTouchInput { up() }
        runOnIdle {
            assertEquals(0f, transition.progress)
            assertEquals(0, chaptersOpened)
        }
        reader.performTouchInput { swipe(bottomCenter - Offset(0f, 12f), topCenter + Offset(0f, 12f)) }
        runOnIdle { assertEquals(1, chaptersOpened) }
        reader.performTouchInput { swipe(topCenter + Offset(0f, 12f), bottomCenter - Offset(0f, 12f)) }
        runOnIdle { assertEquals(1, homeOpened) }
    }

    @Test
    fun readerBottomSwipeLoadsTheNextChapterOnlyAfterCommit() = runComposeUiTest {
        val book = BookData(id = "JAS", name = "James")
        BibleState.updateSelectedBook(book)
        BibleState.updateSelectedChapter("JAS.1")
        val requestedChapters = mutableListOf<Int>()
        setContent {
            val scope = rememberCoroutineScope()
            val transition = remember(scope) { SwipeTransition(scope) }
            BibleBibleTheme {
                Box(Modifier.size(400.dp, 760.dp).onSizeChanged { transition.extent = it.height }) {
                    BibleScripturesPager(
                        chapters = BibleChapterUIState(
                            bookId = 59,
                            chapterId = 1,
                            chapterList = listOf(1, 2, 3, 4, 5),
                            version = "KJV",
                            text = "[1] James, a servant of God.",
                            verses = listOf(BibleChapter(id = "1", v = "1", t = "James, a servant of God.")),
                        ),
                        bibleVersion = "KJV",
                        selectedBook = book,
                        isAISummaryLoading = false,
                        showAISummary = false,
                        bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
                            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
                        ),
                        loadChapter = { _, chapter, _, _ -> requestedChapters.add(chapter) },
                        initialShowGestureTutorial = false,
                        chapterTurn = { _, _, open -> EdgeTurn(transition, onCommit = open) },
                    )
                }
            }
        }
        val pager = onNodeWithTag("reader_chapter_pager")
        pager.performTouchInput { swipe(center, center - Offset(0f, 40f)) }
        runOnIdle { assertTrue(requestedChapters.isEmpty()) }
        pager.performTouchInput { swipe(center, center - Offset(0f, height * 0.4f)) }
        runOnIdle { assertEquals(listOf(2), requestedChapters) }
    }

    @Test
    fun canonicalSuccessorUsesKjvOrderRegardlessOfHomeSort() {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON).data.orEmpty()
            .sortedBy { it.name }
        val malachi = books.first { it.bookId == "MAL" }
        val revelation = books.first { it.bookId == "REV" }
        assertEquals("MAT", nextCanonicalBook(malachi, books)?.bookId)
        assertNull(nextCanonicalBook(revelation, books))
    }

    @Test
    fun homeAppBarBecomesFloatingOnTheFirstScrollAndKeepsSortAccessible() = runComposeUiTest {
        BibleState.updateBooks(Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON))
        CloudSessionManager.applyRuntimeConfig(
            CloudRuntimeConfigDto(
                features = CloudFeatureFlagsDto(
                    biblicalShortsEnabled = false,
                    bookShimmerStyle = "none",
                ),
            ),
        )
        val repository = DailyVerseRepository(
            fetch = { DailyVerse(it, "2026-11-24", DailyVerseData(23, 40, 31, "Daily verse", "ISA")) },
        )
        lateinit var grid: LazyGridState
        lateinit var scope: CoroutineScope
        setContent {
            grid = rememberLazyGridState()
            scope = rememberCoroutineScope()
            BibleBibleTheme {
                Box(Modifier.size(400.dp, 760.dp)) { BibleHomeContent(grid, verseRepository = repository) }
            }
        }

        onNodeWithTag("floating_home_top_bar").assertIsDisplayed()
        onNodeWithText("Daily verse").assertIsDisplayed()
        runOnIdle {
            assertEquals(0f, homeBarGlassProgress(grid, 48f))
            scope.launch { grid.scrollToItem(0, scrollOffset = 8) }
        }
        waitForIdle()
        runOnIdle {
            assertEquals(0, grid.firstVisibleItemIndex)
            assertTrue(homeBarGlassProgress(grid, 48f) > 0f)
            scope.launch { grid.scrollToItem(12) }
        }
        waitForIdle()
        runOnIdle { assertEquals(1f, homeBarGlassProgress(grid, 48f)) }
        onNodeWithText("A-Z").assertIsDisplayed().performClick()
        runOnIdle {
            assertTrue(BibleState.sortAZ)
            scope.launch { grid.scrollToItem(0) }
        }
        waitForIdle()
        runOnIdle { assertEquals(0f, homeBarGlassProgress(grid, 48f)) }
    }

    @Test
    fun switchingVersionPreservesCurrentChapterAfterSwiping() = runComposeUiTest {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        BibleState.updateBooks(books)
        val john = books.data!!.first { it.bookId == "JHN" }
        val chapterList = (1L..21L).toList()

        fun buildChapterState(chapter: Int, version: String) = BibleChapterUIState(
            bookId = 43,
            chapterId = chapter,
            chapterList = chapterList,
            version = version,
            text = "[1] $version John chapter $chapter verse 1.",
            verses = listOf(BibleChapter(id = "1", v = "1", t = "$version John chapter $chapter verse 1.")),
        )

        val loadRequests = mutableListOf<Triple<Int, String, Boolean>>()
        val currentVersion = androidx.compose.runtime.mutableStateOf("KJV")
        val currentChapters = androidx.compose.runtime.mutableStateOf(buildChapterState(1, "KJV"))

        BibleState.restoreReadingLocation(john, 1)

        setContent {
            BibleBibleTheme {
                Box(Modifier.size(400.dp, 760.dp)) {
                    BibleScripturesPager(
                        chapters = currentChapters.value,
                        bibleVersion = currentVersion.value,
                        selectedBook = john,
                        isAISummaryLoading = false,
                        showAISummary = false,
                        bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
                            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
                        ),
                        loadChapter = { _, chapter, version, updateHistory ->
                            loadRequests.add(Triple(chapter, version, updateHistory))
                            currentChapters.value = buildChapterState(chapter, version)
                        },
                        initialShowGestureTutorial = false,
                    )
                }
            }
        }

        waitForIdle()
        onNodeWithText("KJV John chapter 1 verse 1.", substring = true).assertIsDisplayed()

        // Navigate to chapter 4 via chapter strip (simulating user continuing through chapters)
        onNodeWithText("4").performClick()
        waitForIdle()

        runOnIdle {
            assertEquals("JHN.4", BibleState.selectedChapter)
            assertTrue(loadRequests.any { it.first == 4 && it.second == "KJV" })
        }
        onNodeWithText("KJV John chapter 4 verse 1.", substring = true).assertIsDisplayed()

        // Switch Bible version from KJV to BBE while on Chapter 4
        val requestCountBeforeSwitch = loadRequests.size
        runOnIdle { currentVersion.value = "BBE" }
        waitForIdle()

        val requestsAfterSwitch = loadRequests.drop(requestCountBeforeSwitch)
        assertEquals(listOf(Triple(4, "BBE", false)), requestsAfterSwitch)
        assertFalse(requestsAfterSwitch.any { it.first == 1 }, "Must not reset back to chapter 1 on version switch")
        assertEquals("JHN.4", BibleState.selectedChapter)
        onNodeWithText("BBE John chapter 4 verse 1.", substring = true).assertIsDisplayed()
    }

    @Test
    fun readerPrefetchesNextChapterAfterSettling() = runComposeUiTest {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        BibleState.updateBooks(books)
        val john = books.data!!.first { it.bookId == "JHN" }
        BibleState.restoreReadingLocation(john, 2)
        val prefetched = mutableListOf<Triple<String, Int, String>>()

        setContent {
            BibleBibleTheme {
                Box(Modifier.size(400.dp, 760.dp)) {
                    BibleScripturesPager(
                        chapters = BibleChapterUIState(
                            bookId = 43,
                            chapterId = 2,
                            chapterList = (1L..21L).toList(),
                            version = "KJV",
                            text = "[1] KJV John chapter 2 verse 1.",
                            verses = listOf(BibleChapter(id = "1", v = "1", t = "KJV John chapter 2 verse 1.")),
                        ),
                        bibleVersion = "KJV",
                        selectedBook = john,
                        isAISummaryLoading = false,
                        showAISummary = false,
                        bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
                            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
                        ),
                        prefetchChapter = { book, chapter, version ->
                            prefetched.add(Triple(book.bookId, chapter, version))
                        },
                        initialShowGestureTutorial = false,
                    )
                }
            }
        }

        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        assertEquals(listOf(Triple("JHN", 3, "KJV")), prefetched)
    }

}
