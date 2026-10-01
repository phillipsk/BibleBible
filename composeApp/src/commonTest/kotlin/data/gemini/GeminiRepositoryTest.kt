package data.gemini

import data.AppHttpClientConfig
import data.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class GeminiRepositoryTest {
    private val responseJson = """{"candidates":[{"content":{"parts":[{"text":"first "},{"text":"second"}]}}],"usageMetadata":{"totalTokenCount":25}}"""

    @Test
    fun requestUsesSecretHeaderAndBoundedJsonBody() = runTest {
        val messages = mutableListOf<String>()
        val key = "fake-gemini-key"
        val engine = MockEngine { request ->
            assertEquals("/v1beta/models/$GEMINI_MODEL:generateContent", request.url.encodedPath)
            assertEquals("generativelanguage.googleapis.com", request.url.host)
            assertEquals(key, request.headers["x-goog-api-key"])
            assertFalse(request.url.parameters.contains("key"))
            assertEquals(ContentType.Application.Json, request.body.contentType)
            val body = Json.decodeFromString<RequestBody>(request.body.toByteArray().decodeToString())
            assertEquals(2048, body.generationConfig.maxOutputTokens)
            assertEquals(1, body.generationConfig.candidateCount)
            assertEquals("Test prompt", body.contents.single().parts.single().text)
            respond(responseJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createHttpClient(
            AppHttpClientConfig("generativelanguage.googleapis.com", "x-goog-api-key", key),
            engine = engine,
            logger = object : Logger { override fun log(message: String) { messages.add(message) } },
            enableLogging = true,
        )
        try {
            val response = generateContent("Test prompt", client)
            assertEquals("first second", response.summaryText)
            assertEquals(25, response.usageMetadata?.totalTokenCount)
            assertFalse(messages.joinToString().contains(key))
        } finally { client.close() }
    }

    @Test
    fun quotaErrorIsRejectedAndDoesNotExposeProviderBody() = runTest {
        val client = createHttpClient(
            AppHttpClientConfig("generativelanguage.googleapis.com", "x-goog-api-key", "fake-key"),
            engine = MockEngine { respond("private provider error", HttpStatusCode.TooManyRequests) },
        )
        try {
            val error = assertFailsWith<GeminiRequestException> { generateContent("prompt", client) }
            assertEquals("AI summary request failed (HTTP 429).", error.message)
        } finally { client.close() }
    }

    @Test
    fun emptyResponseIsRejected() = runTest {
        val client = createHttpClient(
            AppHttpClientConfig("generativelanguage.googleapis.com", "x-goog-api-key", "fake-key"),
            engine = MockEngine { respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) },
        )
        try {
            assertFailsWith<GeminiRequestException> { generateContent("prompt", client) }
        } finally { client.close() }
    }

    @Test
    fun cancellationPropagatesToCaller() = runTest {
        val client = createHttpClient(
            AppHttpClientConfig("generativelanguage.googleapis.com", "x-goog-api-key", "fake-key"),
            engine = MockEngine { throw CancellationException("Navigated away") },
        )
        try {
            assertFailsWith<CancellationException> { generateContent("prompt", client) }
        } finally { client.close() }
    }
}
