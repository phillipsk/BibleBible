package data.bibleIQ

import data.AppHttpClientConfig
import data.apiBible.BookData
import data.apiBible.getBooksBibleAPI
import data.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BibleIQRepositoryRequestTest {
    @Test
    fun overlappingChapterLoadsAndRevisitsAvoidDuplicateProviderCalls() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths.add(request.url.encodedPath)
            delay(50)
            val response = when (request.url.encodedPath) {
                "/GetChapterCount" -> """{"chapterCount":50}"""
                "/GetChapter" -> """[{"id":"1","b":"1","c":"1","v":"1","t":"${request.url.parameters["versionId"]}"}]"""
                else -> error("Unexpected endpoint")
            }
            respond(response, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createHttpClient(AppHttpClientConfig("iq-bible.p.rapidapi.com", "X-RapidAPI-Key", "fake-key"), engine = engine)
        try {
            getBooksBibleAPI() // Bundled JSON, no network request.
            val book = BookData(name = "Genesis")
            val first = async { getChapterBibleIQ(book, version = "test_kjv", updateReadingHistory = false, client = client) }
            val second = async { getChapterBibleIQ(book, version = "TEST_KJV", updateReadingHistory = false, client = client) }
            first.await(); second.await()
            assertEquals(1, paths.count { it == "/GetChapter" })
            assertEquals(1, paths.count { it == "/GetChapterCount" })
            getChapterBibleIQ(book, version = "test_kjv", updateReadingHistory = false, client = client)
            assertEquals(2, paths.size)
            getChapterBibleIQ(book, version = "test_asv", updateReadingHistory = false, client = client)
            assertEquals(2, paths.count { it == "/GetChapter" })
            assertEquals(1, paths.count { it == "/GetChapterCount" })
            assertEquals("[1] test_asv", BibleIQDataModel.bibleChapter?.text)
        } finally {
            client.close()
            Dispatchers.resetMain()
        }
    }
}
