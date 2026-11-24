package data.config

import data.AppHttpClientConfig
import data.bible.BibleState
import data.bible.BibleVersion
import data.bible.BibleVersions
import data.bible.json.BUNDLED_BIBLE_VERSIONS_JSON
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
        BibleState.updateBibleVersions(BibleVersions())
    }

    @Test
    fun bundledLeanVersionsMatchSelectedSixTranslations() {
        val versions = Json.decodeFromString<List<BibleVersion>>(BUNDLED_BIBLE_VERSIONS_JSON)
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
        val expectedVersions = Json.decodeFromString<List<BibleVersion>>(BUNDLED_BIBLE_VERSIONS_JSON)
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
            assertEquals(6, BibleState.bibleVersions.data.size)
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

    @Test
    fun featureFlagsToggleBiblicalShortsAndVerseOfTheDay() = runTest {
        assertTrue(CloudSessionManager.biblicalShortsEnabled)
        assertTrue(CloudSessionManager.verseOfTheDayEnabled)
        assertEquals("soft_gold_sheen", CloudSessionManager.bookShimmerStyle)
        assertEquals(1.0f, CloudSessionManager.bookShimmerPace)
        assertEquals(1.0f, CloudSessionManager.bookShimmerIntensity)
        assertEquals(10, CloudSessionManager.bookSectionPaddingDp)
        assertEquals("split_testaments", CloudSessionManager.bookGridTabletLayout)
        assertEquals(setOf("JOS", "JOB", "DAN", "MAL", "ROM", "PHM"), CloudSessionManager.bookSectionBreaksMobile)
        assertEquals(setOf("MAL"), CloudSessionManager.bookSectionBreaksTablet)
        assertEquals("literary_serif", CloudSessionManager.readerLayoutStyle)
        assertEquals(1.68f, CloudSessionManager.readerLineHeightMultiplier)
        assertEquals(14, CloudSessionManager.readerVerseGapDp)
        assertEquals(22.0f, CloudSessionManager.readerDefaultFontSizeSp)
        assertTrue(CloudSessionManager.readerRedLetterEnabled)
        assertTrue(CloudSessionManager.elasticSearchEnabled)
        assertTrue(CloudSessionManager.searchMasonryTopicsEnabled)
        assertTrue(CloudSessionManager.readingPlansEnabled)
        assertTrue(CloudSessionManager.readingPlansAutoCheckoffEnabled)
        assertTrue(CloudSessionManager.onDeviceAiEnabled)
        assertTrue(CloudSessionManager.cloudSearchAiFallbackEnabled)
        assertEquals("adaptive", CloudSessionManager.homePlansLayout)

        CloudSessionManager.applyRuntimeConfig(
            CloudRuntimeConfigDto(
                features = CloudFeatureFlagsDto(
                    biblicalShortsEnabled = false,
                    verseOfTheDayEnabled = false,
                    bookShimmerStyle = "ROW_WAVE",
                    bookShimmerPace = 0.65f,
                    bookShimmerIntensity = 1.35f,
                    bookSectionPaddingDp = 16,
                    bookGridTabletLayout = "TESTAMENT_DIVIDER",
                    bookSectionBreaksMobile = "jos, mal",
                    bookSectionBreaksTablet = "jos,mal",
                    readerLayoutStyle = "VERSE_PER_LINE",
                    readerLineHeightMultiplier = 1.85f,
                    readerVerseGapDp = 18,
                    readerDefaultFontSizeSp = 24f,
                    readerRedLetterEnabled = false,
                    elasticSearchEnabled = false,
                    searchMasonryTopicsEnabled = false,
                    readingPlansEnabled = false,
                    readingPlansAutoCheckoffEnabled = false,
                    onDeviceAiEnabled = false,
                    cloudSearchAiFallbackEnabled = false,
                    homePlansLayout = "TWIN_CARDS",
                )
            )
        )
        assertFalse(CloudSessionManager.biblicalShortsEnabled)
        assertFalse(CloudSessionManager.verseOfTheDayEnabled)
        assertEquals("row_wave", CloudSessionManager.bookShimmerStyle)
        assertEquals(0.65f, CloudSessionManager.bookShimmerPace)
        assertEquals(1.35f, CloudSessionManager.bookShimmerIntensity)
        assertEquals(16, CloudSessionManager.bookSectionPaddingDp)
        assertEquals("testament_divider", CloudSessionManager.bookGridTabletLayout)
        assertEquals(setOf("JOS", "MAL"), CloudSessionManager.bookSectionBreaksMobile)
        assertEquals(setOf("JOS", "MAL"), CloudSessionManager.bookSectionBreaksTablet)
        assertEquals("verse_per_line", CloudSessionManager.readerLayoutStyle)
        assertEquals(1.85f, CloudSessionManager.readerLineHeightMultiplier)
        assertEquals(18, CloudSessionManager.readerVerseGapDp)
        assertEquals(24f, CloudSessionManager.readerDefaultFontSizeSp)
        assertFalse(CloudSessionManager.readerRedLetterEnabled)
        assertFalse(CloudSessionManager.elasticSearchEnabled)
        assertFalse(CloudSessionManager.searchMasonryTopicsEnabled)
        assertFalse(CloudSessionManager.readingPlansEnabled)
        assertFalse(CloudSessionManager.readingPlansAutoCheckoffEnabled)
        assertFalse(CloudSessionManager.onDeviceAiEnabled)
        assertFalse(CloudSessionManager.cloudSearchAiFallbackEnabled)
        assertEquals("twin_cards", CloudSessionManager.homePlansLayout)

        CloudSessionManager.applyRuntimeConfig(
            CloudRuntimeConfigDto(
                features = CloudFeatureFlagsDto(
                    bookShimmerStyle = "unknown_style",
                    bookShimmerPace = 10f,
                    bookShimmerIntensity = -1f,
                    bookSectionPaddingDp = 200,
                    bookGridTabletLayout = "invalid",
                    bookSectionBreaksMobile = "",
                    readerLayoutStyle = "unknown",
                    readerLineHeightMultiplier = 9f,
                    readerVerseGapDp = -5,
                    readerDefaultFontSizeSp = 5f,
                    homePlansLayout = "invalid_layout",
                )
            )
        )
        assertEquals("soft_gold_sheen", CloudSessionManager.bookShimmerStyle)
        assertEquals(3.0f, CloudSessionManager.bookShimmerPace)
        assertEquals(0f, CloudSessionManager.bookShimmerIntensity)
        assertEquals(48, CloudSessionManager.bookSectionPaddingDp)
        assertEquals("split_testaments", CloudSessionManager.bookGridTabletLayout)
        assertEquals(setOf("JOS", "JOB", "DAN", "MAL", "ROM", "PHM"), CloudSessionManager.bookSectionBreaksMobile)
        assertEquals("literary_serif", CloudSessionManager.readerLayoutStyle)
        assertEquals(2.2f, CloudSessionManager.readerLineHeightMultiplier)
        assertEquals(0, CloudSessionManager.readerVerseGapDp)
        assertEquals(14f, CloudSessionManager.readerDefaultFontSizeSp)
        assertEquals("adaptive", CloudSessionManager.homePlansLayout)
    }
}
