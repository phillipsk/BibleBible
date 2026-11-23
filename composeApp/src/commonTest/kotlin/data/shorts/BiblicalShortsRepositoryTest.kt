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
    fun defaultCatalogContainsCuratedSubThirtySecondShortsWithValidCategories() {
        val catalog = defaultBiblicalShortsCatalog("https://biblebible.example.run.app")
        assertEquals(24, catalog.size)
        assertEquals(24, catalog.map { it.id }.toSet().size)

        val validCategories = biblicalShortsCategories.filter { it != "All" }.toSet()
        catalog.forEachIndexed { idx, item ->
            assertEquals(idx + 1, item.order)
            assertTrue(item.durationSeconds in 15..29, "Duration out of range for ${item.id}")
            assertTrue(item.bodyDurationSeconds != null && item.bodyDurationSeconds > 0.0, "Missing bodyDurationSeconds for ${item.id}")
            assertEquals(2.5, item.promoEndCardSeconds)
            assertTrue(item.bookId in 1..66, "Book ID out of range for ${item.id}")
            assertTrue(item.chapter >= 1 && item.verse >= 1)
            assertTrue(item.lesson.isNotBlank())
            assertTrue(item.narrative.isNotBlank())
            assertTrue(item.scriptureReference.isNotBlank())
            assertTrue(item.category in validCategories, "Category ${item.category} missing for ${item.id}")
            assertEquals(3, item.scenes.size)
            assertTrue(
                item.videoUrl.startsWith("https://"),
                "Expected HTTPS GCS or gateway URL for ${item.id}, got ${item.videoUrl}",
            )
        }

        // Every displayed category chip has at least one matching Bible clip
        val repo = BiblicalShortsRepository()
        validCategories.forEach { category ->
            assertTrue(
                repo.filterByCategory(catalog, category).isNotEmpty(),
                "Category $category must map to at least one clip",
            )
        }
    }

    @Test
    fun repositoryFetchesFromGatewayCachesAndFiltersByCategory() = runTest {
        CloudSessionManager.overrideGatewayUrlForTesting("https://biblebible.example.run.app")
        val sampleShorts = defaultBiblicalShortsCatalog("https://biblebible.example.run.app")
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
            assertEquals(24, first.size)
            assertEquals(24, second.size)
            assertEquals(1, networkCalls)

            val faithOnly = repo.filterByCategory(first, "Faith & Courage")
            assertEquals(6, faithOnly.size)
            assertTrue(faithOnly.all { it.category == "Faith & Courage" })
            assertEquals(24, repo.filterByCategory(first, "All").size)
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
        assertEquals(24, shorts.size)
        assertEquals("01-col3-bond-that-holds", shorts.first().id)
    }
}
