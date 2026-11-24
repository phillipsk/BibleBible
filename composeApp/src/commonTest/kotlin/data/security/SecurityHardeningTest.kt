package data.security

import data.AppHttpClientConfig
import data.config.CloudRuntimeConfigDto
import data.config.CloudSessionManager
import data.createHttpClient
import data.gemini.SummaryRequestDto
import email.kevinphillips.biblebible.security.ProtectedEndpoints
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityHardeningTest {
    @AfterTest
    fun reset() {
        CloudSessionManager.resetForTesting()
    }

    @Test
    fun protectedEndpointsDecodeAllGatewayRoutesAndHeadersInMemory() {
        assertTrue(ProtectedEndpoints.gatewayUrl().startsWith("https://"))
        assertEquals("/v1/config", ProtectedEndpoints.configPath())
        assertEquals("/v1/auth/challenge", ProtectedEndpoints.authChallengePath())
        assertEquals("/v1/auth/token", ProtectedEndpoints.authTokenPath())
        assertEquals("/v1/gemini/summary", ProtectedEndpoints.geminiSummaryPath())
        assertEquals("/v1/analytics/events", ProtectedEndpoints.analyticsEventsPath())
        assertEquals("/GetBiblicalShorts", ProtectedEndpoints.shortsFeedPath())
        assertEquals("/v1/shorts/media/", ProtectedEndpoints.shortsMediaPrefix())
        assertEquals("/GetChapter", ProtectedEndpoints.chapterPath())
        assertEquals("/GetChapterCount", ProtectedEndpoints.chapterCountPath())
        assertEquals("/GetVerseOfTheDay", ProtectedEndpoints.dailyVersePath())
        assertEquals("/v1/notifications/register", ProtectedEndpoints.pushRegisterPath())
        assertEquals("/v1/notifications/unregister", ProtectedEndpoints.pushUnregisterPath())
        assertEquals("X-Client-Integrity", ProtectedEndpoints.headerClientIntegrity())
    }

    @Test
    fun startupConfigWithAttestationRequiredFetchesChallengeAndExchangesAttestedToken() = runTest {
        CloudSessionManager.overrideGatewayUrlForTesting("https://biblebible-api.example.run.app")
        var challengeRequested = false
        var capturedAttestationJson = ""

        CloudSessionManager.customAttesterForTesting = { nonce ->
            ClientAttestationDto(
                platform = "android",
                nonce = nonce,
                integrityToken = "signed-integrity-$nonce",
            )
        }

        val configDto = CloudRuntimeConfigDto(
            schemaVersion = 1,
            aiSummariesEnabled = true,
            sessionToken = "",
            tokenExpiresInSeconds = 0,
            attestationRequired = true,
        )

        val client = createHttpClient(
            AppHttpClientConfig(baseUrl = CloudSessionManager.gatewayHost),
            engine = MockEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/config" -> respond(
                        Json.encodeToString(configDto),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                    "/v1/auth/challenge" -> {
                        challengeRequested = true
                        respond(
                            """{"nonce":"nonce-abc-123","expiresInSeconds":120}""",
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                    "/v1/auth/token" -> {
                        assertEquals("ok", request.headers["X-Client-Integrity"])
                        capturedAttestationJson = request.body.toByteArray().decodeToString()
                        respond(
                            """{"sessionToken":"attested-jwt-999","tokenExpiresInSeconds":3600,"tier":"attested","attested":true}""",
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            },
            enableLogging = false,
        )

        try {
            CloudSessionManager.initializeStartupConfig(client)
            assertTrue(challengeRequested)
            assertTrue(CloudSessionManager.attestationRequired)
            assertEquals("attested-jwt-999", CloudSessionManager.sessionToken)

            val sent = Json.parseToJsonElement(capturedAttestationJson).jsonObject
            assertEquals("android", sent.getValue("platform").jsonPrimitive.content)
            assertEquals("nonce-abc-123", sent.getValue("nonce").jsonPrimitive.content)
            assertEquals("signed-integrity-nonce-abc-123", sent.getValue("integrityToken").jsonPrimitive.content)
        } finally {
            client.close()
        }
    }

    @Test
    fun structuredSummaryRequestOmitsFreeformPromptsAndInspectsRuntimeCleanly() {
        val serialized = Json.encodeToString(
            SummaryRequestDto(bookId = "PSA", chapter = 23, version = "KJV", model = "gemini-3.5-flash", refresh = true)
        )
        val json = Json.parseToJsonElement(serialized).jsonObject
        assertEquals("PSA", json.getValue("bookId").jsonPrimitive.content)
        assertEquals(23, json.getValue("chapter").jsonPrimitive.content.toInt())
        assertFalse(json.containsKey("prompt"))
        assertFalse(json.containsKey("contents"))

        val status = AppIntegrityProvider.inspectRuntimeEnvironment()
        assertFalse(status.compromised)
    }
}
