package data.apiBible

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aakira.napier.Napier
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleAPIDataModel {
    const val DEFAULT_BIBLE_ID = "de4e12af7f28f599-02"
    private var _selectedLanguage: MutableState<String> = mutableStateOf("eng")
    val selectedLanguage by _selectedLanguage

    var showHomePage by mutableStateOf(true)

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

    private var _books = mutableStateOf(BibleAPIBook())
    val books: BibleAPIBook get() = _books.value
    internal fun updateBooks(newBooks: BibleAPIBook) {
        _books.value = newBooks
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


}