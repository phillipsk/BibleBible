package data.apiBible

import data.apiBible.json.JSON_BIBLES_API_BIBLE_SELECT
import data.apiBible.json.JSON_BOOKS_API_BIBLE
import data.httpClient
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.resources.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

const val LOCAL_DATA = true
val DATABASE_RETENTION = if (BibleAPIDataModel.RELEASE_BUILD) 30_000L else 10L

internal suspend fun getBiblesBibleAPI() {
    try {
        BibleAPIDataModel.bibleVersions.value = if (LOCAL_DATA) {
            Json.decodeFromString<BibleAPIBibles>(JSON_BIBLES_API_BIBLE_SELECT)
        } else {
            httpClient.get<GetBiblesAPIBible>(GetBiblesAPIBible()).body<BibleAPIBibles>()
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        // httpClient.close()
    }
}

internal suspend fun getBooksBibleAPI() {
    try {
        val getBooksAPIBible = if (LOCAL_DATA) {
            Json.decodeFromString<BibleAPIBook>(JSON_BOOKS_API_BIBLE)
        } else {
            httpClient.get(GetBooksAPIBible()).body<BibleAPIBook>()
        }
        BibleAPIDataModel.updateBooks(getBooksAPIBible)
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        // httpClient.close()
    }
}

internal suspend fun checkDatabaseRetention() {
    try {
        withContext(Dispatchers.IO) {
            val database = BibleBibleDatabase(driver = DriverFactory.createDriver())
            database.bibleBibleDatabaseQueries.cleanBibleVerses()
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "BB2452")
    }
}

internal suspend fun checkDatabaseSize() {
    try {
        withContext(Dispatchers.IO) {
            val database = BibleBibleDatabase(driver = DriverFactory.createDriver())
            val count = database.bibleBibleDatabaseQueries.countVerses().executeAsOne()
            val max = DATABASE_RETENTION
            if (count > max) {
                Napier.d(
                    "clean database :: count $count :: max $max :: diff ${count - max}",
                    tag = "BB2452"
                )
                database.bibleBibleDatabaseQueries.removeExcessVerses(max)
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "BB2452")
    }
}


internal suspend fun getChapterBibleAPI(chapterNumber: String, bibleId: String) {
    try {
        Napier.i("getChapterBibleAPI: $chapterNumber :: bibleId $bibleId", tag = "BB2452")
        Napier.d("start load", tag = "BB2452")
        val cachedData = loadVerseData(chapterNumber, bibleId)
        Napier.d("end load", tag = "BB2452")
        Napier.v("cachedData value ${cachedData?.data?.databaseKey}", tag = "BB2452")

        if (cachedData == null || cachedData.data?.verseCount == 0) {
            Napier.d("remote api: $chapterNumber", tag = "BB2452")
            fetchChapter(chapterNumber, bibleId)?.let {
                withContext(Dispatchers.Main) {
                    BibleAPIDataModel.updateChapterContent(it)
                    Napier.d("end fetch ui updated", tag = "BB2452")
                }
                Napier.d("start delay", tag = "BB2452")
//            delay(3000)
                Napier.d("end delay", tag = "BB2452")
                Napier.d("start insertVerse", tag = "BB2452")
                insertBibleVerses(it)
                Napier.d("end insertVerse", tag = "BB2452")
            }
        } else {
            Napier.v("enter DB query", tag = "BB2452")
            withContext(Dispatchers.Main) {
                Napier.v(
                    "update UI main thread cachedData :: ${
                        cachedData.data?.cleanedContent?.take(
                            100
                        )
                    }", tag = "BB2452"
                )
                BibleAPIDataModel.updateChapterContent(cachedData)
            }
            Napier.d(
                "db query ui update: ${BibleAPIDataModel.chapterContent.data?.id} :: ${
                    BibleAPIDataModel.chapterContent.data?.cleanedContent?.take(100)
                }", tag = "BB2452"
            )
        }

    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "BB2452")
    } finally {
        Napier.v("getChapterBibleAPI() :: finally", tag = "BB2455")
        // httpClient.close()
    }
}

