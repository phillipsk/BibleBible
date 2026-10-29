package email.kevinphillips.biblebible.cache

import app.cash.sqldelight.db.SqlDriver

expect object DriverFactory {
    internal fun createDriver(): SqlDriver?
    // Retained for existing repository call sites. The shared driver is app-scoped;
    // a completed query must not close a connection another coroutine is using.
    internal fun closeDB()
}
