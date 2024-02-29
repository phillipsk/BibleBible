package email.kevinphillips.biblebible.cache

import app.cash.sqldelight.db.SqlDriver

expect object DriverFactory {
    internal fun createDriver(): SqlDriver?
    internal fun closeDB()
}