package email.kevinphillips.biblebible.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import java.util.Properties

actual object DriverFactory {
    private var driverRef: SqlDriver? = null

    internal actual fun createDriver(): SqlDriver? {
        if (driverRef == null) {
            driverRef = JdbcSqliteDriver(
                "jdbc:sqlite:BibleBibleDB.db", Properties(),
                BibleBibleDatabase.Schema
            )
        }
        return driverRef!!
    }

    internal actual fun closeDB() {
        driverRef = null
    }
}