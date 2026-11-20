package data.inhouse

import data.config.CloudFeatureFlagsDto
import data.config.CloudRuntimeConfigDto
import data.config.CloudSessionManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InHouseWordsRepositoryTest {

    @AfterTest
    fun tearDown() {
        CloudSessionManager.resetForTesting()
    }

    @Test
    fun filterByCategoryMatchesSermonBibleStudyAndMorningPrayer() {
        val all = InHouseWordsRepository.fallbackWords
        assertEquals(all.size, InHouseWordsRepository.filterByCategory(all, "All").size)

        val sermons = InHouseWordsRepository.filterByCategory(all, "Sermon")
        assertTrue(sermons.isNotEmpty())
        assertTrue(sermons.all { it.category == "Sermon" })

        val prayers = InHouseWordsRepository.filterByCategory(all, "Morning Prayer")
        assertEquals(1, prayers.size)
        assertEquals("LAM", prayers.first().bookUsfm)
    }

    @Test
    fun fetchWordsParsesLiveCloudPayloadAndRespectsFeatureFlag() = runTest {
        CloudSessionManager.overrideGatewayUrlForTesting("https://api.biblebible.test")
        CloudSessionManager.applyRuntimeConfig(
            CloudRuntimeConfigDto(
                features = CloudFeatureFlagsDto(
                    biblicalShortsEnabled = true,
                    verseOfTheDayEnabled = true,
                    inHouseWordsEnabled = false,
                )
            )
        )
        assertFalse(CloudSessionManager.inHouseWordsEnabled)

        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/v1/inhouse/words"))
            respond(
                content = """
                    {
                      "categories": ["All", "Sermon", "Bible Study", "Morning Prayer"],
                      "total": 1,
                      "words": [
                        {
                          "id": "ihw_live_001",
                          "title": "Anchored in Grace",
                          "category": "Sermon",
                          "keyPrinciple": "God's grace sustains us beyond our own strength.",
                          "summary": "From Sunday service on 2 Corinthians 12.",
                          "scriptureReference": "2 Corinthians 12:9",
                          "bookUsfm": "2CO",
                          "chapter": 12,
                          "sourceType": "youtube",
                          "sourceLabel": "YouTube • Sermon"
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = InHouseWordsRepository.fetchWords(category = "All", client = client)
        assertEquals(1, response.total)
        assertEquals("ihw_live_001", response.words.first().id)
        assertEquals("2CO", response.words.first().bookUsfm)
        assertEquals(12, response.words.first().chapter)
        client.close()
    }
}
