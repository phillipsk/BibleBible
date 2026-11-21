package data.bible

import email.kevinphillips.biblebible.db.SelectReadingHistory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BibleStateTest {
    private val originalBooks = BibleState.bibleBooks
    private val originalVersions = BibleState.bibleVersions
    private val originalSort = BibleState.sortAZ

    @AfterTest
    fun resetState() {
        BibleState.updateBooks(originalBooks)
        BibleState.updateBibleVersions(originalVersions)
        BibleState.sortAZ = originalSort
        BibleState.updateBibleChapter(emptyList(), null, "KJV")
    }

    @Test
    fun testUpdateBibleVersions() {
        val versions = BibleVersions(
            data = listOf(
                BibleVersion(null, null, "KJV", "King James Version", null),
                BibleVersion(null, null, "BSB", "Berean Standard Bible", null),
                BibleVersion(null, null, "WEB", "World English Bible", null),
            )
        )

        BibleState.updateBibleVersions(versions)
        assertEquals(3, BibleState.bibleVersions.data.size)
    }

    @Test
    fun testUpdateBibleChapterFormatsStandardAndArabicVerses() {
        val verses = listOf(
            BibleChapter(id = "1", b = "1", c = "1", v = "1", t = "In the beginning..."),
            BibleChapter(id = "2", b = "1", c = "1", v = "2", t = "And the earth..."),
        )

        BibleState.updateBibleChapter(verses, ChapterCount(chapterCount = 50), "KJV")
        val kjvState = BibleState.bibleChapter
        assertEquals(1, kjvState?.bookId)
        assertEquals(1, kjvState?.chapterId)
        assertTrue(kjvState?.text?.contains("[1] In the beginning...") == true)
        assertTrue(kjvState?.text?.contains("[2] And the earth...") == true)
        assertEquals(50, kjvState?.chapterList?.size)

        BibleState.updateBibleChapter(listOf(verses.first()), ChapterCount(1), "SVD")
        assertEquals("1 In the beginning...", BibleState.bibleChapter?.text)
    }

    @Test
    fun testUpdateBibleChapterFiltersEmptyAndBbeBracketPlaceholders() {
        val verses = listOf(
            BibleChapter(id = "40017020", b = "40", c = "17", v = "20", t = "And he said to them..."),
            BibleChapter(id = "40017021", b = "40", c = "17", v = "21", t = "[]"),
            BibleChapter(id = "40017022", b = "40", c = "17", v = "22", t = "   "),
            BibleChapter(id = "40017023", b = "40", c = "17", v = "23", t = "And they were very sad."),
        )

        BibleState.updateBibleChapter(verses, ChapterCount(28), "BBE")
        assertEquals(
            "[20] And he said to them... [23] And they were very sad.",
            BibleState.bibleChapter?.text,
        )
    }

    @Test
    fun testSortToggle() {
        BibleState.sortAZ = false
        assertEquals("OT-NT", BibleState.selectedSortType)

        BibleState.sortAZ = true
        assertEquals("A-Z", BibleState.selectedSortType)
    }

    @Test
    fun testUpdateReadingHistory() {
        val mockBooks = BibleBooksResponse(
            data = listOf(
                BookData(null, null, null, "Genesis", null, emptyList()),
                BookData(null, null, null, "Exodus", null, emptyList()),
            )
        )
        BibleState.updateBooks(mockBooks)

        val history = listOf(
            SelectReadingHistory(b = "1", c = "1", DATE = "2023-10-01"),
            SelectReadingHistory(b = "2", c = "5", DATE = "2023-10-02"),
        )

        BibleState.updateReadingHistory(history)

        val result = BibleState.readingHistory
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("Genesis", result[0].bookName)
        assertEquals(1, result[0].chapterId)
        assertEquals("October 1, 2023", result[0].date)

        assertEquals("Exodus", result[1].bookName)
        assertEquals(5, result[1].chapterId)
        assertEquals("October 2, 2023", result[1].date)
    }

    @Test
    fun testSingleChapterBooks() {
        assertTrue(BibleState.singleChapterBooksOrdinal.contains("31"))
        assertTrue(BibleState.singleChapterBooksOrdinal.contains("57"))
    }
}
