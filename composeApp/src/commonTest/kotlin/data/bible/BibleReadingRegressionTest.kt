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
    @Test
    fun dailyVerseRefreshRevalidatesAndKeepsTheCachedVerseAfterFailure() = runTest {
        val day = "2026-11-24"
        var refreshCalls = 0
        val repo = DailyVerseRepository(
            fetch = { DailyVerse(it, day, DailyVerseData(23, 40, 31, "Original verse", "ISA")) },
            today = { day },
            refreshFetch = { version, after ->
                refreshCalls++
                assertEquals(if (refreshCalls == 1) 31 else 30, after?.verse)
                if (refreshCalls == 2) error("Offline")
                DailyVerse(version, day, DailyVerseData(23, 40, 30, "Updated verse", "ISA"))
            },
        )
        assertEquals("Original verse", repo.get("KJV").data.text)
        assertEquals("Updated verse", repo.get("KJV", refresh = true).data.text)
        assertFailsWith<IllegalStateException> { repo.get("KJV", refresh = true) }
        assertEquals("Updated verse", repo.get("KJV").data.text)
        assertEquals(2, refreshCalls)
    }

    @Test
    fun dailyVerseRepositoryPersistsAcrossColdStartsAndFallsBackOffline() = runTest {
        val prefs = data.shorts.InMemoryShortsPrefsBacking()
        var currentDay = "2026-11-25"
        var networkCalls = 0

        val repo1 = DailyVerseRepository(
            fetch = { version ->
                networkCalls++
                DailyVerse(version, currentDay, DailyVerseData(43, 3, 16, "For God so loved the world", "JHN"))
            },
            today = { currentDay },
            prefs = prefs,
        )
        assertEquals("For God so loved the world", repo1.get("KJV").data.text)
        assertEquals(1, networkCalls)

        // Same-day cold start hits persisted storage with zero network calls even when offline.
        val repo2 = DailyVerseRepository(
            fetch = { error("offline") },
            today = { currentDay },
            prefs = prefs,
        )
        assertEquals("For God so loved the world", repo2.get("KJV").data.text)

        // Manual refresh updates persisted storage so subsequent cold starts see the new verse.
        val repo3 = DailyVerseRepository(
            fetch = { error("unused") },
            today = { currentDay },
            refreshFetch = { version, after ->
                assertEquals(16, after?.verse)
                DailyVerse(version, currentDay, DailyVerseData(19, 23, 1, "The Lord is my shepherd", "PSA"))
            },
            prefs = prefs,
        )
        assertEquals("The Lord is my shepherd", repo3.get("KJV", refresh = true).data.text)

        val repo4 = DailyVerseRepository(
            fetch = { error("should hit same-day cache") },
            today = { currentDay },
            prefs = prefs,
        )
        assertEquals("The Lord is my shepherd", repo4.get("KJV").data.text)

        // Next day offline launch gracefully falls back to last persisted verse instead of failing.
        currentDay = "2026-11-26"
        val offlineNextDayRepo = DailyVerseRepository(
            fetch = { error("network unavailable") },
            today = { currentDay },
            prefs = prefs,
        )
        assertEquals("The Lord is my shepherd", offlineNextDayRepo.get("KJV").data.text)
    }

}
