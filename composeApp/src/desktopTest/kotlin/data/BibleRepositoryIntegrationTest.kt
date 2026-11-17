package data

import data.apiBible.BibleAPIBook
import data.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import data.apiBible.json.JSON_BOOKS_API_BIBLE
import data.bibleIQ.BibleChapter
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.ChapterCount
import data.bibleIQ.getChapterBibleIQ
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** Desktop's disabled persistent cache makes these deterministic network-to-model tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class BibleRepositoryIntegrationTest {
    @Test
    fun chapterLoadsThroughRepositoryAndUpdatesReader() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val original = BibleAPIDataModel.bibleBooks
        val paths = mutableListOf<String>()
        val client = createHttpClient(
            AppHttpClientConfig(baseUrl = "biblebible-api.example.run.app"),
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
            },
            enableLogging = false
        )
        try {
            val books = Json.decodeFromString<BibleAPIBook>(JSON_BOOKS_API_BIBLE)
            BibleAPIDataModel.updateBooks(books)
            getChapterBibleIQ(books.data!!.first(), version = "BSB", updateReadingHistory = false, client = client)
            assertEquals(listOf("/GetChapter", "/GetChapterCount"), paths)
            assertEquals(
                "[1] In the beginning God created the heavens and the earth.",
                BibleIQDataModel.bibleChapter?.text
            )
            assertEquals(50, BibleIQDataModel.bibleChapter?.chapterList?.size)
        } finally {
            client.close()
            BibleAPIDataModel.updateBooks(original)
            BibleIQDataModel.updateBibleChapter(emptyList(), null, "KJV")
            Dispatchers.resetMain()
        }
    }

    @Test
    fun emptyApiResponseReportsErrorInsteadOfReplacingPassage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        while (BibleIQDataModel.snackBarChannel.tryReceive().isSuccess) { }
        val client = createHttpClient(
            AppHttpClientConfig(baseUrl = "biblebible-api.example.run.app"),
            engine = MockEngine { respond("[]", headers = headersOf(HttpHeaders.ContentType, "application/json")) },
            enableLogging = false
        )
        try {
            BibleIQDataModel.updateBibleChapter(listOf(BibleChapter("1", "1", "1", "1", "Previous")), ChapterCount(50), "KJV")
            getChapterBibleIQ(BookData(), updateReadingHistory = false, client = client)
            assertEquals("Error fetching chapter", BibleIQDataModel.snackBarChannel.tryReceive().getOrNull())
            assertEquals("[1] Previous", BibleIQDataModel.bibleChapter?.text)
        } finally {
            client.close()
            BibleIQDataModel.updateBibleChapter(emptyList(), null, "KJV")
            Dispatchers.resetMain()
        }
    }
}
