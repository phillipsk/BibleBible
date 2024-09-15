package data.appPrefs

import data.bibleIQ.BibleIQDataModel
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

internal suspend fun updateUserPreferences(fontSize: Float, selectedVersion: String) {
    try {
        DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
            withContext(Dispatchers.IO) {
                database.bibleBibleDatabaseQueries.updateUserPrefs(
                    fontSize = fontSize.toDouble(),
                    bibleVersion = selectedVersion,
                )
                Napier.v("updateUserPrefs :: fontSize $fontSize", tag = "AP8243")
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "AP8243")
    } finally {
        DriverFactory.closeDB()
    }
}

internal suspend fun updateUserPrefsBibleVersion(selectedVersion: String) {
    try {
        DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
            withContext(Dispatchers.IO) {
                database.bibleBibleDatabaseQueries.updateUserPrefsBibleVersion(
                    bibleVersion = selectedVersion,
                )
                Napier.v(
                    "updateUserPrefsBibleVersion :: selectedVersion $selectedVersion",
                    tag = "AP8243"
                )
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "AP8243")
    } finally {
        DriverFactory.closeDB()
    }
}

internal suspend fun getUserPreferences() {
    try {
        DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
            withContext(Dispatchers.IO) {
                val data = database.bibleBibleDatabaseQueries.selectUserPrefs().executeAsList()
                Napier.v("getUserPreferences :: data :: $data", tag = "AP8243")
                if (data.isEmpty()) {
                    database.bibleBibleDatabaseQueries.insertUserPrefs()
                    Napier.v("init app load user prefs :: INSERT init load", tag = "AP8243")
                } else {
                    withContext(Dispatchers.Main) {
                        BibleIQDataModel.selectedFontSize = data[0].fontSize.toFloat()
                        BibleIQDataModel.updateSelectedVersion(data[0].bibleVersion)
                        Napier.v(
                            "getUserPreferences :: fontSize ${BibleIQDataModel.selectedFontSize} " +
                                    " bibleVersion :: ${BibleIQDataModel.selectedVersion}",
                            tag = "AP8243"
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "AP8243")
    } finally {
        DriverFactory.closeDB()
    }
}