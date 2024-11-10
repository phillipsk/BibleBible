package data.appPrefs

import data.apiBible.BibleAPIDataModel
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.DATABASE_RETENTION
import data.bibleIQ.DATABASE_RETENTION_READING_HISTORY
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.initBookLoad

internal suspend fun updateUserPrefsFontSize(fontSize: Float) {
    try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
                database.bibleBibleDatabaseQueries.updateUserPrefsFontSize(
                    fontSize = fontSize.toDouble()
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
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
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

internal suspend fun updateUserPrefsBibleBook(selectedBook: Long) {
    try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
                database.bibleBibleDatabaseQueries.updateUserPrefsSelectedBook(
                    selectedBook = selectedBook,
                )
                Napier.v(
                    "updateUserPrefsBibleVersion :: selectedBook $selectedBook",
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

internal suspend fun updateUserPrefsSetHomePage() {
    try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
                database.bibleBibleDatabaseQueries.updateUserPrefsSetHomePage()
                Napier.v("updateUserPrefsSetHomePage ::", tag = "AP8243")
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "AP8243")
    } finally {
        DriverFactory.closeDB()
    }
}

internal suspend fun updateUserPrefsBibleChapter(selectedChapter: Long) {
    try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
                database.bibleBibleDatabaseQueries.updateUserPrefsSelectedChapter(
                    selectedChapter = selectedChapter,
                )
                Napier.v(
                    "updateUserPrefsBibleVersion :: selectedChapter $selectedChapter",
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
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
                val data = database.bibleBibleDatabaseQueries.selectUserPrefs().executeAsList()
                Napier.v("getUserPreferences :: data :: $data", tag = "AP8243")
                if (data.isEmpty()) {
                    database.bibleBibleDatabaseQueries.insertUserPrefs()
                    Napier.v("init app load user prefs :: INSERT init load", tag = "AP8243")
                } else {
                    withContext(Dispatchers.Main) {
                        BibleIQDataModel.selectedFontSize = data[0].fontSize.toFloat()
                        BibleIQDataModel.updateSelectedVersion(data[0].bibleVersion)
                        val selectedBook = data[0].selectedBook?.toInt()?.minus(1)
                        val selectedChapter = data[0].selectedChapter?.toInt() ?: 1
                        if (selectedBook != null && BibleAPIDataModel.uiBooks.data != null) {
                            withContext(Dispatchers.Main) {
                                BibleAPIDataModel.uiBooks.data?.get(selectedBook)
                                    ?.run {
                                        initBookLoad(
                                            bookData = this,
                                            selectedChapter = selectedChapter,
                                            fromAppPrefs = true
                                        )
                                    }
                            }
                        }
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

internal suspend fun checkDatabaseRetention() {
    try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let {
                BibleBibleDatabase(driver = it)
            }?.bibleBibleDatabaseQueries?.cleanBibleVerses()
            DriverFactory.closeDB()
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "BB2452")
    } finally {
        DriverFactory.closeDB()
    }
}

internal suspend fun checkDatabaseSize() {
    try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
                val count = database.bibleBibleDatabaseQueries.countVerses().executeAsOne()
                val max = DATABASE_RETENTION
                if (count > max) {
                    Napier.d(
                        "clean database :: count $count :: max $max :: diff ${count - max}",
                        tag = "BB2452"
                    )
                    database.bibleBibleDatabaseQueries.removeExcessVerses(max)
                    DriverFactory.closeDB()
                }
            }
        }

    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "BB2452")
    } finally {
        DriverFactory.closeDB()
    }
}

internal suspend fun cleanReadingHistory() {
    try {
        withContext(Dispatchers.IO) {
            val count = countReadingHistory()
            val max = DATABASE_RETENTION_READING_HISTORY
            if (count != null && (count > max)) {
                DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }
                    ?.bibleBibleDatabaseQueries?.cleanReadingHistory((count - max))
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "BB2452")
    } finally {
        DriverFactory.closeDB()
    }
}

private suspend fun countReadingHistory(): Long? {
    return try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }
                ?.bibleBibleDatabaseQueries?.countReadingHistory()?.executeAsOne()
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "BB2452")
        null
    } finally {
        DriverFactory.closeDB()
    }
}
