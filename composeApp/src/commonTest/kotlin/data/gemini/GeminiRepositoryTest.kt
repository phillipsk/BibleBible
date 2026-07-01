package data.gemini

import data.GeminiModel
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeminiRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testGenerateContentUpdatesModel() = runTest {
        // Prepare mock data
        val expectedSummary = "Mocked AI Summary"
        val mockResponse = GeminiResponseDto(
            candidates = listOf(
                CandidateDto(
                    content = ContentDto(
                        parts = listOf(
                            PartDto(text = expectedSummary)
                        )
                    )
                )
            )
        )

        // Setup MockEngine
        val mockEngine = MockEngine { request ->
            // Verify model name in URL
            assertTrue(request.url.encodedPath.contains(GEMINI_MODEL), "URL should contain the correct Gemini model name: $GEMINI_MODEL")
            
            respond(
                content = json.encodeToString(mockResponse),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val mockClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        // Action: Set state to look successful after update
        GeminiModel.showSummary = true
        GeminiModel.isLoading = false

        // Execute
        generateContent("Test prompt", mockClient)

        // Verify
        assertEquals(expectedSummary, GeminiModel.geminiDataText)
        assertTrue(GeminiModel.isSuccessful, "GeminiModel should be in successful state")
    }
}
