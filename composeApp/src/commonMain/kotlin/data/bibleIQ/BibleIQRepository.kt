package data.bibleIQ

import JSON_BOOKS
import JSON_VERSIONS
import data.apiBible.BookData
import data.httpClientBibleIQ
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

const val LOCAL_DATA = true
val DATABASE_RETENTION = if (BibleIQDataModel.RELEASE_BUILD) 30_000L else 100L

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
    try {
        val versions = if (LOCAL_DATA) {
            Json.decodeFromString<List<BibleIQVersion>>(JSON_VERSIONS)
        } else {
            httpClientBibleIQ.get(GetVersions()).body<List<BibleIQVersion>>()
        }
        BibleIQDataModel.updateBibleVersions(BibleIQVersions(versions))
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ092")
    }
}

internal suspend fun getChapterBibleIQ(
    book: BookData,
    chapter: Int = 1,
    version: String = BibleIQDataModel.selectedVersion
) {
    try {
        val bookId = BibleIQDataModel.getAPIBibleCardinal(book.remoteKey)
        Napier.v("getChapterBibleIQ: bookId: $bookId :: chapter $chapter", tag = "IQ093")
        val chapterVerses: List<BibleChapter>
        var chapterCount: ChapterCount?

        Napier.i("getChapterBibleAPI: $chapter :: version $version", tag = "IQ093")
        Napier.d("start load", tag = "IQ093")
        val cachedData = loadVerseData(bookId, chapter, version)
        Napier.d("end load", tag = "IQ093")
//        val bookName = BibleIQDataModel.getAPIBibleCardinal(book.remoteKey).toString()
        Napier.v(
            "httpclient cachedData value: book:: ${
                cachedData?.firstOrNull()?.b + " :: verse: " + cachedData?.firstOrNull()?.c
            }", tag = "IQ093"
        )

        if (cachedData.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
//                delay(3000)
                chapterVerses =
                    httpClientBibleIQ.get(
                        GetChapter(
                            bookId = bookId,
                            chapterId = chapter.toString(),
                            versionId = version.lowercase()
                        )
                    ).body<List<BibleChapter>>()
                Napier.v(
                    "getChapterBibleIQ: ${chapterVerses.firstOrNull()?.t?.take(100)}",
                    tag = "IQ093"
                )
                if (chapterVerses.isEmpty()) {
                    throw IOException("Error fetching chapter")
                }
                chapterCount = queryBookChapterSize(bookId, version)
                if (chapterCount?.chapterCount == 0L || chapterCount?.chapterCount == null) {
                    Napier.v("sql :: insert chapterCount", tag = "IQ093")
                    chapterCount = getChapterCountBibleIQ(bookId).await()
                    insertChapterCount(chapterCount, bookId, version)
                }

                withContext(Dispatchers.Main) {
                    Napier.v("getChapterBibleIQ :: update UI", tag = "IQ093")
                    BibleIQDataModel.updateBibleChapter(chapterVerses, chapterCount, version)
                }
                insertBibleVerses(chapterVerses, version, chapterCount)
            }
        } else {
            chapterCount = queryBookChapterSize(bookId, version)
            withContext(Dispatchers.Main) {
                Napier.v("getChapterBibleIQ :: update UI", tag = "IQ093")
                BibleIQDataModel.updateBibleChapter(cachedData, chapterCount, version)
            }
        }
    } catch (e: IOException) {
        BibleIQDataModel.updateErrorSnackBar(e.message ?: "Error fetching chapter")
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ093")
    }
}

suspend fun insertChapterCount(chapterCount: ChapterCount?, bookId: Int, version: String) {
    try {
        chapterCount?.let {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
                withContext(Dispatchers.IO) {
                    database.bibleBibleDatabaseQueries.insertChapterCount(
                        uuid = bookId.toString() + "-" + version.lowercase(),
                        b = bookId.toString(),
                        version = version.lowercase(),
                        chapterCount = chapterCount.chapterCount,
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

private suspend fun getChapterCountBibleIQ(bookId: Int) = coroutineScope {
    async(Dispatchers.IO) {
        httpClientBibleIQ.get(GetChapterCount(bookId)).body<ChapterCount>()
    }
}

private suspend fun insertBibleVerses(
    chapterContent: List<BibleChapter>,
    version: String,
    chapterCount: ChapterCount?
) {
    DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
        try {
            withContext(Dispatchers.IO) {
                Napier.d("inside insert load before delay", tag = "IQ093")
                Napier.d("inside insert load end delay", tag = "IQ093")
                Napier.v("insertVerse bookId  :: ${chapterContent.firstOrNull()?.b}", tag = "IQ093")
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
                                chapterCount = chapterCount?.chapterCount ?: 0
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
}

private suspend fun loadVerseData(
    bookId: Int,
    chapter: Int,
    version: String
): List<BibleChapter>? {
    DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
        return try {
            withContext(Dispatchers.IO) {
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

                Napier.v("bibleQueries selectedChapter $bookId :: hello world", tag = "IQ093")
                Napier.v(
                    "bibleQueries ${bibleQueries.firstOrNull()?.v?.take(100)}",
                    tag = "IQ093"
                )

                val list = bibleQueries.map {
                    BibleChapter(id = it.id, b = it.b, c = it.c, v = it.v, t = it.t)
                }
                list
            }
        } catch (e: Exception) {
            Napier.e("Error: ${e.message}", tag = "IQ093")
            null
        } finally {
            DriverFactory.closeDB()
        }
    }
    return null
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
