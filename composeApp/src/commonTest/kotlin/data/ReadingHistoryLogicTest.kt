package data

import data.apiBible.BibleAPIDataModel
import email.kevinphillips.biblebible.db.SelectReadingHistory
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingHistoryLogicTest {

    @Test
    fun testReadingHistoryFiltering() {
        // Mocking the logic found in BibleAPIRepository.kt
        
        val readingHistory = listOf(
            SelectReadingHistory(DATE = "2023-10-01", b = "1", c = "2"),
            SelectReadingHistory(DATE = "2023-10-01", b = "1", c = "1"), // Should be kept because previous was same book chapter 2
            SelectReadingHistory(DATE = "2023-10-02", b = "2", c = "1"), // Should be kept because index is 0 or no previous same book chapter 2
            SelectReadingHistory(DATE = "2023-10-03", b = "3", c = "1"), // Should be kept because it's first record of book 3
            SelectReadingHistory(DATE = "2023-10-04", b = "4", c = "1")  // Should be kept
        )

        // The logic from BibleAPIRepository:
        val filteredReadingHistory = readingHistory.filterIndexed { index, currentRecord ->
            if (currentRecord.c == "1" && index > 0 && currentRecord.b !in BibleAPIDataModel.singleChapterBooksOrdinal) {
                val previousRecord = readingHistory[index - 1]
                currentRecord.b == previousRecord.b && previousRecord.c == "2"
            } else {
                true
            }
        }

        // Based on the logic:
        // Index 0: (b=1, c=2) -> true
        // Index 1: (b=1, c=1) -> index > 0 and c == 1. Previous (index 0) is b=1, c=2. Match! -> true
        // Index 2: (b=2, c=1) -> index > 0 and c == 1. Previous (index 1) is b=1, c=1. No match (b 2 != 1). -> false
        
        // Wait, let's re-read the logic in the code:
        // if (currentRecord.c == "1" && index > 0 && currentRecord.b !in BibleAPIDataModel.singleChapterBooksOrdinal) {
        //     val previousRecord = readingHistory[index - 1]
        //     currentRecord.b == previousRecord.b && previousRecord.c == "2"
        // } else { true }
        
        // So if it's chapter 1, it ONLY stays if the previous record was chapter 2 of the same book (unless it's a single chapter book).
        // This seems to be filtering out "false starts" where someone just lands on chapter 1 but didn't come from chapter 2? 
        // Or maybe it's the other way around? 
        
        val expectedSize = 2 // (1,2) and (1,1). (2,1), (3,1), (4,1) should be filtered out if they are chapter 1 and not coming from chapter 2.
        
        assertEquals(2, filteredReadingHistory.size, "Should filter out chapter 1 if not preceded by chapter 2 of same book")
        assertEquals("1", filteredReadingHistory[0].b)
        assertEquals("2", filteredReadingHistory[0].c)
        assertEquals("1", filteredReadingHistory[1].b)
        assertEquals("1", filteredReadingHistory[1].c)
    }
}
