package data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Runs the generated production SQL against a fresh real SQLite database per test. */
class BibleCacheIntegrationTest {
    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { BibleBibleDatabase.Schema.create(it) }
    private val queries = BibleBibleDatabase(driver).bibleBibleDatabaseQueries

    @AfterTest
    fun close() = driver.close()

    private fun verse(id: String, chapter: String, version: String, date: String = "2026-11-01") {
        queries.insertVerse("$id-$version", id, "1", chapter, "1", "$version text", version, 50, date, date)
    }

    @Test
    fun cachedChaptersAreIsolatedByTranslationAndChapter() {
        verse("01001001", "1", "kjv")
        verse("01001001", "1", "bsb")
        verse("01002001", "2", "kjv")
        assertEquals("bsb text", queries.selectVersesByBookId("1", "1", "bsb").executeAsList().single().t)
        assertEquals("01002001", queries.selectVersesByBookId("1", "2", "kjv").executeAsList().single().id)
        assertTrue(queries.selectVersesByBookId("2", "1", "kjv").executeAsList().isEmpty())
    }

    @Test
    fun chapterCountsAreIsolatedByVersion() {
        queries.insertChapterCount("1-kjv", "1", "kjv", 50, "2026-11-01")
        queries.insertChapterCount("1-bsb", "1", "bsb", 50, "2026-11-01")
        assertEquals(50, queries.countVersesByBookId("1", "bsb").executeAsOne().chapterCount)
        assertNull(queries.countVersesByBookId("2", "bsb").executeAsOneOrNull())
    }

    @Test
    fun cacheLimitRemovesLeastRecentlyUpdatedEntries() {
        verse("1", "1", "kjv", "2026-01-01 09:00:00")
        verse("2", "2", "kjv", "2026-02-01 09:00:00")
        // Re-reading chapter 1 updates its updated_at so chapter 2 becomes least recently used.
        queries.updateVerseTimestamp("2026-03-15 12:00:00", "1", "1", "kjv")
        verse("3", "3", "kjv", "2026-03-01 09:00:00")

        queries.removeExcessVerses(2)

        assertEquals(2, queries.countVerses().executeAsOne())
        assertEquals(1, queries.selectVersesByBookId("1", "1", "kjv").executeAsList().size)
        assertTrue(queries.selectVersesByBookId("1", "2", "kjv").executeAsList().isEmpty())
        assertEquals(1, queries.selectVersesByBookId("1", "3", "kjv").executeAsList().size)
    }

    @Test
    fun multiVersionChaptersCoexistIndependentlyForSameBookAndChapter() {
        queries.insertVerse("43003016-kjv", "43003016", "43", "3", "16", "For God so loved", "kjv", 21, "2026-11-01", "2026-11-01")
        queries.insertVerse("43003016-bsb", "43003016", "43", "3", "16", "For God so loved the world", "bsb", 21, "2026-11-01", "2026-11-01")
        queries.insertVerse("43003016-bbe", "43003016", "43", "3", "16", "For God had such love", "bbe", 21, "2026-11-01", "2026-11-01")

        assertEquals("For God so loved", queries.selectVersesByBookId("43", "3", "kjv").executeAsList().single().t)
        assertEquals("For God so loved the world", queries.selectVersesByBookId("43", "3", "bsb").executeAsList().single().t)
        assertEquals("For God had such love", queries.selectVersesByBookId("43", "3", "bbe").executeAsList().single().t)

        queries.updateVerseTimestamp("2026-11-20 12:00:00", "43", "3", "kjv")
        assertEquals("2026-11-01", queries.selectVersesByBookId("43", "3", "bsb").executeAsList().single().updated_at)
    }

    @Test
    fun preferencesDefaultToKjvAndPersistSelectedVersion() {
        queries.insertUserPrefs()
        assertEquals("KJV", queries.selectUserPrefs().executeAsOne().bibleVersion)
        queries.updateUserPrefsBibleVersion("BSB")
        assertEquals("BSB", queries.selectUserPrefs().executeAsOne().bibleVersion)
    }

    @Test
    fun historyIsReturnedNewestFirst() {
        queries.insertReadingHistory("2026-11-01 10:00:00", "1", "1")
        queries.insertReadingHistory("2026-11-02 10:00:00", "43", "3")
        assertEquals(listOf("43", "1"), queries.selectReadingHistory().executeAsList().map { it.b })
    }
}
