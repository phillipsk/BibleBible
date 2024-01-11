package data.apiBible

import data.api.apiBible.BibleAPIDataModel
import data.apiBible.json.JSON_BIBLES_API_BIBLE
import data.apiBible.json.JSON_BOOKS_API_BIBLE
import data.httpClient
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

const val LOCAL_DATA = true

suspend fun getBiblesBibleAPI() {
    try {
        BibleAPIDataModel.bibleVersions.value = if (LOCAL_DATA) {
            Json.decodeFromString<BibleAPIBibles>(JSON_BIBLES_API_BIBLE)
        } else {
            httpClient.get<GetBiblesAPIBible>(GetBiblesAPIBible()).body<BibleAPIBibles>()
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        httpClient.close()
    }
}

suspend fun getBooksBibleAPI() {
    try {
        val getBooksAPIBible = if (LOCAL_DATA) {
            Json.decodeFromString<BibleAPIBook>(JSON_BOOKS_API_BIBLE)
        } else {
            httpClient.get(GetBooksAPIBible()).body<BibleAPIBook>()
        }
        BibleAPIDataModel.books.value = getBooksAPIBible
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        httpClient.close()
    }
}


suspend fun getChapterBibleAPI() {
    try {

        Napier.d("start load", tag = "BB2452")
        val cachedData = loadVerseData()
        Napier.d("end load", tag = "BB2452")

        if (cachedData == null) {
            Napier.d(
                "remote api: ${BibleAPIDataModel.chapter.value.data?.id} :: ${BibleAPIDataModel.chapter.value.data?.cleanedContent?.take(130)}",
                tag = "BB2452"
            )
            Napier.d("start fetch", tag = "BB2452")
            BibleAPIDataModel.chapter.value =
                httpClient.get(GetChapterAPIBible()).body<ChapterContent>()
            Napier.d("end fetch", tag = "BB2452")
            Napier.d("start delay", tag = "BB2452")
//            delay(3000)
            Napier.d("end delay", tag = "BB2452")
            Napier.d("start insertVerse", tag = "BB2452")
            insertBibleVerses()
            Napier.d("end insertVerse", tag = "BB2452")
        } else {
            BibleAPIDataModel.chapter.value = cachedData
            Napier.d(cachedData.toString(), tag = "BB2452-ios")
            Napier.d(
                "db query: ${BibleAPIDataModel.chapter.value.data?.id} :: ${BibleAPIDataModel.chapter.value.data?.cleanedContent?.take(130)}",
                tag = "BB2452"
            )
        }

    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "BB2452")
    } finally {
        httpClient.close()
    }
}

private suspend fun insertBibleVerses() {
    withContext(Dispatchers.IO) {
        val database = BibleBibleDatabase(driver = DriverFactory.createDriver())
        BibleAPIDataModel.chapter.value.let {
            database.bibleBibleDatabaseQueries.insertVerse(
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
        }
    }
}


// Assuming this function is called within a coroutine scope
private suspend fun loadVerseData(): ChapterContent? {
    return withContext(Dispatchers.IO) {
        Napier.d("inside start load before delay", tag = "BB2452")
//        delay(3000)
        Napier.d("inside start load end delay", tag = "BB2452")
        val database = BibleBibleDatabase(driver = DriverFactory.createDriver())
        val bibleQueries = database.bibleBibleDatabaseQueries
            .selectVersesById(BibleAPIDataModel.selectedChapter).executeAsList()

        bibleQueries.firstOrNull()?.let {
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
                    copyright = "",
                )
            )
        }
    }
}
