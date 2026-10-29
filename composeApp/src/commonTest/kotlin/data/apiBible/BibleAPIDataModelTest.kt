package data.apiBible

import email.kevinphillips.biblebible.db.SelectReadingHistory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BibleAPIDataModelTest {

    @Test
    fun testUpdateReadingHistory() {
        // Using positional arguments for BookData if named for private 'id' fails
        // BookData(id, bibleId, abbreviation, name, nameLong, chapters)
        val mockBooks = BibleAPIBook(
            data = listOf(
                BookData(null, null, null, "Genesis", null, emptyList()),
                BookData(null, null, null, "Exodus", null, emptyList())
            )
        )
        BibleAPIDataModel.updateBooks(mockBooks)

        val history = listOf(
            SelectReadingHistory(b = "1", c = "1", DATE = "2023-10-01"),
            SelectReadingHistory(b = "2", c = "5", DATE = "2023-10-02")
        )

        BibleAPIDataModel.updateReadingHistory(history)

        val result = BibleAPIDataModel.readingHistory
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
        assertTrue(BibleAPIDataModel.singleChapterBooksOrdinal.contains("31"))
        assertTrue(BibleAPIDataModel.singleChapterBooksOrdinal.contains("57"))
    }
}
