package data.bible

import data.bible.json.BUNDLED_BIBLE_BOOKS_JSON
import data.bible.json.BUNDLED_BIBLE_VERSIONS_JSON
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BibleReadingRegressionTest {
    private val originalBooks = BibleState.bibleBooks
    private val originalSort = BibleState.sortAZ
    private val originalVersion = BibleState.selectedVersion

    @AfterTest
    fun restore() {
        BibleState.updateBooks(originalBooks)
        BibleState.sortAZ = originalSort
        BibleState.updateSelectedVersion(originalVersion)
        BibleState.updateBibleChapter(emptyList(), null, "KJV")
    }

    @Test
    fun everyCanonicalBookMapsToItsOrdinalEvenWhenSorted() {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        BibleState.updateBooks(books)
        assertEquals(66, books.data!!.size)
        for (sorted in listOf(false, true)) {
            BibleState.sortAZ = sorted
            books.data!!.forEachIndexed { index, book ->
                assertEquals(index + 1, BibleState.getBookOrdinal(book.remoteKey), book.name)
            }
        }
    }

    @Test
    fun bundledVersionsContainLeanSixPublicDomainTranslations() {
        val versions = Json.decodeFromString<List<BibleVersion>>(BUNDLED_BIBLE_VERSIONS_JSON)
        assertEquals(listOf("KJV", "BSB", "WEB", "BBE", "RV1909", "SVD"), versions.map { it.abbreviation })
    }

    @Test
    fun chapterFormattingPreservesVerseNumbersAndUnicode() {
        val verses = listOf(
            BibleChapter("1", "43", "3", "16", "لأن الله"),
            BibleChapter("2", "43", "3", "18", "العالم"),
        )
        BibleState.updateBibleChapter(verses, ChapterCount(21), "svd")
        assertEquals("16 لأن الله\n18 العالم", BibleState.bibleChapter?.text)
        BibleState.updateBibleChapter(verses, ChapterCount(21), "BBE")
        assertEquals("[16] لأن الله [18] العالم", BibleState.bibleChapter?.text)
        assertEquals((1L..21L).toList(), BibleState.bibleChapter?.chapterList)
    }

    @Test
    fun emptyChapterClearsPreviousPassage() {
        BibleState.updateBibleChapter(listOf(BibleChapter("1", "1", "1", "1", "Previous")), ChapterCount(50), "KJV")
        BibleState.updateBibleChapter(emptyList(), null, "KJV")
        assertNull(BibleState.bibleChapter)
    }

    @Test
    fun nullVersionPreservesChoiceAndUnsupportedVersionFallsBackToKjv() {
        BibleState.updateSelectedVersion("BSB")
        BibleState.updateSelectedVersion(null)
        assertEquals("BSB", BibleState.selectedVersion)

        BibleState.updateSelectedVersion("UNKNOWN_LEGACY")
        assertEquals("KJV", BibleState.selectedVersion)
    }

    @Test
    fun dailyVerseRepositoryCachesPerDateAndTranslationAndRecoversFromErrors() = runTest {
        var currentDay = "2026-11-17"
        var calls = 0
        var shouldFail = true
        val repo = DailyVerseRepository(
            fetch = { version ->
                calls++
                if (shouldFail) error("network down")
                DailyVerse(version, currentDay, DailyVerseData(43, 3, 16, "Verse in $version ($currentDay)", "JHN"))
            },
            today = { currentDay },
        )

        assertFailsWith<IllegalStateException> { repo.get("BSB") }
        shouldFail = false

        val first = repo.get("bsb")
        val second = repo.get("BSB")
        assertEquals(first, second)
        assertEquals(2, calls)

        currentDay = "2026-11-18"
        val nextDay = repo.get("BSB")
        assertEquals("2026-11-18", nextDay.date)
        assertTrue(calls == 3)
    }
}
