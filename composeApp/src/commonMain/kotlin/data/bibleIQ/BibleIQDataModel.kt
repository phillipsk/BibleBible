package data.bibleIQ

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.apiBible.BibleAPIDataModel

object BibleIQDataModel {

    var bibleChapter by mutableStateOf(BibleChapterUIState())
        private set

    fun updateBibleChapter(newChapter: List<BibleChapter>) {
        bibleChapter = BibleChapterUIState(
            id = newChapter.first().id,
            bookId = newChapter.first().b,
            chapterId = newChapter.first().c,
            text = newChapter.map { it.t }.joinToString(),
            chapterList = newChapter.mapNotNull { it.v?.toInt() }
        )
    }

    fun getAPIBibleCardinal(chapterNumber: String): Int {
        val name = chapterNumber.substringBefore(".")
        return BibleAPIDataModel.books.data?.indexOfFirst { it.bookId == name } ?: 0
    }
}