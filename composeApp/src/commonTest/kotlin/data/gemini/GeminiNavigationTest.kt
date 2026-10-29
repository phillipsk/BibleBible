package data.gemini

import data.GeminiModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeminiNavigationTest {
    @Test
    fun navigatingAwayCancelsSummaryAndRejectsALateResponse() = runTest {
        val started = CompletableDeferred<Unit>()
        val releaseResponse = CompletableDeferred<Unit>()
        val client = HttpClient(MockEngine {
            started.complete(Unit)
            withContext(NonCancellable) {
                releaseResponse.await()
                respond(
                    """{"candidates":[{"content":{"parts":[{"text":"Old chapter summary"}]}}]}""",
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }) { install(ContentNegotiation) { json() } }

        try {
            GeminiModel.showSummary = true
            GeminiModel.isLoading = true
            val request = launch {
                GeminiModel.generateAISummary(pullToRefresh = true) { prompt -> generateContent(prompt, client) }
            }
            started.await()
            GeminiModel.cancelPendingSummary()
            releaseResponse.complete(Unit)
            request.join()

            assertTrue(request.isCancelled)
            assertNull(GeminiModel.geminiDataText, "A canceled chapter's response must not become the new summary")
            assertFalse(GeminiModel.showSummary)
            assertFalse(GeminiModel.isLoading)
        } finally {
            releaseResponse.complete(Unit)
            GeminiModel.cancelPendingSummary()
            client.close()
        }
    }
}
