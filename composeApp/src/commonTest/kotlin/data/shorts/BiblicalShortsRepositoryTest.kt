package data.shorts

import data.AppHttpClientConfig
import data.config.CloudSessionManager
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
import kotlin.test.assertTrue

class BiblicalShortsRepositoryTest {

    @AfterTest
    fun resetGateway() {
        CloudSessionManager.resetForTesting()
    }

    @Test
    fun defaultCatalogContainsFiftyValidSubThirtySecondShorts() {
        val catalog = defaultBiblicalShortsCatalog("https://biblebible.example.run.app")
        assertEquals(50, catalog.size)
        assertEquals(50, catalog.map { it.id }.toSet().size)

        catalog.forEachIndexed { idx, item ->
            assertEquals(idx + 1, item.order)
            assertTrue(item.durationSeconds in 15..29, "Duration out of range for ${item.id}")
            assertTrue(item.bookId in 1..66, "Book ID out of range for ${item.id}")
            assertTrue(item.chapter >= 1 && item.verse >= 1)
            assertTrue(item.lesson.isNotBlank())
            assertTrue(item.narrative.isNotBlank())
            assertTrue(item.scriptureReference.isNotBlank())
            assertEquals(3, item.scenes.size)
            assertEquals(
                "https://biblebible.example.run.app/v1/shorts/media/${item.id}.mp4",
                item.videoUrl,
            )
        }
    }

    @Test
    fun repositoryFetchesFromGatewayCachesAndFiltersByCategory() = runTest {
        CloudSessionManager.overrideGatewayUrlForTesting("https://biblebible.example.run.app")
        val sampleShorts = defaultBiblicalShortsCatalog("https://biblebible.example.run.app").take(4)
        var networkCalls = 0
        val client = createHttpClient(
            AppHttpClientConfig(baseUrl = CloudSessionManager.gatewayHost),
            engine = MockEngine { request ->
                networkCalls++
                assertEquals("/GetBiblicalShorts", request.url.encodedPath)
                respond(
                    Json.encodeToString(
                        BiblicalShortsFeedDto(
                            schemaVersion = 1,
                            totalCount = sampleShorts.size,
                            maxDurationSeconds = 30,
                            categories = biblicalShortsCategories.drop(1),
                            shorts = sampleShorts,
                        )
                    ),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )

        try {
            val repo = BiblicalShortsRepository(
                fetch = { fetchBiblicalShortsFeed(client) },
            )
            val first = repo.getShorts()
            val second = repo.getShorts()
            assertEquals(4, first.size)
            assertEquals(4, second.size)
            assertEquals(1, networkCalls)

            val faithOnly = repo.filterByCategory(first, "Faith & Courage")
            assertEquals(2, faithOnly.size)
            assertTrue(faithOnly.all { it.category == "Faith & Courage" })
            assertEquals(4, repo.filterByCategory(first, "All").size)
        } finally {
            client.close()
        }
    }

    @Test
    fun repositoryFallsBackToOfflineCatalogOnNetworkFailure() = runTest {
        val repo = BiblicalShortsRepository(
            fetch = { throw IllegalStateException("Offline") },
            fallbackProvider = { defaultBiblicalShortsCatalog("") },
        )
        val shorts = repo.getShorts()
        assertEquals(50, shorts.size)
        assertEquals("short-01-creation-light", shorts.first().id)
    }
}
