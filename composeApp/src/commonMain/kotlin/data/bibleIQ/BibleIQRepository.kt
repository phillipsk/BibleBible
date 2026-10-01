package data.bibleIQ

import JSON_BOOKS
import JSON_VERSIONS
import data.GeminiModel
import data.RequestCache
import data.apiBible.BibleAPIDataModel.readingHistory
import data.apiBible.BookData
import data.apiBible.getReadingHistory
import data.apiBible.getTimeZone
import data.httpClientBibleIQ
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.get
import io.ktor.http.isSuccess
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

const val LOCAL_DATA = true
val DATABASE_RETENTION = if (BibleIQDataModel.RELEASE_BUILD) 30_000L else 30_000L
val DATABASE_RETENTION_READING_HISTORY = if (BibleIQDataModel.RELEASE_BUILD) 500L else 500L

private data class ChapterRequest(val bookId: Int, val chapter: Int, val version: String)
private data class ChapterData(val verses: List<BibleChapter>, val count: ChapterCount?)
private val chapterRequests = RequestCache<ChapterRequest, ChapterData>(maxEntries = 8)
private val chapterCounts = RequestCache<Int, ChapterCount>(maxEntries = 66)
private var currentChapterRequest: ChapterRequest? = null

internal suspend fun getBooksBibleIQ() {
    try {
        val books = if (LOCAL_DATA) {
            Json.decodeFromString<List<BibleIQBook>>(JSON_BOOKS)
        } else {
            httpClientBibleIQ.get(GetBooks()).body<List<BibleIQBook>>()
        }
        BibleIQDataModel.updateBibleBooks(BibleIQBooks(books))
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ092")
    }
}

internal suspend fun getVersionsBibleIQ() {
    if (BibleIQDataModel.bibleVersions.data.isNotEmpty()) {
        Napier.v("getVersionsBibleIQ() :: already loaded", tag = "AL792")
        return
    }
    try {
        val versions = if (LOCAL_DATA) {
            withContext(Dispatchers.IO) {
                Json.decodeFromString<List<BibleIQVersion>>(JSON_VERSIONS)
            }
        } else {
            httpClientBibleIQ.get(GetVersions()).body<List<BibleIQVersion>>()
        }
        withContext(Dispatchers.Main) {
            BibleIQDataModel.updateBibleVersions(BibleIQVersions(versions))
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ092")
    }
}

internal suspend fun getChapterBibleIQ(
    book: BookData,
    chapter: Int = 1,
    version: String = BibleIQDataModel.selectedVersion,
    updateReadingHistory: Boolean = true,
    client: HttpClient = httpClientBibleIQ,
) {
    try {
        val bookId = BibleIQDataModel.getAPIBibleOrdinal(book.remoteKey)
        val key = ChapterRequest(bookId, chapter, version.lowercase())
        if (currentChapterRequest != key) {
            currentChapterRequest = key
            GeminiModel.resetForChapter()
        }
        val data = chapterRequests.get(key) {
            withContext(Dispatchers.IO) {
                val cached = loadVerseData(bookId, chapter, key.version)
                var count = queryBookChapterSize(bookId, key.version)
                if (count?.chapterCount == null || count.chapterCount == 0L) {
                    count = getChapterCountBibleIQ(bookId, client)
                    insertChapterCount(count, bookId, key.version)
                }
                if (!cached.isNullOrEmpty()) {
                    updateTimestampBibleVerses(cached.firstOrNull(), key.version)
                    ChapterData(cached, count)
                } else {
                    val response = client.get(GetChapter(bookId, chapter.toString(), key.version))
                    if (!response.status.isSuccess()) {
                        throw IOException("Bible chapter unavailable (HTTP ${response.status.value}).")
                    }
                    val verses = response.body<List<BibleChapter>>()
                    if (verses.isEmpty()) throw IOException("Error fetching chapter")
                    insertBibleVerses(verses, key.version, count)
                    ChapterData(verses, count)
                }
            }
        }
        withContext(Dispatchers.Main) {
            // A response for a chapter we have left must not replace the current chapter.
            if (currentChapterRequest == key) {
                BibleIQDataModel.updateBibleChapter(data.verses, data.count, key.version)
            }
        }
        if (currentChapterRequest != key) return
        if (updateReadingHistory) {
            insertReadingHistory(bookId, chapter)
            getReadingHistory()
        }
        Napier.v("BibleIQRepository :: count :: ${readingHistory?.size}", tag = "RH1283")
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        BibleIQDataModel.updateErrorSnackBar(e.message ?: "Error fetching chapter")
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ093")
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
                            uuid = bookId.toString() + "-" + version.lowercase(),
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

private suspend fun getChapterCountBibleIQ(bookId: Int, client: HttpClient): ChapterCount {
    return chapterCounts.get(bookId) {
        val response = client.get(GetChapterCount(bookId))
        if (!response.status.isSuccess()) {
            throw IOException("Chapter count unavailable (HTTP ${response.status.value}).")
        }
        response.body<ChapterCount>().also {
            if (it.chapterCount == null || it.chapterCount <= 0) throw IOException("Error fetching chapter count")
        }
    }
}

private suspend fun insertBibleVerses(
    chapterContent: List<BibleChapter>,
    version: String,
    chapterCount: ChapterCount?
) {
    try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }
                ?.let { database ->
                    Napier.d("inside insert load before delay", tag = "IQ093")
                    Napier.d("inside insert load end delay", tag = "IQ093")
                    Napier.v(
                        "insertVerse bookId  :: ${chapterContent.firstOrNull()?.b}",
                        tag = "IQ093"
                    )
                    chapterContent.let {
//                        delay(3000)
                        database.transaction {
                            it.forEach {
                                database.bibleBibleDatabaseQueries.insertVerse(
                                    uuid = it.id + "-" + version.lowercase(),
                                    id = it.id ?: "",
                                    b = it.b ?: "",
                                    c = it.c ?: "",
                                    v = it.v ?: "",
                                    t = it.t ?: "",
                                    version = version.lowercase(),
                                    chapterCount = chapterCount?.chapterCount ?: 0,
                                    created_at = getTimeZone(),
                                    updated_at = getTimeZone()
                                )
                            }
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
    version: String?
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

private suspend fun loadVerseData(
    bookId: Int,
    chapter: Int,
    version: String
): List<BibleChapter>? {
    return try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }
                ?.let { database ->
                    Napier.d("inside start load before delay", tag = "IQ093")
//                 delay(3000)
                    Napier.d("inside start load end delay", tag = "IQ093")

                    val bibleQueries = database.bibleBibleDatabaseQueries
                        .selectVersesByBookId(
                            bookId.toString(),
                            chapter.toString(),
                            version.lowercase()
                        )
                        .executeAsList()

                    Napier.v(
                        "bibleQueries selectedChapter $bookId :: hello world",
                        tag = "IQ093"
                    )
                    Napier.v(
                        "bibleQueries ${bibleQueries.firstOrNull()?.v?.take(100)}",
                        tag = "IQ093"
                    )

                    val list = bibleQueries.map {
                        BibleChapter(id = it.id, b = it.b, c = it.c, v = it.v, t = it.t)
                    }
                    list
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
                    version.lowercase()
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
