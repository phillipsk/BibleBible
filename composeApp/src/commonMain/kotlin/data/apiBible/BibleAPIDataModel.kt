package data.api.apiBible

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.apiBible.BibleAPIBibles
import data.apiBible.BibleAPIBook
import data.apiBible.BookData
import data.apiBible.ChapterContent
import io.github.aakira.napier.Napier
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleAPIDataModel {
    const val MVP_UI = true
    private const val DEFAULT_BIBLE_ID = "de4e12af7f28f599-02"
    var selectedLanguage: MutableState<String>? = if (MVP_UI) mutableStateOf("eng") else null

    var uiState by mutableStateOf(UIState())
    var selectedBibleId = mutableStateOf(DEFAULT_BIBLE_ID)
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
    val abbreviationList get() = bibleVersions.value.data?.map { it } ?: emptyList()
    var selectedVersion: MutableState<String> = mutableStateOf("")
        get() {
            if (field.value.isEmpty()) {
                field.value = abbreviationList.find {
                    it.abbreviationLocal?.contains("KJV") == true
                }?.abbreviationLocal ?: "KJV"
            }
            println("println :: updated selectedVersion $field")
            return field
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

/*    fun updateBooksView() {
        uiState = uiState.copy(updateView = true, selectedBookData = selectedBookData.value)
        _chapterContent = ChapterContent()
    }*/

}

data class UIState(
    var updateView: Boolean = false,
    var selectedVersion: MutableState<String> = mutableStateOf("KJV"),
    var selectedBookData: BookData = BookData(),
    var selectedChapter: MutableState<Int> = mutableStateOf(-1),
    var selectedChapterString: MutableState<String> = mutableStateOf(""),
)