package data.bibleIQ

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.BiblePagerUiState
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleIQDataModel {

    var biblePagerUiState: BiblePagerUiState by mutableStateOf(BiblePagerUiState.Loading)
        private set

/*    fun updateBiblePagerState(newState: BiblePagerState) {
        when (newState) {
            is BiblePagerState.Success -> {
                val chapterCount = newState.chapterCount
                updateBibleChapter(newState.bibleChapter, chapterCount)
            }

            else -> {}
        }
        biblePagerState = newState
    }*/

    var bibleVersions by mutableStateOf(BibleIQVersions())
        private set

    fun updateBibleVersions(newVersions: BibleIQVersions) {
        bibleVersions = newVersions
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
        Napier.v("updateSelectedVersion: $version", tag = "IQ2452")
        _selectedVersion.value = version ?: (selectedVersion)
    }

    var bibleBooks by mutableStateOf(BibleIQBooks())
        private set

    fun updateBibleBooks(newBooks: BibleIQBooks) {
        bibleBooks = newBooks
    }

    var selectedBook by mutableStateOf(BookData())
        private set

    fun updateSelectedBook(newBook: BookData) {
        selectedBook = newBook
    }

    var bibleChapter by mutableStateOf<BibleChapterUIState?>(BibleChapterUIState())
        private set

    fun getAPIBibleCardinal(chapterNumber: String): Int {
        val name = chapterNumber.substringBefore(".")
        return (BibleAPIDataModel.books.data?.indexOfFirst { it.bookId == name } ?: 1).plus(1)
    }

    suspend fun fetchBibleChapter(selectedBook: BookData, chapter: Int, bibleVersion: String) {
        Napier.v(
            "fetchBibleChapter: selectedBook: ${selectedBook.name} :: chapter: $chapter",
            tag = "BB2452"
        )
        coroutineScope {
            biblePagerUiState = BiblePagerUiState.Loading
            launch(Dispatchers.IO) {
                try {
                    val data = getChapterBibleIQ(selectedBook.bookId, chapter, bibleVersion).let {
                        it ?: throw Exception("Error fetching chapter")
                    }
//                    updateBibleChapter(chapters, chapterCount)
                    withContext(Dispatchers.Main) {
//                        updateBiblePagerState(data)
                        biblePagerUiState = BiblePagerUiState.Success(data)
                    }
                } catch (e: Exception) {
                    Napier.e("Error: ${e.message}", tag = "BB2452")
                    biblePagerUiState = BiblePagerUiState.Error(e.message ?: "Error")
                }
            }
        }
    }
}