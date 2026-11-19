package data.bibleIQ

import data.AppHttpClientConfig
import data.createHttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.resources.get
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** Exercises the production client configuration and typed resources, without credentials/network. */
class BibleIQHttpContractTest {
    @Test
    fun chapterRequestPreservesBookChapterAndBbeTranslation() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https", request.url.protocol.name)
            assertEquals("iq-bible.p.rapidapi.com", request.url.host)
            assertEquals("/GetChapter", request.url.encodedPath)
            assertEquals("43", request.url.parameters["bookId"])
            assertEquals("3", request.url.parameters["chapterId"])
            assertEquals("bbe", request.url.parameters["versionId"])
            assertEquals("test-only-key", request.headers["X-RapidAPI-Key"])
            respond("""[{"id":"43003016","b":"43","c":"3","v":"16","t":"For God had such love for the world","extra":"ignored"}]""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createHttpClient(AppHttpClientConfig("iq-bible.p.rapidapi.com", "X-RapidAPI-Key", "test-only-key"), engine = engine, enableLogging = false)
        try {
            val verse = client.get(GetChapter(43, "3", "bbe")).body<List<BibleChapter>>().single()
            assertEquals("43003016", verse.id)
            assertEquals("16", verse.v)
            assertEquals("For God had such love for the world", verse.t)
        } finally { client.close() }
    }

    @Test
    fun chapterCountUsesBookIdAndDecodesCount() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/GetChapterCount", request.url.encodedPath)
            assertEquals("19", request.url.parameters["bookId"])
            respond("""{"chapterCount":150}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createHttpClient(AppHttpClientConfig("iq-bible.p.rapidapi.com", "X-RapidAPI-Key", "test"), engine = engine, enableLogging = false)
        try { assertEquals(150L, client.get(GetChapterCount(19)).body<ChapterCount>().chapterCount) }
        finally { client.close() }
    }
}
