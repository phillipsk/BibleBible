package data.search

import data.ai.OnDeviceAiAssistant
import data.bible.BibleState
import data.bible.BookData
import data.config.CloudSessionManager
import data.httpClientCloudGateway
import email.kevinphillips.biblebible.security.ProtectedEndpoints
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

internal object SearchRepository {
    fun searchImmediateLocal(
        query: String,
        availableBooks: List<BookData> = BibleState.uiBooks.data.orEmpty(),
        includeMasonry: Boolean = CloudSessionManager.searchMasonryTopicsEnabled,
    ): SearchResultUiState {
        val clean = query.trim()
        if (clean.isEmpty()) {
            return SearchResultUiState(query = "", aiSource = "none")
        }
        val directRef = BundledTopicIndex.parseDirectReference(clean)
        val books = BundledTopicIndex.matchBooks(clean, availableBooks)
        val (topics, cards) = BundledTopicIndex.searchLocalTopics(
            query = clean,
            includeMasonry = includeMasonry,
        )
        return SearchResultUiState(
            query = clean,
            aiSource = "deterministic",
            mappedTopics = topics,
            directReference = directRef,
            books = books,
            masonryCards = cards,
        )
    }

    suspend fun searchEnriched(
        query: String,
        versionId: String = BibleState.selectedVersion,
        availableBooks: List<BookData> = BibleState.uiBooks.data.orEmpty(),
        client: HttpClient = httpClientCloudGateway,
        onDeviceAiEnabled: Boolean = CloudSessionManager.onDeviceAiEnabled,
        cloudFallbackEnabled: Boolean = CloudSessionManager.cloudSearchAiFallbackEnabled,
        includeMasonry: Boolean = CloudSessionManager.searchMasonryTopicsEnabled,
    ): SearchResultUiState {
        val clean = query.trim()
        if (clean.isEmpty()) return SearchResultUiState(query = "", aiSource = "none")

        val baseLocal = searchImmediateLocal(
            query = clean,
            availableBooks = availableBooks,
            includeMasonry = includeMasonry,
        )

        var intentTopics = emptyList<String>()
        var aiProviderLabel = ""
        val wordCount = clean.split(Regex("\\s+")).count { it.isNotBlank() }

        // Tier 1: Progressive On-Device AI intent extraction for multi-word queries when deterministic topics are empty
        if (
            onDeviceAiEnabled &&
            includeMasonry &&
            baseLocal.directReference == null &&
            baseLocal.masonryCards.isEmpty() &&
            wordCount >= 2 &&
            OnDeviceAiAssistant.isAvailable()
        ) {
            val extracted = OnDeviceAiAssistant.extractTopicIntent(clean, BundledTopicIndex.topicNames)
            if (extracted != null && extracted.topics.isNotEmpty()) {
                intentTopics = extracted.topics
                aiProviderLabel = extracted.provider
            }
        }

        val localWithIntent = if (intentTopics.isNotEmpty()) {
            val (mapped, cards) = BundledTopicIndex.searchLocalTopics(
                query = clean,
                explicitTopics = intentTopics,
                includeMasonry = includeMasonry,
            )
            baseLocal.copy(
                aiSource = "on_device",
                aiProviderLabel = aiProviderLabel,
                mappedTopics = mapped,
                masonryCards = cards,
            )
        } else {
            baseLocal
        }

        // Tier 2 / Tier 3: Query GCP Cloud Run /v1/search if gateway is configured
        if (!CloudSessionManager.isGatewayConfigured) {
            return localWithIntent
        }

        val shouldConsultCloud =
            (includeMasonry && (localWithIntent.masonryCards.isEmpty() || wordCount >= 2 || cloudFallbackEnabled)) ||
                localWithIntent.books.isEmpty()

        if (!shouldConsultCloud) {
            return localWithIntent
        }

        val cloudDto = fetchCloudSearch(
            query = clean,
            versionId = versionId,
            intentTopics = intentTopics,
            client = client,
        ) ?: return localWithIntent

        val resolvedCards = if (includeMasonry) {
            cloudDto.masonryCards.ifEmpty { localWithIntent.masonryCards }
        } else {
            emptyList()
        }
        val resolvedSource = when {
            intentTopics.isNotEmpty() -> "on_device"
            cloudDto.aiSource.isNotBlank() -> cloudDto.aiSource
            else -> localWithIntent.aiSource
        }
        return SearchResultUiState(
            query = clean,
            aiSource = resolvedSource,
            aiProviderLabel = if (resolvedSource == "on_device") {
                aiProviderLabel.ifBlank { OnDeviceAiAssistant.providerLabel() }
            } else {
                ""
            },
            mappedTopics = cloudDto.mappedTopics.ifEmpty { localWithIntent.mappedTopics },
            directReference = cloudDto.directReference ?: localWithIntent.directReference,
            books = cloudDto.books.ifEmpty { localWithIntent.books },
            masonryCards = resolvedCards,
            isLoadingCloud = false,
        )
    }

    internal suspend fun fetchCloudSearch(
        query: String,
        versionId: String,
        intentTopics: List<String>,
        client: HttpClient = httpClientCloudGateway,
    ): CloudSearchResponseDto? {
        val baseUrl = CloudSessionManager.configuredGatewayUrl
            .takeIf { CloudSessionManager.parseSecureGatewayUrl(it) != null }
            ?: return null
        val prefix = if ("://" in baseUrl) baseUrl else "https://$baseUrl"
        val endpoint = "$prefix${ProtectedEndpoints.searchPath()}"
        return try {
            val response = withContext(Dispatchers.IO) {
                client.get {
                    expectSuccess = false
                    url(endpoint)
                    parameter("q", query)
                    parameter("versionId", versionId)
                    if (intentTopics.isNotEmpty()) {
                        parameter("intentTopics", intentTopics.joinToString(","))
                    }
                }
            }
            if (!response.status.isSuccess()) return null
            response.body<CloudSearchResponseDto>()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
