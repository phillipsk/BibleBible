package data

import data.bible.BibleBooksResponse
import data.bible.BibleChapter
import data.bible.BibleState
import data.bible.BookData
import data.bible.ChapterCount
import data.bible.fetchBibleChapter
import data.bible.json.BUNDLED_BIBLE_BOOKS_JSON
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
        val original = BibleState.bibleBooks
        val paths = mutableListOf<String>()
        val client = createHttpClient(AppHttpClientConfig(baseUrl = "biblebible-api.example.run.app"),
            engine = MockEngine { request ->
                paths += request.url.encodedPath
                val body = when (request.url.encodedPath) {
                    "/GetChapter" -> {
                        assertEquals("bsb", request.url.parameters["versionId"])
                        """[{"id":"01001001","b":"1","c":"1","v":"1","t":"In the beginning God created the heavens and the earth."}]"""
                    }
                    "/GetChapterCount" -> """{"chapterCount":50}"""
                    else -> error("Unexpected request: ${request.url.encodedPath}")
                }
                respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }, enableLogging = false)
        try {
            val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
            BibleState.updateBooks(books)
            fetchBibleChapter(books.data!!.first(), version = "BSB", updateReadingHistory = false, client = client)
            assertEquals(listOf("/GetChapter", "/GetChapterCount"), paths)
            assertEquals("[1] In the beginning God created the heavens and the earth.", BibleState.bibleChapter?.text)
            assertEquals(50, BibleState.bibleChapter?.chapterList?.size)
        } finally {
            clearCache()
            client.close()
            BibleState.updateBooks(original)
            BibleState.updateBibleChapter(emptyList(), null, "KJV")
            Dispatchers.resetMain()
        }
    }

    @Test fun emptyApiResponseReportsErrorInsteadOfReplacingPassage() = runTest {
        clearCache()
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        while (BibleState.snackBarChannel.tryReceive().isSuccess) { }
        val client = createHttpClient(AppHttpClientConfig(baseUrl = "biblebible-api.example.run.app"),
            engine = MockEngine { respond("[]", headers = headersOf(HttpHeaders.ContentType, "application/json")) }, enableLogging = false)
        try {
            BibleState.updateBibleChapter(listOf(BibleChapter("1", "1", "1", "1", "Previous")), ChapterCount(50), "KJV")
            fetchBibleChapter(BookData(), updateReadingHistory = false, client = client)
            assertEquals("Error fetching chapter", BibleState.snackBarChannel.tryReceive().getOrNull())
            assertEquals("[1] Previous", BibleState.bibleChapter?.text)
        } finally {
            clearCache()
            client.close()
            BibleState.updateBibleChapter(emptyList(), null, "KJV")
            Dispatchers.resetMain()
        }
    }
}
