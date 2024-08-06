package data.apiBible

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import data.bibleIQ.BibleIQDataModel
import email.kevinphillips.biblebible.db.SelectReadingHistory
import io.github.aakira.napier.Napier
import kotlinx.datetime.LocalDate
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleAPIDataModel {
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


    private val lightGreen = Color(0x80CCFFCC)
    private val lightBlue = Color(0x8080D4EA)
    private val yellow = Color(0x80FFFF99)
    private val orange = Color(0x80FFCC99)
    private val lightPurple = Color(0x80E0B0FF)
    private val red = Color(0x80FF9999)
    private val lightRed = Color(0x80FFA07A)
    private val darkBlue = Color(0x808B9DC3)
    private val darkGreen = Color(0x809CCEA0)
    private val darkPurple = Color(0x806B5B95)
    private val defaultColor = Color(0x80FFFFFF)

    fun getBibleBookColor(ordinal: Int): Color {
        return when (ordinal) {
            in pentateuch -> lightGreen
            in historicalBooks -> lightBlue
            in poetryAndWisdom -> yellow
            in majorProphets -> orange
            in minorProphets -> lightPurple
            in gospels -> red
            in acts -> lightRed
            in paulineEpistles -> darkBlue
            in generalEpistles -> darkGreen
            in apocalypticLiterature -> darkPurple
            else -> defaultColor
        }
    }

    const val DEFAULT_BIBLE_ID = "de4e12af7f28f599-02"
    private var _selectedLanguage: MutableState<String> = mutableStateOf("eng")
    val selectedLanguage by _selectedLanguage

    private var _selectedBibleId = mutableStateOf(DEFAULT_BIBLE_ID)
    val selectedBibleId: String by _selectedBibleId
    internal fun updateSelectedBibleId(bibleId: String? = null) {
        Napier.v("updateSelectedBibleId: $bibleId", tag = "BB2452")
        _selectedBibleId.value = bibleId ?: (selectedBibleId)
    }

    private var _chapterContent: MutableState<ChapterContent> = mutableStateOf(ChapterContent())
    val chapterContent: ChapterContent get() = _chapterContent.value
    internal fun updateChapterContent(newContent: ChapterContent) {
        _chapterContent.value = newContent
    }

    var bibleBooks by mutableStateOf(BibleAPIBook())
        private set
    val uiBooks: BibleAPIBook
        get() = if (!BibleIQDataModel.sortAZ) bibleBooks else
            bibleBooks.copy(data = bibleBooks.data?.sortedBy { it.name })
    internal fun updateBooks(newBooks: BibleAPIBook) {
        bibleBooks = newBooks
    }

    var bibleVersions = mutableStateOf(BibleAPIBibles())
    val abbreviationList: List<BibleAPIBibles.BibleAPIVersion> get() = bibleVersions.value.data?.map { it } ?: emptyList()

    private var _selectedVersion: MutableState<String> = mutableStateOf("")
        get() {
            if (field.value.isEmpty()) {
                field.value = abbreviationList.find {
                    it.abbreviationLocal?.contains("KJV") == true
                }?.abbreviationLocal ?: "KJV"
            }
            return field
        }
    val selectedVersion: String by _selectedVersion
    internal fun updateSelectedVersion(version: String? = null) {
        Napier.v("updateSelectedVersion: $version", tag = "BB2452")
        _selectedVersion.value = version ?: (selectedVersion)
    }

    private var _selectedBookData = mutableStateOf(BookData())
    val selectedBookData: BookData get() = _selectedBookData.value
    internal fun updateBookData(newBookData: BookData) {
        _selectedBookData.value = newBookData
    }

    private var _selectedChapter: MutableState<String> = mutableStateOf("")
    val selectedChapter: String by _selectedChapter

    internal fun updateSelectedChapter(chapter: String? = null) {
        Napier.v("updateSelectedChapter: $chapter", tag = "BB2452")
        _selectedChapter.value = chapter ?: (selectedBookData.bookId + ".1")
    }


    var readingHistory by mutableStateOf<List<ReadingHistoryUIState>?>(null)
        private set

    fun updateReadingHistory(newHistory: List<SelectReadingHistory>) {
        readingHistory = newHistory.map {
            if (it.b == null || it.c == null) return@map ReadingHistoryUIState()
            ReadingHistoryUIState(
                bookName = bibleBooks.data?.get(it.b.toInt() - 1)?.name,
                chapterId = it.c.toInt(),
                date = transformDate(it.DATE)
            )
        }
    }

    private fun transformDate(dateString: String): String {
        val localDate = LocalDate.parse(dateString)
        return localDate.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + localDate.dayOfMonth + ", " + localDate.year
    }

}