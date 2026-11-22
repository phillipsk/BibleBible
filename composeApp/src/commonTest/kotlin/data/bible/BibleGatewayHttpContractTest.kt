package data.bible

import data.AppHttpClientConfig
import data.config.CloudSessionManager
import data.createHttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.resources.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** Exercises the production client configuration and typed Cloud Gateway requests without network. */
class BibleGatewayHttpContractTest {
    @AfterTest
    fun resetSession() {
        CloudSessionManager.resetForTesting()
    }

    @Test
    fun chapterRequestPreservesBookChapterAndBsbTranslationWithoutClientKeys() = runTest {
        CloudSessionManager.overrideGatewayUrlForTesting("https://biblebible-api.example.run.app")
        CloudSessionManager.sessionToken = "session-token-abc"
        val engine = MockEngine { request ->
            assertEquals("https", request.url.protocol.name)
            assertEquals("biblebible-api.example.run.app", request.url.host)
            assertEquals("/GetChapter", request.url.encodedPath)
            assertEquals("43", request.url.parameters["bookId"])
            assertEquals("3", request.url.parameters["chapterId"])
            assertEquals("bsb", request.url.parameters["versionId"])
            assertEquals("Bearer session-token-abc", request.headers[HttpHeaders.Authorization])
            assertNull(request.headers["X-RapidAPI-Key"])
            assertNull(request.headers["x-goog-api-key"])
            respond(
                """[{"id":"43003016","b":"43","c":"3","v":"16","t":"For God so loved the world","extra":"ignored"}]""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = createHttpClient(
            AppHttpClientConfig(baseUrl = CloudSessionManager.gatewayHost),
            engine = engine,
            enableLogging = false,
        )
        try {
            val verse = client.get(GetChapter(43, "3", "bsb")).body<List<BibleChapter>>().single()
            assertEquals("43003016", verse.id)
            assertEquals("16", verse.v)
            assertEquals("For God so loved the world", verse.t)
        } finally {
            client.close()
        }
    }

    @Test
    fun chapterCountUsesBookIdAndDecodesCount() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/GetChapterCount", request.url.encodedPath)
            assertEquals("19", request.url.parameters["bookId"])
            respond("""{"chapterCount":150}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createHttpClient(
            AppHttpClientConfig(baseUrl = "biblebible-api.example.run.app"),
            engine = engine,
            enableLogging = false,
        )
        try {
            assertEquals(150L, client.get(GetChapterCount(19)).body<ChapterCount>().chapterCount)
        } finally {
            client.close()
        }
    }

    @Test
    fun verseOfTheDayRequestUsesSelectedTranslationAndDecodesPayload() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/GetVerseOfTheDay", request.url.encodedPath)
            assertEquals("bbe", request.url.parameters["versionId"])
            respond(
                """{"version":"BBE","date":"2026-11-17","data":{"book_number":43,"chapter_number":3,"verse_number":16,"text":"For God had such love for the world","book_usfm":"JHN"}}""",
                HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = createHttpClient(
            AppHttpClientConfig(baseUrl = "biblebible-api.example.run.app"),
            engine = engine,
            enableLogging = false,
        )
        try {
            val daily = fetchDailyVerse("BBE", client)
            assertEquals("BBE", daily.version)
            assertEquals(43, daily.data.bookNumber)
            assertEquals("JHN", daily.data.bookUsfm)
            assertFalse(daily.data.text.isBlank())
        } finally {
            client.close()
        }
    }
}
