package data.bibleIQ

import data.apiBible.BibleAPIDataModel

object BibleIQDataModel {

    var bibleChapter = mutableListOf<BibleChapter>()
        private set

    fun updateBibleChapter(newChapter: List<BibleChapter>) {
        bibleChapter = newChapter.toMutableList()
    }

    fun getAPIBibleCardinal(chapterNumber: String, bibleId: String): Int {
        val name = chapterNumber.substringBefore(".")
        return BibleAPIDataModel.books.data?.indexOfFirst { it.bookId == name } ?: 0
    }
}