private suspend fun fetchChapter(chapter: String, bibleId: String): ChapterContent? {
    return try {
        Napier.d("start fetch :: $chapter", tag = "BB2452")
        withContext(Dispatchers.IO) {
            httpClient.get<GetChapterAPIBible>(
                GetChapterAPIBible(
                    chapter = chapter,
                    bibleId = bibleId
                )
            )
                .body<ChapterContent>()
        }.also {
            Napier.d("end fetch", tag = "BB2452")
        }
    } catch (e: Exception) {
        "Error fetching chapter: ${chapter + " :: " + e.message}".also {
            BibleAPIDataModel.updateErrorSnackBar(it)
            Napier.e(it, tag = "BB2452")
        }
        null
    }
}

private suspend fun insertBibleVerses(chapterContent: ChapterContent) {
    return try {
        withContext(Dispatchers.IO) {
            Napier.d("inside insert load before delay", tag = "BB2452")
//            delay(3000)
            Napier.d("inside insert load end delay", tag = "BB2452")
            Napier.v("insertVerse chapterContent  :: ${chapterContent.data?.id}", tag = "BB2452")
            val database = BibleBibleDatabase(driver = DriverFactory.createDriver())
            chapterContent.let {
                database.bibleBibleDatabaseQueries.insertVerse(
                    uuid = it.data?.databaseKey ?: "",
                    id = it.data?.id ?: "",
                    bibleId = it.data?.bibleId ?: "",
                    number = it.data?.number ?: "",
                    bookId = it.data?.bookId ?: "",
                    reference = it.data?.reference ?: "",
                    copyright = it.data?.copyright ?: "",
                    verseCount = it.data?.verseCount?.toLong() ?: 0L,
                    content = it.data?.cleanedContent ?: "",
                    nextId = it.data?.next?.id ?: "",
                    nextNumber = it.data?.next?.number ?: "",
                    nextBookId = it.data?.next?.bookId ?: "",
                    previousId = it.data?.previous?.id ?: "",
                    previousNumber = it.data?.previous?.number ?: "",
                    previousBookId = it.data?.previous?.bookId ?: "",
                )
                Napier.v("insertVerse chapterContent  :: finish", tag = "BB2452")
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "BB2452")
    }
}

private suspend fun loadVerseData(selectedChapter: String, bibleId: String): ChapterContent? {
    return try {
        withContext(Dispatchers.IO) {
            Napier.d("inside start load before delay", tag = "BB2452")
            // delay(3000)
            Napier.d("inside start load end delay", tag = "BB2452")

            val database = BibleBibleDatabase(driver = DriverFactory.createDriver())
            val bibleQueries = database.bibleBibleDatabaseQueries
                .selectVersesById(selectedChapter, bibleId).executeAsOneOrNull()

            Napier.v("bibleQueries selectedChapter $selectedChapter :: hello world", tag = "BB2452")
            Napier.v("bibleQueries ${bibleQueries?.content?.take(100)}", tag = "BB2452")

            bibleQueries?.let {
                ChapterContent(
                    ChapterData(
                        id = it.id,
                        bibleId = it.bibleId,
                        number = it.number,
                        bookId = it.bookId,
                        reference = it.reference,
                        content = it.content,
                        verseCount = it.verseCount?.toInt(),
                        next = ChapterReference(
                            id = it.nextId,
                            number = it.nextNumber,
                            bookId = it.nextBookId
                        ),
                        previous = ChapterReference(
                            id = it.previousId,
                            number = it.previousNumber,
                            bookId = it.previousBookId
                        ),
                        copyright = ""
                    )
                )
            }.also {
                Napier.d("UI ready to update from DB query end", tag = "BB2452")
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "BB2452")
        null
    }
}
