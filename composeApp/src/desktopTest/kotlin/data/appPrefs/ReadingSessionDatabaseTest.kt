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
                // The released schema contains the same existing tables but no session, seen-words, or reading-plans tables.
                driver.execute(null, "DROP TABLE SavedReadingPosition", 0)
                driver.execute(null, "DROP TABLE SeenBiblicalWords", 0)
                driver.execute(null, "DROP TABLE ActiveReadingPlan", 0)
                driver.execute(null, "DROP TABLE ReadingPlanProgress", 0)
                driver.execute(null, "DROP TABLE BiblicalShortEngagement", 0)
                driver.execute(null, "PRAGMA user_version = 6", 0)
                val database = BibleBibleDatabase(driver)
                database.bibleBibleDatabaseQueries.insertUserPrefs()
                database.bibleBibleDatabaseQueries.updateUserPrefs(30.0, "SVD")
                database.bibleBibleDatabaseQueries.insertReadingHistory("2026-10-28 12:00:00", "19", "119")
            }
            openDesktopDatabase(path).use { driver ->
                assertEquals(10L, BibleBibleDatabase.Schema.version)
                val database = BibleBibleDatabase(driver)
                val preferences = database.bibleBibleDatabaseQueries.selectUserPrefs().executeAsOne()
                assertEquals(30.0, preferences.fontSize)
                assertEquals("SVD", preferences.bibleVersion)
                assertEquals(1, database.bibleBibleDatabaseQueries.selectReadingHistory().executeAsList().size)
                val store = ReadingSessionStore(database)
                assertNull(store.read())
                store.write(reading)
                assertEquals(reading, store.read())
                database.bibleBibleDatabaseQueries.insertSeenBiblicalWord("short-01", 1_800_000_000_000L)
                assertEquals(listOf("short-01"), database.bibleBibleDatabaseQueries.selectSeenBiblicalWordIds().executeAsList())
                database.bibleBibleDatabaseQueries.upsertActiveReadingPlan("peace_in_every_season", 1_800_000_000_000L)
                database.bibleBibleDatabaseQueries.insertReadingPlanProgress(
                    reading_id = "peace_in_every_season:d1:r1",
                    plan_id = "peace_in_every_season",
                    day_number = 1L,
                    completed_at = 1_800_000_000_000L,
                    source = "auto_scroll",
                )
                assertEquals("peace_in_every_season", database.bibleBibleDatabaseQueries.selectActiveReadingPlan().executeAsOne().plan_id)
                assertEquals(1, database.bibleBibleDatabaseQueries.selectAllReadingPlanProgress().executeAsList().size)
                database.bibleBibleDatabaseQueries.upsertShortEngagement(
                    short_id = "short-01",
                    view_count = 3L,
                    same_session_replay_count = 1L,
                    manual_revisit_count = 1L,
                    pause_count = 2L,
                    total_watch_time_ms = 42_000L,
                    last_watched_at = 1_800_000_000_000L,
                    cycle_seen = 1L,
                )
                val engagement = database.bibleBibleDatabaseQueries.selectShortEngagementById("short-01").executeAsOne()
                assertEquals(3L, engagement.view_count)
                assertEquals(42_000L, engagement.total_watch_time_ms)
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
