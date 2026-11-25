package data.bible

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import data.GeminiModel
import data.analytics.AnalyticsTracker
import data.bible.json.BUNDLED_BIBLE_BOOKS_JSON
import data.bible.json.BUNDLED_BIBLE_VERSIONS_JSON
import data.httpClientCloudGateway
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import email.kevinphillips.biblebible.db.SelectReadingHistory
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val DATABASE_RETENTION = 65_000L
const val DATABASE_RETENTION_READING_HISTORY = 500L

@Serializable
private data class VerseFormatMeta(
    @kotlinx.serialization.SerialName("v") val v: String,
    @kotlinx.serialization.SerialName("p") val p: Boolean = false,
    @kotlinx.serialization.SerialName("r") val r: List<List<Int>> = emptyList(),
)

private val chapterFormatJson = Json { ignoreUnknownKeys = true }

internal suspend fun loadBibleBooks() {
    if (BibleState.bibleBooks.data?.isNotEmpty() == true) {
        Napier.v("loadBibleBooks() :: already loaded", tag = "AL792")
        return
    }
    try {
        val loadedBooks = withContext(Dispatchers.IO) {
            Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        }
        withContext(Dispatchers.Main) {
            BibleState.updateBooks(loadedBooks)
        }
    } catch (e: Exception) {
        Napier.e("loadBibleBooks Error: ${e.message}", tag = "IQ092")
    }
}

internal suspend fun loadBibleVersions() {
    if (BibleState.bibleVersions.data.isNotEmpty()) {
        Napier.v("loadBibleVersions() :: already loaded", tag = "AL792")
        return
    }
    try {
        val versions = withContext(Dispatchers.IO) {
            Json.decodeFromString<List<BibleVersion>>(BUNDLED_BIBLE_VERSIONS_JSON)
        }
        withContext(Dispatchers.Main) {
            BibleState.updateBibleVersions(BibleVersions(versions))
        }
    } catch (e: Exception) {
        Napier.e("loadBibleVersions Error: ${e.message}", tag = "IQ092")
    }
}

internal suspend fun fetchBibleChapter(
    book: BookData,
    chapter: Int = 1,
    version: String = BibleState.selectedVersion,
    updateReadingHistory: Boolean = true,
    client: HttpClient = httpClientCloudGateway,
) {
    try {
        val bookId = BibleState.getBookOrdinal(book.remoteKey)
        val isSameBook = BibleState.bibleChapter?.bookId == bookId
        GeminiModel.cancelPendingSummary(preserveShowSummary = isSameBook)
        Napier.v("fetchBibleChapter: bookId: $bookId :: chapter $chapter", tag = "IQ093")
        val chapterVerses: List<BibleChapter>
        var chapterCount: ChapterCount?

        val cachedEntry = loadVerseData(bookId, chapter, version)
        val cachedData = cachedEntry?.first
        val hasFormatMeta = cachedEntry?.second == true

        if (cachedData.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                chapterVerses =
                    client.getBibleChapter(
                        bookId = bookId,
                        chapterId = chapter.toString(),
                        versionId = version.lowercase(),
                    ).body<List<BibleChapter>>()
                if (chapterVerses.isEmpty()) {
                    throw IOException("Error fetching chapter")
                }
                chapterCount = queryBookChapterSize(bookId, version)
                if (chapterCount?.chapterCount == 0L || chapterCount?.chapterCount == null) {
                    chapterCount = fetchChapterCount(bookId, client).await()
                    insertChapterCount(chapterCount, bookId, version)
                }

                withContext(Dispatchers.Main) {
                    BibleState.updateBibleChapter(chapterVerses, chapterCount, version)
                }
                insertBibleVerses(chapterVerses, version, chapterCount)
                saveChapterFormatMeta(bookId, chapter, version, chapterVerses)
                AnalyticsTracker.trackChapterLoaded(bookId, chapter, version, source = "remote")
            }
        } else {
            chapterCount = queryBookChapterSize(bookId, version)
            withContext(Dispatchers.Main) {
                BibleState.updateBibleChapter(cachedData, chapterCount, version)
            }
            updateTimestampBibleVerses(cachedData.firstOrNull(), version)
            AnalyticsTracker.trackChapterLoaded(bookId, chapter, version, source = "cache")
            if (!hasFormatMeta) {
                // Legacy cached chapter lacks paragraph/red-letter metadata; quietly refresh when online
                withContext(Dispatchers.IO) {
                    runCatching {
                        val refreshed = client.getBibleChapter(
                            bookId = bookId,
                            chapterId = chapter.toString(),
                            versionId = version.lowercase(),
                        ).body<List<BibleChapter>>()
                        if (refreshed.isNotEmpty()) {
                            saveChapterFormatMeta(bookId, chapter, version, refreshed)
                            withContext(Dispatchers.Main) {
                                BibleState.updateBibleChapter(refreshed, chapterCount, version)
                            }
                        }
                    }
                }
            }
        }
        if (updateReadingHistory) {
            insertReadingHistory(bookId, chapter)
            fetchReadingHistory()
        }
        Napier.v("BibleRepository :: history count :: ${BibleState.readingHistory?.size}", tag = "RH1283")
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AnalyticsTracker.trackError("chapter_fetch", e.message ?: "IOException")
        BibleState.updateErrorSnackBar(e.message ?: "Error fetching chapter")
    } catch (e: Exception) {
        AnalyticsTracker.trackError("chapter_fetch", e.message ?: "Exception")
        Napier.e("Error: ${e.message}", tag = "IQ093")
    }
}

