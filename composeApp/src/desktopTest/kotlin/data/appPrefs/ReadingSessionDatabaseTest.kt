package data.appPrefs

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import email.kevinphillips.biblebible.cache.openDesktopDatabase
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadingSessionDatabaseTest {
    private val reading = SavedReadingSession("PSA", 119, "ASV", 5067, 0.5f, lastActiveMillis = 1_800_000_000_000L)

    @Test
    fun readingLocationAndAnchorSurviveClosingAndReopeningTheDatabase() {
        val path = Files.createTempFile("biblebible-reading-", ".db")
        try {
            openDesktopDatabase(path).use { driver ->
                val database = BibleBibleDatabase(driver)
                database.bibleBibleDatabaseQueries.insertUserPrefs()
                database.bibleBibleDatabaseQueries.updateUserPrefs(26.0, "KJV")
                ReadingSessionStore(database).write(reading)
            }
            openDesktopDatabase(path).use { driver ->
                val database = BibleBibleDatabase(driver)
                assertEquals(reading, ReadingSessionStore(database).read())
                val preferences = database.bibleBibleDatabaseQueries.selectUserPrefs().executeAsOne()
                assertEquals(26.0, preferences.fontSize)
                assertEquals("KJV", preferences.bibleVersion)
            }
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun migrationFromReleasedSchemaPreservesPreferencesAndReadingHistory() {
        val path = Files.createTempFile("biblebible-migration-", ".db")
        try {
            JdbcSqliteDriver("jdbc:sqlite:$path").use { driver ->
                BibleBibleDatabase.Schema.create(driver)
                // The released schema contains the same existing tables but no session table.
                driver.execute(null, "DROP TABLE SavedReadingPosition", 0)
                driver.execute(null, "PRAGMA user_version = 6", 0)
                val database = BibleBibleDatabase(driver)
                database.bibleBibleDatabaseQueries.insertUserPrefs()
                database.bibleBibleDatabaseQueries.updateUserPrefs(30.0, "SVD")
                database.bibleBibleDatabaseQueries.insertReadingHistory("2026-10-28 12:00:00", "19", "119")
            }
            openDesktopDatabase(path).use { driver ->
                assertEquals(7L, BibleBibleDatabase.Schema.version)
                val database = BibleBibleDatabase(driver)
                val preferences = database.bibleBibleDatabaseQueries.selectUserPrefs().executeAsOne()
                assertEquals(30.0, preferences.fontSize)
                assertEquals("SVD", preferences.bibleVersion)
                assertEquals(1, database.bibleBibleDatabaseQueries.selectReadingHistory().executeAsList().size)
                val store = ReadingSessionStore(database)
                assertNull(store.read())
                store.write(reading)
                assertEquals(reading, store.read())
            }
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun newerNavigationReplacesTheSingleSavedLocationAndHomeCanBeSaved() {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BibleBibleDatabase.Schema.create(driver)
            val store = ReadingSessionStore(BibleBibleDatabase(driver))
            store.write(reading)
            val latest = reading.copy(bookKey = "JHN", chapter = 3, characterOffset = 20, isHome = true)
            store.write(latest)
            assertEquals(latest, store.read())
            store.clear()
            assertNull(store.read())
        }
    }
}
