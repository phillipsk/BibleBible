package data.apiBible

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.bibleIQ.BibleIQDataModel
import email.kevinphillips.biblebible.db.SelectReadingHistory
import io.github.aakira.napier.Napier
import kotlinx.datetime.LocalDate
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleAPIDataModel {
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