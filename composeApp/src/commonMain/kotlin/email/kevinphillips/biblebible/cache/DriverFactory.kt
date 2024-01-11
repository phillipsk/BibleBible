package email.kevinphillips.biblebible.cache

import app.cash.sqldelight.db.SqlDriver
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import email.kevinphillips.biblebible.db.BibleBibleDatabaseQueries

expect class DriverFactory {
    fun createDriver(): SqlDriver
}