package data.bibleIQ

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BibleIQRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testGetChapterBibleIQ_Success() = runTest {
        val mockVerses = listOf(
            BibleChapter(id = "1", b = "1", c = "1", v = "1", t = "Test Verse Content")
        )
        
        val mockEngine = MockEngine { _ ->
            respond(
                content = json.encodeToString(mockVerses),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
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
        
        BibleIQDataModel.updateBibleChapter(verses, chapterCount, version)
        
        val uiState = BibleIQDataModel.bibleChapter
        assertNotNull(uiState)
        assertEquals(50L, uiState?.chapterList?.size?.toLong())
        assertEquals(1, uiState?.bookId)
    }
}
