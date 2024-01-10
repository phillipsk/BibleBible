package data.bibleIQ

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.api.bible.BibleAPIBibles
import data.api.bible.BibleAPIBook
import data.api.bible.BookData
import data.api.bible.ChapterContent
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleIQ {
    const val MVP_UI = true
    private const val DEFAULT_BIBLE_ID = "de4e12af7f28f599-02"
    var selectedLanguage: MutableState<String>? = if (MVP_UI) mutableStateOf("eng") else null

    var uiState by mutableStateOf(UIState())
    var selectedBibleId = mutableStateOf(DEFAULT_BIBLE_ID)
    var chapter = mutableStateOf(ChapterContent())
    var books = mutableStateOf(BibleAPIBook())
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
    var selectedBookData = mutableStateOf(BookData())

    private var _selectedChapter: MutableState<String> = mutableStateOf("")
    val selectedChapter: String by _selectedChapter

    fun updateSelectedChapter(chapter: String? = null) {
        _selectedChapter.value = selectedBookData.value.bookId + "." + (chapter ?: "1")
    }

    fun updateBooksView() {
        uiState = uiState.copy(updateView = true, selectedBookData = selectedBookData.value)
        chapter.value = ChapterContent()
    }

}

data class UIState(
    var updateView: Boolean = false,
    var selectedVersion: MutableState<String> = mutableStateOf("KJV"),
    var selectedBookData: BookData = BookData(),
    var selectedChapter: MutableState<Int> = mutableStateOf(-1),
    var selectedChapterString: MutableState<String> = mutableStateOf(""),
)