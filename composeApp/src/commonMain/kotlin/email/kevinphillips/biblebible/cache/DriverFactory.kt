package email.kevinphillips.biblebible.cache

import app.cash.sqldelight.db.SqlDriver

expect object DriverFactory {
    fun createDriver(): SqlDriver
}