internal suspend fun fetchReadingHistory() {
    var filteredReadingHistory = emptyList<SelectReadingHistory>()
    try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }
                ?.let { database ->
                    val historyRows = database.bibleBibleDatabaseQueries
                        .selectReadingHistory()
                        .executeAsList()
                    // Filter false-start chapter 1 entries unless immediately preceded by chapter 2
                    // or belonging to a single-chapter book.
                    filteredReadingHistory = historyRows.filterIndexed { index, currentRecord ->
                        if (currentRecord.c == "1" && index > 0 && currentRecord.b !in BibleState.singleChapterBooksOrdinal) {
                            val previousRecord = historyRows[index - 1]
                            currentRecord.b == previousRecord.b && previousRecord.c == "2"
                        } else {
                            true
                        }
                    }
                }
            withContext(Dispatchers.Main) {
                BibleState.updateReadingHistory(filteredReadingHistory)
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ094")
    } finally {
        DriverFactory.closeDB()
    }
}

internal suspend fun insertReadingHistory(bookId: Int, chapter: Int) {
    try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
                database.transaction {
                    database.bibleBibleDatabaseQueries.insertReadingHistory(
                        created_at = getTimeZone(),
                        b = bookId.toString(),
                        c = chapter.toString(),
                    )
                }
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ093")
    } finally {
        DriverFactory.closeDB()
    }
}

internal suspend fun insertChapterCount(chapterCount: ChapterCount?, bookId: Int, version: String) {
    try {
        withContext(Dispatchers.IO) {
            chapterCount?.let {
                DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }
                    ?.let { database ->
                        database.bibleBibleDatabaseQueries.insertChapterCount(
                            uuid = "$bookId-${version.lowercase()}",
                            b = bookId.toString(),
                            version = version.lowercase(),
                            chapterCount = chapterCount.chapterCount,
                            created_at = getTimeZone(),
                        )
                    }
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ093")
    } finally {
        DriverFactory.closeDB()
    }
}

private suspend fun fetchChapterCount(bookId: Int, client: HttpClient) = coroutineScope {
    async(Dispatchers.IO) {
        client.getChapterCount(bookId).body<ChapterCount>()
    }
}

private suspend fun insertBibleVerses(
    chapterContent: List<BibleChapter>,
    version: String,
    chapterCount: ChapterCount?,
) {
    try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }
                ?.let { database ->
                    database.transaction {
                        chapterContent.forEach { verse ->
                            database.bibleBibleDatabaseQueries.insertVerse(
                                uuid = "${verse.id}-${version.lowercase()}",
                                id = verse.id ?: "",
                                b = verse.b ?: "",
                                c = verse.c ?: "",
                                v = verse.v ?: "",
                                t = verse.t ?: "",
                                version = version.lowercase(),
                                chapterCount = chapterCount?.chapterCount ?: 0,
                                created_at = getTimeZone(),
                                updated_at = getTimeZone(),
                            )
                        }
                    }
                }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ093")
    } finally {
        DriverFactory.closeDB()
    }
}

