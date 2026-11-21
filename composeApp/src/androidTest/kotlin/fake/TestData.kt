package fake

import data.bible.BibleBooksResponse
import data.bible.BibleChapterUIState
import data.bible.BookData
import data.bible.Chapter

object TestData {
    private const val BIBLE_ID = "de4e12af7f28f599-02"

    private fun book(id: String, abbreviation: String, name: String) = BookData(
        id = id,
        bibleId = BIBLE_ID,
        abbreviation = abbreviation,
        name = name,
        nameLong = name,
        // Production book data includes an introductory entry before chapter 1.
        chapters = listOf(
            Chapter(name = "$id.intro", bibleId = BIBLE_ID, bookId = id, number = "intro"),
            Chapter(name = "$id.1", bibleId = BIBLE_ID, bookId = id, number = "1"),
            Chapter(name = "$id.2", bibleId = BIBLE_ID, bookId = id, number = "2"),
        ),
    )

    val testBooks = listOf(
        book("GEN", "Gen", "Genesis"),
        book("EXO", "Exo", "Exodus"),
        book("MAT", "Mat", "Matthew"),
    )

    val testBibleBooks = BibleBooksResponse(data = testBooks)

    val testBibleChapterUIState = BibleChapterUIState(
        id = "GEN.1",
        bookId = 1,
        chapterId = 1,
        text = "[1] In the beginning God created the heaven and the earth.",
        chapterList = listOf(1L, 2L),
    )
}
