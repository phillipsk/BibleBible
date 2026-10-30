package data.gemini

import data.AppHttpClientConfig
import data.GeminiModel
import data.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeminiRepositoryTest {
    private val secret = "synthetic-review-key"

    @AfterTest
    fun resetModel() {
        GeminiModel.updateGeminiData(GeminiResponseDto())
        GeminiModel.showSummary = false
        GeminiModel.isLoading = false
    }

    @Test
    fun requestSendsSecretHeaderAndExplicitTokenLimitInActualJson() = runTest {
        val client = client(MockEngine { request ->
            assertEquals("/v1beta/models/$GEMINI_MODEL:generateContent", request.url.encodedPath)
            assertEquals(secret, request.headers["x-goog-api-key"])
            assertFalse(request.url.parameters.contains("key"))
            assertEquals(ContentType.Application.Json, request.body.contentType)
            val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            val config = body.getValue("generationConfig").jsonObject
            assertEquals(2048, config.getValue("maxOutputTokens").jsonPrimitive.int)
            assertEquals(1, config.getValue("candidateCount").jsonPrimitive.int)
            respond(Json.encodeToString(summary("Summary")), HttpStatusCode.OK, jsonHeaders())
        })
        try {
            GeminiModel.showSummary = true
            generateContent("Test prompt", client)
            assertEquals("Summary", GeminiModel.geminiDataText)
            assertTrue(GeminiModel.isSuccessful)
            assertNull(GeminiModel.errorMessage)
        } finally { client.close() }
    }

    @Test
    fun depletedCreditsAreReportedWithoutReusingOldSummary() = runTest {
        GeminiModel.updateGeminiData(summary("Old summary"))
        GeminiModel.showSummary = true
        request(HttpStatusCode.PaymentRequired, """{"error":{"message":"Prepayment credits depleted","status":"RESOURCE_EXHAUSTED"}}""")
        assertNull(GeminiModel.geminiDataText)
        assertFalse(GeminiModel.isSuccessful)
        assertTrue(GeminiModel.errorMessage.orEmpty().contains("prepaid credits"))
    }

    @Test
    fun authorizationAndQuotaErrorsHaveDifferentMessages() = runTest {
        request(HttpStatusCode.Forbidden, "{}")
        assertTrue(GeminiModel.errorMessage.orEmpty().contains("API key"))
        request(HttpStatusCode.TooManyRequests, "{}")
        assertTrue(GeminiModel.errorMessage.orEmpty().contains("quota or rate limit"))
    }

    @Test
    fun nonJsonServerErrorsDoNotExposeProviderContent() = runTest {
        request(HttpStatusCode.ServiceUnavailable, "private-provider-content")
        assertTrue(GeminiModel.errorMessage.orEmpty().contains("503"))
        assertFalse(GeminiModel.errorMessage.orEmpty().contains("private-provider-content"))
    }

    @Test
    fun emptyResponseIsNotSuccessful() = runTest {
        request(HttpStatusCode.OK, "{}")
        assertTrue(GeminiModel.errorMessage.orEmpty().contains("no summary"))
        assertFalse(GeminiModel.isSuccessful)
    }

    @Test
    fun successfulRetryClearsPreviousError() = runTest {
        request(HttpStatusCode.PaymentRequired, "{}")
        request(HttpStatusCode.OK, Json.encodeToString(summary("Retry summary")))
        assertEquals("Retry summary", GeminiModel.geminiDataText)
        assertNull(GeminiModel.errorMessage)
    }

    @Test
    fun transportFailuresClearStaleDataAndHideExceptionDetails() = runTest {
        GeminiModel.updateGeminiData(summary("Old summary"))
        val client = client(MockEngine { error("sensitive-request-detail") })
        try {
            generateContent("Test prompt", client)
            assertNull(GeminiModel.geminiDataText)
            assertTrue(GeminiModel.errorMessage.orEmpty().contains("connection"))
            assertFalse(GeminiModel.errorMessage.orEmpty().contains("sensitive-request-detail"))
        } finally { client.close() }
    }

    @Test
    fun coroutineCancellationIsPropagated() = runTest {
        val client = client(MockEngine { throw CancellationException("Cancelled") })
        try {
            assertFailsWith<CancellationException> { generateContent("Test prompt", client) }
            assertNull(GeminiModel.errorMessage)
        } finally { client.close() }
    }

    @Test
    fun diagnosticLoggingDoesNotExposeCredential() = runTest {
        val messages = mutableListOf<String>()
        val logger = object : Logger { override fun log(message: String) { messages.add(message) } }
        val client = client(
            MockEngine { respond(Json.encodeToString(summary("Summary")), HttpStatusCode.OK, jsonHeaders()) },
            logger, true,
        )
        try {
            generateContent("Test prompt", client)
            assertFalse(messages.joinToString().contains(secret))
        } finally { client.close() }
    }

    private fun summary(text: String) = GeminiResponseDto(
        candidates = listOf(CandidateDto(ContentDto(parts = listOf(PartDto(text)))))
    )

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

    private fun client(engine: MockEngine, logger: Logger = Logger.SIMPLE, enableLogging: Boolean = false): HttpClient =
        createHttpClient(
            AppHttpClientConfig("generativelanguage.googleapis.com", "x-goog-api-key", secret),
            engine = engine, logger = logger, enableLogging = enableLogging,
        )

    private suspend fun request(status: HttpStatusCode, content: String) {
        val client = client(MockEngine { respond(content, status, jsonHeaders()) })
        try { generateContent("Test prompt", client) } finally { client.close() }
    }
}
