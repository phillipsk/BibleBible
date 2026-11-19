package data.appPrefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReadingSessionDatabaseAndroidTest {
    private val reading = SavedReadingSession(
        "PSA", 119, "KJV", 5067, 0.5f, lastActiveMillis = 1_800_000_000_000L
    )

    @Test
    fun savedBookChapterAndAnchorSurviveAndroidDatabaseReopen() = withTestDatabase { context, name ->
        AndroidSqliteDriver(BibleBibleDatabase.Schema, context, name).use { driver ->
            val database = BibleBibleDatabase(driver)
            database.bibleBibleDatabaseQueries.insertUserPrefs()
            database.bibleBibleDatabaseQueries.updateUserPrefs(26.0, "ASV")
            ReadingSessionStore(database).write(reading)
        }
        AndroidSqliteDriver(BibleBibleDatabase.Schema, context, name).use { driver ->
            val database = BibleBibleDatabase(driver)
            assertEquals(reading, ReadingSessionStore(database).read())
            val preferences = database.bibleBibleDatabaseQueries.selectUserPrefs().executeAsOne()
            assertEquals(26.0, preferences.fontSize, 0.0)
            assertEquals("ASV", preferences.bibleVersion)
        }
    }

    @Test
    fun androidOpenHelperMigratesReleasedSchemaWithoutLosingExistingData() = withTestDatabase { context, name ->
        AndroidSqliteDriver(BibleBibleDatabase.Schema, context, name).use { driver ->
            val database = BibleBibleDatabase(driver)
            database.bibleBibleDatabaseQueries.insertUserPrefs()
            database.bibleBibleDatabaseQueries.updateUserPrefs(30.0, "SVD")
            database.bibleBibleDatabaseQueries.insertReadingHistory("2026-10-28 12:00:00", "19", "119")
            // Released schema6 is identical except for the new session table.
            driver.execute(null, "DROP TABLE SavedReadingPosition", 0)
            driver.execute(null, "PRAGMA user_version = 6", 0)
        }
        // SQLiteOpenHelper invokes the real generated6→7 migration on reopen.
        AndroidSqliteDriver(BibleBibleDatabase.Schema, context, name).use { driver ->
            val database = BibleBibleDatabase(driver)
            val preferences = database.bibleBibleDatabaseQueries.selectUserPrefs().executeAsOne()
            assertEquals(30.0, preferences.fontSize, 0.0)
            assertEquals("SVD", preferences.bibleVersion)
            assertEquals(1, database.bibleBibleDatabaseQueries.selectReadingHistory().executeAsList().size)
            val store = ReadingSessionStore(database)
            assertNull(store.read())
            store.write(reading)
            assertEquals(reading, store.read())
        }
    }

    private fun withTestDatabase(test: (Context, String) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "reading-session-test-${UUID.randomUUID()}.db"
        try {
            test(context, name)
        } finally {
            context.deleteDatabase(name)
        }
    }
}
