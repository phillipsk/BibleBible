package data

import data.apiBible.BibleAPIBook
import data.apiBible.BibleAPIDataModel
import data.apiBible.json.JSON_BOOKS_API_BIBLE
import data.bibleIQ.*
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BibleRepositoryIntegrationTest {
    private fun clearCache() {
        DriverFactory.createDriver()?.let { driver ->
            BibleBibleDatabase(driver).bibleBibleDatabaseQueries.removeExcessVerses(0)
            driver.execute(null, "DELETE FROM ChapterCounts", 0)
        }
    }

    @Test fun chapterLoadsThroughRepositoryAndUpdatesReader() = runTest {
        clearCache()
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val original = BibleAPIDataModel.bibleBooks
        val paths = mutableListOf<String>()
        val client = createHttpClient(AppHttpClientConfig("iq-bible.p.rapidapi.com", "X-RapidAPI-Key", "test"),
            engine = MockEngine { request ->
                paths += request.url.encodedPath
                val body = when (request.url.encodedPath) {
                    "/GetChapter" -> {
                        assertEquals("bbe", request.url.parameters["versionId"])
                        """[{"id":"01001001","b":"1","c":"1","v":"1","t":"At the first God made the heaven and the earth."}]"""
                    }
                    "/GetChapterCount" -> """{"chapterCount":50}"""
                    else -> error("Unexpected request")
                }
                respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }, enableLogging = false)
        try {
            val books = Json.decodeFromString<BibleAPIBook>(JSON_BOOKS_API_BIBLE)
            BibleAPIDataModel.updateBooks(books)
            getChapterBibleIQ(books.data!!.first(), version = "BBE", updateReadingHistory = false, client = client)
            assertEquals(listOf("/GetChapter", "/GetChapterCount"), paths)
            assertEquals("[1] At the first God made the heaven and the earth.", BibleIQDataModel.bibleChapter?.text)
            assertEquals(50, BibleIQDataModel.bibleChapter?.chapterList?.size)
        } finally {
            clearCache()
            client.close()
            BibleAPIDataModel.updateBooks(original)
            BibleIQDataModel.updateBibleChapter(emptyList(), null, "KJV")
            Dispatchers.resetMain()
        }
    }

    @Test fun emptyApiResponseReportsErrorInsteadOfReplacingPassage() = runTest {
        clearCache()
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        while (BibleIQDataModel.snackBarChannel.tryReceive().isSuccess) { }
        val client = createHttpClient(AppHttpClientConfig("iq-bible.p.rapidapi.com", "X-RapidAPI-Key", "test"),
            engine = MockEngine { respond("[]", headers = headersOf(HttpHeaders.ContentType, "application/json")) }, enableLogging = false)
        try {
            BibleIQDataModel.updateBibleChapter(listOf(BibleChapter("1", "1", "1", "1", "Previous")), ChapterCount(50), "KJV")
            getChapterBibleIQ(data.apiBible.BookData(), updateReadingHistory = false, client = client)
            assertEquals("Error fetching chapter", BibleIQDataModel.snackBarChannel.tryReceive().getOrNull())
            assertEquals("[1] Previous", BibleIQDataModel.bibleChapter?.text)
        } finally {
            clearCache()
            client.close()
            BibleIQDataModel.updateBibleChapter(emptyList(), null, "KJV")
            Dispatchers.resetMain()
        }
    }
}
