package email.kevinphillips.biblebible.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(BibleBibleDatabase.Schema, "BibleBibleDB.db")
    }
}