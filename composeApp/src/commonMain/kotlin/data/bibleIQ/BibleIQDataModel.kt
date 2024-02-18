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

    var bibleChapter by mutableStateOf<BibleChapterUIState?>(BibleChapterUIState())
        private set

    fun updateBibleChapter(newChapter: List<BibleChapter>) {
        bibleChapter = newChapter.firstOrNull()?.b?.toInt()?.let { bookId ->
            BibleChapterUIState(
                id = newChapter.firstOrNull()?.id,
                bookId = bookId,
                chapterId = newChapter.firstOrNull()?.c?.toInt(),
                text = newChapter.joinToString(" ") { "[${it.v}] ${it.t}" },
                chapterList = newChapter.mapNotNull { it.v?.toInt() }
            )
        }
    }

    fun getAPIBibleCardinal(chapterNumber: String): Int {
        val name = chapterNumber.substringBefore(".")
        return (BibleAPIDataModel.books.data?.indexOfFirst { it.bookId == name } ?: 1).plus(1)
    }
}