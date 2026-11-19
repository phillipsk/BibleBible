package email.kevinphillips.biblebible.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.db.QueryResult
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import java.nio.file.Files
import java.nio.file.Path

actual object DriverFactory {
    private val driver: SqlDriver by lazy {
        val path = System.getProperty("biblebible.database.path")?.let(Path::of)
            ?: Path.of(System.getProperty("user.home"), ".biblebible", "BibleBibleDB.db")
        openDesktopDatabase(path)
    }

    internal actual fun createDriver(): SqlDriver? {
        return driver
    }

    internal actual fun closeDB() {
        // Shared with background session saves; per-query closes race active readers.
    }
}

internal fun openDesktopDatabase(path: Path): SqlDriver {
    path.toAbsolutePath().parent?.let(Files::createDirectories)
    val driver = JdbcSqliteDriver("jdbc:sqlite:${path.toAbsolutePath()}")
    try {
        BibleBibleDatabase(driver).transaction {
            val oldVersion = driver.executeQuery(null, "PRAGMA user_version", { cursor ->
                QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0 else 0)
            }, 0).value
            val newVersion = BibleBibleDatabase.Schema.version
            when {
                oldVersion == 0L -> BibleBibleDatabase.Schema.create(driver)
                oldVersion < newVersion -> BibleBibleDatabase.Schema.migrate(driver, oldVersion, newVersion)
                else -> check(oldVersion == newVersion) { "Database was created by a newer version of BibleBible" }
            }
            driver.execute(null, "PRAGMA user_version = $newVersion", 0)
        }
        return driver
    } catch (error: Exception) {
        driver.close()
        throw error
    }
}
