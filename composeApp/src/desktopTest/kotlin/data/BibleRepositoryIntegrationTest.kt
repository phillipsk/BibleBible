package data

import data.apiBible.BibleAPIBook
import data.apiBible.BibleAPIDataModel
import data.apiBible.json.JSON_BOOKS_API_BIBLE
import data.bibleIQ.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import kotlin.test.*

/** Desktop's disabled persistent cache makes these deterministic network-to-model tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class BibleRepositoryIntegrationTest {
    @Test fun chapterLoadsThroughRepositoryAndUpdatesReader() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val original = BibleAPIDataModel.bibleBooks
        val paths = mutableListOf<String>()
        val client = createHttpClient(AppHttpClientConfig("iq-bible.p.rapidapi.com", "X-RapidAPI-Key", "test"),
            engine = MockEngine { request ->
                paths += request.url.encodedPath
                val body = when (request.url.encodedPath) {
                    "/bibles/eng_bbe/GEN/1" -> {
                        assertTrue(request.url.parameters.isEmpty())
                        """{"version":"eng_bbe","data":[{"book_number":1,"chapter_number":1,"verse_number":1,"text":"At the first God made the heaven and the earth."}]}"""
                    }
                    "/bibles/eng_bbe/GEN/meta" -> """{"chapter_count":50}"""
                    else -> error("Unexpected request")
                }
                respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }, enableLogging = false)
        try {
            val books = Json.decodeFromString<BibleAPIBook>(JSON_BOOKS_API_BIBLE)
            BibleAPIDataModel.updateBooks(books)
            getChapterBibleIQ(books.data!!.first(), version = "BBE", updateReadingHistory = false, client = client)
            assertEquals(listOf("/bibles/eng_bbe/GEN/1", "/bibles/eng_bbe/GEN/meta"), paths)
            assertEquals("[1] At the first God made the heaven and the earth.", BibleIQDataModel.bibleChapter?.text)
            assertEquals(50, BibleIQDataModel.bibleChapter?.chapterList?.size)
        } finally {
            client.close()
            BibleAPIDataModel.updateBooks(original)
            BibleIQDataModel.updateBibleChapter(emptyList(), null, "KJV")
            Dispatchers.resetMain()
        }
    }

    @Test fun emptyApiResponseReportsErrorInsteadOfReplacingPassage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        while (BibleIQDataModel.snackBarChannel.tryReceive().isSuccess) { }
        val client = createHttpClient(AppHttpClientConfig("iq-bible.p.rapidapi.com", "X-RapidAPI-Key", "test"),
            engine = MockEngine { respond("""{"version":"eng_kjv","data":[]}""", headers = headersOf(HttpHeaders.ContentType, "application/json")) }, enableLogging = false)
        try {
            BibleIQDataModel.updateBibleChapter(listOf(BibleChapter("1", "1", "1", "1", "Previous")), ChapterCount(50), "KJV")
            getChapterBibleIQ(data.apiBible.BookData(), updateReadingHistory = false, client = client)
            assertEquals("Unable to load this chapter. Check your connection and try again.", BibleIQDataModel.snackBarChannel.tryReceive().getOrNull())
            assertEquals("[1] Previous", BibleIQDataModel.bibleChapter?.text)
        } finally {
            client.close()
            BibleIQDataModel.updateBibleChapter(emptyList(), null, "KJV")
            Dispatchers.resetMain()
        }
    }
}
