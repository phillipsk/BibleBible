package email.kevinphillips.biblebible.cache

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import email.kevinphillips.biblebible.db.BibleBibleDatabase

actual object DriverFactory {

    lateinit var context: Context

    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(BibleBibleDatabase.Schema, context, "BibleBibleDB.db")
    }
}