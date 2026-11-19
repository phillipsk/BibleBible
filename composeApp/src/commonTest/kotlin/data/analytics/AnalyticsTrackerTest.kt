package data.analytics

import data.AppHttpClientConfig
import data.config.CloudSessionManager
import data.createHttpClient
import data.shorts.defaultBiblicalShortsCatalog
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsTrackerTest {

    @AfterTest
    fun tearDown() = runTest {
        AnalyticsTracker.resetForTesting()
        CloudSessionManager.resetForTesting()
    }

    @Test
    fun recordsFullSweepEventsAndFlushesBatchToCloudGateway() = runTest {
        AnalyticsTracker.resetForTesting()
        AnalyticsTracker.remoteDispatchEnabled = false
        CloudSessionManager.overrideGatewayUrlForTesting("https://biblebible.example.run.app")

        val sampleShort = defaultBiblicalShortsCatalog("https://biblebible.example.run.app").first()

        AnalyticsTracker.track("app_launch", "lifecycle")
        AnalyticsTracker.trackScreenView("home")
        AnalyticsTracker.trackBookSelected("GEN", "Genesis", source = "home_grid")
        AnalyticsTracker.trackChapterLoaded(bookId = 1, chapter = 6, version = "KJV", source = "remote")
        AnalyticsTracker.trackShortViewed(sampleShort)
        AnalyticsTracker.trackTranslationChanged("KJV", "BSB")
        AnalyticsTracker.trackFontSizeChanged(24f, source = "slider")
        AnalyticsTracker.trackAiSummary("ai_summary_generated", mapOf("model" to "gemini-3.5-flash"))

        val recorded = AnalyticsTracker.snapshotEvents()
        assertEquals(8, recorded.size)
        assertEquals(
            listOf(
                "app_launch",
                "screen_view",
                "book_selected",
                "chapter_loaded",
                "short_viewed",
                "translation_changed",
                "font_size_changed",
                "ai_summary_generated",
            ),
            recorded.map { it.name },
        )
        assertTrue(recorded.all { it.sessionId == AnalyticsTracker.sessionId })

        var capturedPayload = ""
        val mockClient = createHttpClient(
            AppHttpClientConfig(baseUrl = CloudSessionManager.gatewayHost),
            engine = MockEngine { request ->
                assertEquals("/v1/analytics/events", request.url.encodedPath)
                capturedPayload = request.body.toByteArray().decodeToString()
                respond(
                    """{"status":"accepted","ingested":8,"totalEvents":8}""",
                    HttpStatusCode.Accepted,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )

        try {
            val flushedCount = AnalyticsTracker.flushNow(mockClient)
            assertEquals(8, flushedCount)
            val decoded = Json.decodeFromString<AnalyticsBatchRequestDto>(capturedPayload)
            assertEquals(8, decoded.events.size)
            assertEquals("Genesis", decoded.events[2].properties["bookName"])
            assertEquals("short-01-creation-light", decoded.events[4].properties["shortId"])
            // Queue should now be empty on second flush
            assertEquals(0, AnalyticsTracker.flushNow(mockClient))
        } finally {
            mockClient.close()
        }
    }
}