private suspend fun updateTimestampBibleVerses(
    bibleChapter: BibleChapter?,
    version: String?,
) {
    try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }
                ?.let { database ->
                    database.transaction {
                        database.bibleBibleDatabaseQueries.updateVerseTimestamp(
                            updated_at = getTimeZone(),
                            b = bibleChapter?.b ?: "",
                            c = bibleChapter?.c ?: "",
                            version = version?.lowercase() ?: "",
                        )
                    }
                }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ093")
    } finally {
        DriverFactory.closeDB()
    }
}

private fun chapterFormatKey(bookId: Int, chapter: Int, version: String): String =
    "v2:$bookId:$chapter:${version.lowercase()}"

private fun ensureChapterFormatMetaTable(driver: SqlDriver) {
    driver.execute(
        null,
        "CREATE TABLE IF NOT EXISTS ChapterFormatMeta (chapterKey TEXT PRIMARY KEY, metaJson TEXT NOT NULL)",
        0,
    )
}

private suspend fun saveChapterFormatMeta(
    bookId: Int,
    chapter: Int,
    version: String,
    verses: List<BibleChapter>,
) {
    runCatching {
        withContext(Dispatchers.IO) {
            val driver = DriverFactory.createDriver() ?: return@withContext
            ensureChapterFormatMetaTable(driver)
            val entries = verses.mapNotNull { verse ->
                val verseNum = verse.v ?: return@mapNotNull null
                if (!verse.p && verse.r.isEmpty()) return@mapNotNull null
                VerseFormatMeta(v = verseNum, p = verse.p, r = verse.r)
            }
            val jsonPayload = chapterFormatJson.encodeToString(entries)
            val key = chapterFormatKey(bookId, chapter, version)
            driver.execute(
                null,
                "INSERT OR REPLACE INTO ChapterFormatMeta (chapterKey, metaJson) VALUES (?, ?)",
                2,
            ) {
                bindString(0, key)
                bindString(1, jsonPayload)
            }
        }
    }
}

private suspend fun loadVerseData(
    bookId: Int,
    chapter: Int,
    version: String,
): Pair<List<BibleChapter>, Boolean>? {
    return try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { driver ->
                val database = BibleBibleDatabase(driver = driver)
                val bibleQueries = database.bibleBibleDatabaseQueries
                    .selectVersesByBookId(
                        bookId.toString(),
                        chapter.toString(),
                        version.lowercase(),
                    )
                    .executeAsList()

                if (bibleQueries.isEmpty()) {
                    return@let null
                }

                val metaJson = runCatching {
                    ensureChapterFormatMetaTable(driver)
                    val key = chapterFormatKey(bookId, chapter, version)
                    driver.executeQuery(
                        null,
                        "SELECT metaJson FROM ChapterFormatMeta WHERE chapterKey = ?",
                        { cursor ->
                            QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null)
                        },
                        1,
                    ) {
                        bindString(0, key)
                    }.value
                }.getOrNull()

                val metaByVerse = if (metaJson != null) {
                    runCatching {
                        chapterFormatJson.decodeFromString<List<VerseFormatMeta>>(metaJson)
                            .associateBy { it.v }
                    }.getOrDefault(emptyMap())
                } else {
                    emptyMap()
                }

                val chapters = bibleQueries.map { row ->
                    val meta = metaByVerse[row.v]
                    BibleChapter(
                        id = row.id,
                        b = row.b,
                        c = row.c,
                        v = row.v,
                        t = row.t,
                        p = meta?.p ?: false,
                        r = meta?.r ?: emptyList(),
                    )
                }
                chapters to (metaJson != null)
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ093")
        null
    } finally {
        DriverFactory.closeDB()
    }
}

internal suspend fun queryBookChapterSize(bookId: Int, version: String): ChapterCount? {
    return try {
        withContext(Dispatchers.IO) {
            val count = DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }
                ?.bibleBibleDatabaseQueries?.countVersesByBookId(
                    bookId.toString(),
                    version.lowercase(),
                )
                ?.executeAsOneOrNull()?.chapterCount
            ChapterCount(count)
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ093")
        null
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
                        tag = "BB2452",
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
            if (count != null && count > max) {
                DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }
                    ?.bibleBibleDatabaseQueries?.cleanReadingHistory(count - max)
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
