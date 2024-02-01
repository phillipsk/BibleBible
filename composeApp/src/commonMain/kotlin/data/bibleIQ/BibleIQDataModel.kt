package data.bibleIQ

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.apiBible.BibleAPIDataModel

object BibleIQDataModel {

    var bibleChapter by mutableStateOf<BibleChapterUIState?>(BibleChapterUIState())
        private set

    fun updateBibleChapter(newChapter: List<BibleChapter>) {

        bibleChapter = newChapter.firstOrNull()?.b?.toInt()?.let { bookId ->
            BibleChapterUIState(
                id = newChapter.firstOrNull()?.id,
                bookId = bookId,
                chapterId = newChapter.firstOrNull()?.c?.toInt(),
                text = newChapter.map { it.t }.joinToString(),
                chapterList = newChapter.mapNotNull { it.v?.toInt() }
            )
        }
    }

    fun getAPIBibleCardinal(chapterNumber: String): Int {
        val name = chapterNumber.substringBefore(".")
        return (BibleAPIDataModel.books.data?.indexOfFirst { it.bookId == name } ?: 1).plus(1)
    }
}