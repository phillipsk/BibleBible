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
    const val RELEASE_BUILD = false
    private var _selectedLanguage: MutableState<String> = mutableStateOf("eng")
    val selectedLanguage by _selectedLanguage

    var uiState by mutableStateOf(UIState())
    private var _selectedBibleId = mutableStateOf(DEFAULT_BIBLE_ID)
    val selectedBibleId: String by _selectedBibleId
    fun updateSelectedBibleId(bibleId: String? = null) {
        Napier.v("updateSelectedBibleId: $bibleId", tag = "BB2452")
        _selectedBibleId.value = bibleId ?: (selectedBibleId)
    }

    private var _chapterContent: MutableState<ChapterContent> = mutableStateOf(ChapterContent())
    val chapterContent: ChapterContent get() = _chapterContent.value
    fun updateChapterContent(newContent: ChapterContent) {
        _chapterContent.value = newContent
    }

    private var _books = mutableStateOf(BibleAPIBook())
    val books: BibleAPIBook get() = _books.value
    fun updateBooks(newBooks: BibleAPIBook) {
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
            println("println :: updated selectedVersion $field")
            return field
        }
    val selectedVersion: String by _selectedVersion
    fun updateSelectedVersion(version: String? = null) {
        Napier.v("updateSelectedVersion: $version", tag = "BB2452")
        _selectedVersion.value = version ?: (selectedVersion)
    }

    private var _selectedBookData = mutableStateOf(BookData())
    val selectedBookData: BookData get() = _selectedBookData.value
    fun updateBookData(newBookData: BookData) {
        _selectedBookData.value = newBookData
    }

    private var _selectedChapter: MutableState<String> = mutableStateOf("")
    val selectedChapter: String by _selectedChapter

    fun updateSelectedChapter(chapter: String? = null) {
        Napier.v("updateSelectedChapter: $chapter", tag = "BB2452")
        _selectedChapter.value = chapter ?: (selectedBookData.bookId + ".1")
//        _selectedChapter.value = selectedBookData.value.bookId + "." + (chapter ?: "1")
    }
}

data class UIState(
    var updateView: Boolean = false,
    var selectedVersion: MutableState<String> = mutableStateOf("KJV"),
    var selectedBookData: BookData = BookData(),
    var selectedChapter: MutableState<Int> = mutableStateOf(-1),
    var selectedChapterString: MutableState<String> = mutableStateOf(""),
)