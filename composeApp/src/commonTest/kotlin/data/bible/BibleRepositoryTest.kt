package data.bible

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BibleRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @AfterTest
    fun resetState() {
        BibleState.updateBibleChapter(emptyList(), null, "KJV")
    }

    @Test
    fun testFetchBibleChapterSuccess() = runTest {
        val mockVerses = listOf(
            BibleChapter(id = "1", b = "1", c = "1", v = "1", t = "Test Verse Content")
        )

        val mockEngine = MockEngine { _ ->
            respond(
                content = json.encodeToString(mockVerses),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val mockClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val response = mockClient.get("https://test.com")
        val result = response.body<List<BibleChapter>>()

        assertEquals(1, result.size)
        assertEquals("Test Verse Content", result[0].t)
    }

    @Test
    fun testChapterCountingLogic() {
        val chapterCount = ChapterCount(chapterCount = 50)
        val version = "kjv"
        val verses = listOf(
            BibleChapter(id = "1", b = "1", c = "1", v = "1", t = "Genesis 1:1")
        )

        BibleState.updateBibleChapter(verses, chapterCount, version)

        val uiState = BibleState.bibleChapter
        assertNotNull(uiState)
        assertEquals(50L, uiState.chapterList?.size?.toLong())
        assertEquals(1, uiState.bookId)
    }
}
