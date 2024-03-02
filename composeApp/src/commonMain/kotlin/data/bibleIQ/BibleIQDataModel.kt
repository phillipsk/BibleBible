package data.bibleIQ

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import io.github.aakira.napier.Napier
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleIQDataModel {
    const val RELEASE_BUILD = false
    const val DEFAULT_BIBLE_ID = "kjv"
    var bibleVersions by mutableStateOf(BibleIQVersions())
        private set

    fun updateBibleVersions(newVersions: BibleIQVersions) {
        val list = newVersions.data.filter {
            it.abbreviation == "KJV" || it.abbreviation == "ASV"
                    || it.abbreviation == "RV1909" || it.abbreviation == "SVD"
        }
        bibleVersions = BibleIQVersions(data = list)
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

    fun updateBibleChapter(newChapter: List<BibleChapter>, chapterCount: ChapterCount?) {
        bibleChapter = newChapter.firstOrNull()?.b?.toInt()?.let { bookId ->
            BibleChapterUIState(
                id = newChapter.firstOrNull()?.id,
                bookId = bookId,
                chapterId = newChapter.firstOrNull()?.c?.toInt(),
                text = newChapter.joinToString(" ") { "[${it.v}] ${it.t}" },
                chapterList = chapterCount?.chapterCount?.let { count ->
                    (1..count).toList()
                }
            )
        }
    }

    fun getAPIBibleCardinal(chapterNumber: String): Int {
        val name = chapterNumber.substringBefore(".")
        return (BibleAPIDataModel.books.data?.indexOfFirst { it.bookId == name } ?: 1).plus(1)
    }

    val onHomeClick: () -> Unit = {
        Napier.v("onHomeClick", tag = "BB2452")
        BibleAPIDataModel.showHomePage = true
    }

    var errorSnackBar: String by mutableStateOf("")
        private set
    internal fun updateErrorSnackBar(error: String) {
        Napier.v("updateErrorSnackBar: $error", tag = "BB2452")
        errorSnackBar = error
    }
    internal fun clearErrorSnackBar() {
        errorSnackBar = ""
    }
}