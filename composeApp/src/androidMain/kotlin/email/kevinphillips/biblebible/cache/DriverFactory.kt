package email.kevinphillips.biblebible.cache

import android.annotation.SuppressLint
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import email.kevinphillips.biblebible.db.BibleBibleDatabase

@SuppressLint("StaticFieldLeak")
actual object DriverFactory {
    lateinit var context: Context
    private var driver: SqlDriver? = null

    actual fun createDriver(): SqlDriver? {
        driver = AndroidSqliteDriver(BibleBibleDatabase.Schema, context, "BibleBibleDB.db")
        return driver
    }

    internal actual fun closeDB() {
//        driver?.close()
//        driver = null
    }
}