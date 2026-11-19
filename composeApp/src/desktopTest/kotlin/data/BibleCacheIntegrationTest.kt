package data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import kotlin.test.*

/** Runs the generated production SQL against a fresh real SQLite database per test. */
class BibleCacheIntegrationTest {
    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { BibleBibleDatabase.Schema.create(it) }
    private val queries = BibleBibleDatabase(driver).bibleBibleDatabaseQueries
    @AfterTest fun close() = driver.close()

    private fun verse(id: String, chapter: String, version: String, date: String = "2026-11-01") {
        queries.insertVerse("$id-$version", id, "1", chapter, "1", "$version text", version, 50, date, date)
    }

    @Test fun cachedChaptersAreIsolatedByTranslationAndChapter() {
        verse("01001001", "1", "kjv")
        verse("01001001", "1", "bbe")
        verse("01002001", "2", "kjv")
        assertEquals("bbe text", queries.selectVersesByBookId("1", "1", "bbe").executeAsList().single().t)
        assertEquals("01002001", queries.selectVersesByBookId("1", "2", "kjv").executeAsList().single().id)
        assertTrue(queries.selectVersesByBookId("2", "1", "kjv").executeAsList().isEmpty())
    }

    @Test fun chapterCountsAreIsolatedByVersion() {
        queries.insertChapterCount("1-kjv", "1", "kjv", 50, "2026-11-01")
        queries.insertChapterCount("1-bbe", "1", "bbe", 50, "2026-11-01")
        assertEquals(50, queries.countVersesByBookId("1", "bbe").executeAsOne().chapterCount)
        assertNull(queries.countVersesByBookId("2", "bbe").executeAsOneOrNull())
    }

    @Test fun cacheLimitRemovesOldestEntries() {
        verse("1", "1", "kjv", "2026-01-01")
        verse("2", "2", "kjv", "2026-02-01")
        verse("3", "3", "kjv", "2026-03-01")
        queries.removeExcessVerses(2)
        assertEquals(2, queries.countVerses().executeAsOne())
        assertTrue(queries.selectVersesByBookId("1", "1", "kjv").executeAsList().isEmpty())
    }

    @Test fun preferencesDefaultToKjvAndPersistBbe() {
        queries.insertUserPrefs()
        assertEquals("KJV", queries.selectUserPrefs().executeAsOne().bibleVersion)
        queries.updateUserPrefsBibleVersion("BBE")
        assertEquals("BBE", queries.selectUserPrefs().executeAsOne().bibleVersion)
    }

    @Test fun historyIsReturnedNewestFirst() {
        queries.insertReadingHistory("2026-11-01 10:00:00", "1", "1")
        queries.insertReadingHistory("2026-11-02 10:00:00", "43", "3")
        assertEquals(listOf("43", "1"), queries.selectReadingHistory().executeAsList().map { it.b })
    }
}
