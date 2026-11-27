package email.kevinphillips.biblebible.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import email.kevinphillips.biblebible.db.BibleBibleDatabase

internal actual object DriverFactory {
    private val driver: SqlDriver by lazy {
        NativeSqliteDriver(BibleBibleDatabase.Schema, "BibleBibleDB.db")
    }

    internal actual fun createDriver(): SqlDriver? {
        return driver
    }

    internal actual fun closeDB() {
        // The pooled native driver is shared for the lifetime of the application.
    }
}
