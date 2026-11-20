package data.inhouse

import data.config.CloudSessionManager
import data.httpClientCloudGateway
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class InHouseWordDto(
    val id: String,
    val title: String,
    val category: String = "Sermon",
    val keyPrinciple: String = "",
    val summary: String = "",
    val scriptureReference: String = "",
    val bookUsfm: String = "PSA",
    val chapter: Int = 1,
    val sourceType: String = "mp3",
    val sourceLabel: String = "",
    val sourceUrl: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: String = "",
)

@Serializable
data class InHouseWordsResponseDto(
    val categories: List<String> = listOf("All", "Sermon", "Bible Study", "Morning Prayer"),
    val total: Int = 0,
    val words: List<InHouseWordDto> = emptyList(),
)

object InHouseWordsRepository {
    val defaultCategories: List<String> = listOf("All", "Sermon", "Bible Study", "Morning Prayer")

    val fallbackWords: List<InHouseWordDto> = listOf(
        InHouseWordDto(
            id = "ihw_starter_peace_storm",
            title = "Walking in Peace Through Uncertainty",
            category = "Sermon",
            keyPrinciple = "True peace is not the absence of storms, but anchoring anxious thoughts in thankful prayer before God.",
            summary = "When pressure mounts, replacing spiraling worry with specific, grateful petitions invites God's peace to stand guard over both heart and mind.",
            scriptureReference = "Philippians 4:6-7",
            bookUsfm = "PHP",
            chapter = 4,
            sourceType = "youtube",
            sourceLabel = "Sunday Sermon",
            tags = listOf("Peace", "Prayer", "Trust"),
        ),
        InHouseWordDto(
            id = "ihw_starter_love_neighbor",
            title = "Practical Compassion in Everyday Life",
            category = "Bible Study",
            keyPrinciple = "Loving your neighbor moves beyond warm words into inconvenient, visible mercy for the person right in front of you.",
            summary = "Walking through the Good Samaritan reminds us that faithfulness is measured by crossed streets, shared burdens, and quiet generosity.",
            scriptureReference = "Luke 10:25-37",
            bookUsfm = "LUK",
            chapter = 10,
            sourceType = "youtube",
            sourceLabel = "Midweek Bible Study",
            tags = listOf("Compassion", "Neighbor", "Discipleship"),
        ),
        InHouseWordDto(
            id = "ihw_starter_morning_mercy",
            title = "Fresh Mercies Before the Day Begins",
            category = "Morning Prayer",
            keyPrinciple = "Yesterday's exhaustion never limits today's grace; every sunrise carries new strength from a faithful God.",
            summary = "Beginning the morning by recalling God's steadfast love re-centers our focus before work, deadlines, or distractions compete for our attention.",
            scriptureReference = "Lamentations 3:22-24",
            bookUsfm = "LAM",
            chapter = 3,
            sourceType = "mp3",
            sourceLabel = "Morning Prayer",
            tags = listOf("Mercy", "Morning", "Renewal"),
        ),
        InHouseWordDto(
            id = "ihw_starter_servant_heart",
            title = "Strength Found in Humble Service",
            category = "Sermon",
            keyPrinciple = "In God's kingdom, greatness is never seized through status; it is quietly lived by serving those who cannot repay you.",
            summary = "Reflecting on Christ's example of servant leadership calls our homes, workplaces, and church community to lead with humility rather than pride.",
            scriptureReference = "Mark 10:43-45",
            bookUsfm = "MRK",
            chapter = 10,
            sourceType = "band",
            sourceLabel = "Church Devotional",
            tags = listOf("Humility", "Service", "Leadership"),
        ),
    )

    fun filterByCategory(words: List<InHouseWordDto>, category: String): List<InHouseWordDto> {
        val norm = category.trim()
        if (norm.isBlank() || norm.equals("All", ignoreCase = true)) return words
        return words.filter { it.category.equals(norm, ignoreCase = true) }
    }

    suspend fun fetchWords(
        category: String = "All",
        client: HttpClient = httpClientCloudGateway,
    ): InHouseWordsResponseDto {
        val baseUrl = CloudSessionManager.configuredGatewayUrl
            .takeIf { CloudSessionManager.parseSecureGatewayUrl(it) != null }
        if (baseUrl == null) {
            val filtered = filterByCategory(fallbackWords, category)
            return InHouseWordsResponseDto(
                categories = defaultCategories,
                total = filtered.size,
                words = filtered,
            )
        }
        val endpoint = if ("://" in baseUrl) "$baseUrl/v1/inhouse/words" else "https://$baseUrl/v1/inhouse/words"
        return try {
            val response = withContext(Dispatchers.IO) {
                client.get {
                    expectSuccess = false
                    url(endpoint)
                    if (category.isNotBlank() && !category.equals("All", ignoreCase = true)) {
                        parameter("category", category)
                    }
                }
            }
            if (!response.status.isSuccess()) {
                Napier.w("In-House Words API returned HTTP ${response.status.value}; using fallback", tag = "InHouseWords")
                val filtered = filterByCategory(fallbackWords, category)
                return InHouseWordsResponseDto(defaultCategories, filtered.size, filtered)
            }
            val parsed = response.body<InHouseWordsResponseDto>()
            if (parsed.words.isEmpty() && category.equals("All", ignoreCase = true)) {
                InHouseWordsResponseDto(defaultCategories, fallbackWords.size, fallbackWords)
            } else {
                parsed
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("In-House Words fetch failed (${e::class.simpleName}); using fallback", tag = "InHouseWords")
            val filtered = filterByCategory(fallbackWords, category)
            InHouseWordsResponseDto(defaultCategories, filtered.size, filtered)
        }
    }
}
