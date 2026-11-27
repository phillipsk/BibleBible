package email.kevinphillips.biblebible.cache

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import java.nio.file.Files
import java.nio.file.Path

internal fun isMacOsDesktop(): Boolean =
    System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)

internal fun desktopAppDataDir(): Path {
    val explicitDbPath = System.getProperty("biblebible.database.path")?.takeIf { it.isNotBlank() }
    if (explicitDbPath != null) {
        return Path.of(explicitDbPath).toAbsolutePath().parent ?: Path.of(".")
    }
    val userHome = System.getProperty("user.home")
    return if (isMacOsDesktop()) {
        Path.of(userHome, "Library", "Application Support", "BibleBible")
    } else {
        Path.of(userHome, ".biblebible")
    }
}

internal fun ensureDesktopSqliteTmpDir(baseDir: Path = desktopAppDataDir()): Path {
    val existing = System.getProperty("org.sqlite.tmpdir")?.takeIf { it.isNotBlank() }
    if (existing != null) {
        val existingPath = Path.of(existing)
        runCatching { Files.createDirectories(existingPath) }
        return existingPath
    }
    val tmpDir = baseDir.toAbsolutePath().resolve("sqlite-tmp")
    runCatching { Files.createDirectories(tmpDir) }
    System.setProperty("org.sqlite.tmpdir", tmpDir.toString())
    return tmpDir
}

internal actual object DriverFactory {
    private val driver: SqlDriver by lazy {
        val explicitPath = System.getProperty("biblebible.database.path")
            ?.takeIf { it.isNotBlank() }
            ?.let(Path::of)
        val dataDir = desktopAppDataDir()
        val path = explicitPath ?: dataDir.resolve("BibleBibleDB.db")
        if (explicitPath == null && isMacOsDesktop() && !Files.exists(path)) {
            val legacyPath = Path.of(System.getProperty("user.home"), ".biblebible", "BibleBibleDB.db")
            if (Files.isRegularFile(legacyPath) && Files.isReadable(legacyPath)) {
                runCatching {
                    path.toAbsolutePath().parent?.let(Files::createDirectories)
                    Files.copy(legacyPath, path)
                }
            }
        }
        ensureDesktopSqliteTmpDir(path.toAbsolutePath().parent ?: dataDir)
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
