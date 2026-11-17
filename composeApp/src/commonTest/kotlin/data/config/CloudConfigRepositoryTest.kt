package data.config

import JSON_VERSIONS
import data.AppHttpClientConfig
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.BibleIQVersion
import data.bibleIQ.BibleIQVersions
import data.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloudConfigRepositoryTest {

    @AfterTest
    fun resetState() {
        CloudSessionManager.resetForTesting()
        BibleIQDataModel.updateBibleVersions(BibleIQVersions())
    }

    @Test
    fun bundledLeanVersionsMatchSelectedSixTranslations() {
        val versions = Json.decodeFromString<List<BibleIQVersion>>(JSON_VERSIONS)
        assertEquals(
            listOf("KJV", "BSB", "WEB", "BBE", "RV1909", "SVD"),
            versions.map { it.abbreviation }
        )
        assertEquals("BSB", versions.first { it.abbreviation == "BSB" }.uiAbbreviation)
        assertEquals("WEB", versions.first { it.abbreviation == "WEB" }.uiAbbreviation)
        assertEquals("BBE", versions.first { it.abbreviation == "BBE" }.uiAbbreviation)
        assertEquals("Spanish: RV1909", versions.first { it.abbreviation == "RV1909" }.uiAbbreviation)
        assertEquals("Arabic: SVD", versions.first { it.abbreviation == "SVD" }.uiAbbreviation)
    }

    @Test
    fun initializeStartupConfigLoadsSessionTokenAndDynamicSettings() = runTest {
        CloudSessionManager.overrideGatewayUrlForTesting("https://biblebible-api.example.run.app")
        val expectedVersions = Json.decodeFromString<List<BibleIQVersion>>(JSON_VERSIONS)
        val payload = CloudRuntimeConfigDto(
            schemaVersion = 1,
            aiSummariesEnabled = true,
            geminiModel = "gemini-3.5-flash",
            defaultVersion = "KJV",
            supportedVersions = expectedVersions,
            sessionToken = "short-lived-jwt-token-123",
            tokenExpiresInSeconds = 3600L,
        )

        val client = createHttpClient(
            AppHttpClientConfig(baseUrl = CloudSessionManager.gatewayHost),
            engine = MockEngine { request ->
                assertEquals("/v1/config", request.url.encodedPath)
                respond(
                    Json.encodeToString(payload),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        )

        try {
            val loaded = CloudSessionManager.initializeStartupConfig(client)
            assertNotNull(loaded)
            assertEquals("short-lived-jwt-token-123", CloudSessionManager.sessionToken)
            assertEquals("gemini-3.5-flash", CloudSessionManager.geminiModel)
            assertTrue(CloudSessionManager.aiSummariesEnabled)
            assertEquals(6, BibleIQDataModel.bibleVersions.data.size)
            assertFalse(loaded.toString().contains("short-lived-jwt-token-123"))
        } finally {
            client.close()
        }
    }

    @Test
    fun refreshSessionTokenUpdatesTokenFromAuthEndpoint() = runTest {
        CloudSessionManager.overrideGatewayUrlForTesting("https://biblebible-api.example.run.app")
        val client = createHttpClient(
            AppHttpClientConfig(baseUrl = CloudSessionManager.gatewayHost),
            engine = MockEngine { request ->
                assertEquals("/v1/auth/token", request.url.encodedPath)
                respond(
                    """{"sessionToken":"refreshed-jwt-token-456","tokenExpiresInSeconds":3600}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        )

        try {
            val refreshed = CloudSessionManager.refreshSessionToken(client)
            assertEquals("refreshed-jwt-token-456", refreshed)
            assertEquals("refreshed-jwt-token-456", CloudSessionManager.sessionToken)
        } finally {
            client.close()
        }
    }

    @Test
    fun insecureHttpGatewayUrlsAreRejected() {
        assertNull(CloudSessionManager.parseSecureGatewayUrl("http://insecure.example.com"))
        assertNull(CloudSessionManager.parseSecureGatewayUrl("https://user:pass@example.com"))
        assertNotNull(CloudSessionManager.parseSecureGatewayUrl("https://biblebible-api.example.run.app"))
    }
}
