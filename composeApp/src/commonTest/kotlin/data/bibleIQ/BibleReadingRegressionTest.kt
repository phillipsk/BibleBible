package data.bibleIQ

import data.apiBible.json.JSON_BOOKS_API_BIBLE
import JSON_VERSIONS
import data.apiBible.BibleAPIBook
import data.apiBible.BibleAPIDataModel
import kotlinx.serialization.json.Json
import kotlin.test.*

class BibleReadingRegressionTest {
    private val originalBooks = BibleAPIDataModel.bibleBooks
    private val originalSort = BibleIQDataModel.sortAZ
    private val originalVersion = BibleIQDataModel.selectedVersion

    @AfterTest fun restore() {
        BibleAPIDataModel.updateBooks(originalBooks)
        BibleIQDataModel.sortAZ = originalSort
        BibleIQDataModel.updateSelectedVersion(originalVersion)
        BibleIQDataModel.updateBibleChapter(emptyList(), null, "KJV")
    }

    @Test fun everyCanonicalBookMapsToItsIqOrdinalEvenWhenSorted() {
        val books = Json.decodeFromString<BibleAPIBook>(JSON_BOOKS_API_BIBLE)
        BibleAPIDataModel.updateBooks(books)
        assertEquals(66, books.data!!.size)
        for (sorted in listOf(false, true)) {
            BibleIQDataModel.sortAZ = sorted
            books.data!!.forEachIndexed { index, book ->
                assertEquals(index + 1, BibleIQDataModel.getAPIBibleOrdinal(book.remoteKey), book.name)
            }
        }
    }

    @Test fun bundledVersionsContainRequestedTranslations() {
        val versions = Json.decodeFromString<List<BibleIQVersion>>(JSON_VERSIONS)
        assertTrue(versions.map { it.abbreviation }.containsAll(listOf("KJV", "BBE", "ASV", "RV1909", "SVD")))
    }

    @Test fun chapterFormattingPreservesVerseNumbersAndUnicode() {
        val verses = listOf(BibleChapter("1", "43", "3", "16", "لأن الله"), BibleChapter("2", "43", "3", "18", "العالم"))
        BibleIQDataModel.updateBibleChapter(verses, ChapterCount(21), "svd")
        assertEquals("16 لأن الله\n18 العالم", BibleIQDataModel.bibleChapter?.text)
        BibleIQDataModel.updateBibleChapter(verses, ChapterCount(21), "BBE")
        assertEquals("[16] لأن الله [18] العالم", BibleIQDataModel.bibleChapter?.text)
        assertEquals((1L..21L).toList(), BibleIQDataModel.bibleChapter?.chapterList)
    }

    @Test fun emptyChapterClearsPreviousPassage() {
        BibleIQDataModel.updateBibleChapter(listOf(BibleChapter("1", "1", "1", "1", "Previous")), ChapterCount(50), "KJV")
        BibleIQDataModel.updateBibleChapter(emptyList(), null, "KJV")
        assertNull(BibleIQDataModel.bibleChapter)
    }

    @Test fun nullVersionUpdatePreservesUserChoice() {
        BibleIQDataModel.updateSelectedVersion("BBE")
        BibleIQDataModel.updateSelectedVersion(null)
        assertEquals("BBE", BibleIQDataModel.selectedVersion)
    }
}
