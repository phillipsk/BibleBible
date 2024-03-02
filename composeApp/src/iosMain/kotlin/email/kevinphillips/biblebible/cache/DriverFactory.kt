package email.kevinphillips.biblebible.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import email.kevinphillips.biblebible.db.BibleBibleDatabase

actual object DriverFactory {
    private var driverRef: SqlDriver? = null

    actual fun createDriver(): SqlDriver? {
        if (driverRef == null) {
            driverRef = NativeSqliteDriver(BibleBibleDatabase.Schema, "BibleBibleDB.db")
        }
        return driverRef!!
    }

    internal actual fun closeDB() {
        driverRef = null
    }
}