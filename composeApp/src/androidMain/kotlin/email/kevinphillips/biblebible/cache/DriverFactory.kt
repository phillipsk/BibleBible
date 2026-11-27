package email.kevinphillips.biblebible.cache

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import email.kevinphillips.biblebible.db.BibleBibleDatabase

internal actual object DriverFactory {
    internal lateinit var context: Context
    private val driver: SqlDriver by lazy {
        AndroidSqliteDriver(BibleBibleDatabase.Schema, context.applicationContext, "BibleBibleDB.db")
    }

    internal actual fun createDriver(): SqlDriver? {
        return driver
    }

    internal actual fun closeDB() {
        // One application-lifetime driver: other repositories may still be using it.
    }
}
