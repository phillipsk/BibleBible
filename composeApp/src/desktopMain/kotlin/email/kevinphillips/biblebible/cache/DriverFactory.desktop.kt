package email.kevinphillips.biblebible.cache

import app.cash.sqldelight.db.SqlDriver

actual object DriverFactory {
    internal actual fun createDriver(): SqlDriver? {
        return null
    }

    internal actual fun closeDB() {
    }
}