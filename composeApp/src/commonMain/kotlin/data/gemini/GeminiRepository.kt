package data.gemini

import data.GeminiModel
import data.TIMEOUT_LIMIT_GEMINI
import data.analytics.AnalyticsTracker
import data.config.CloudSessionManager
import data.httpClientCloudGateway
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import email.kevinphillips.biblebible.security.ProtectedEndpoints
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/** Default cloud model used for chapter summaries. */
internal const val GEMINI_MODEL = "gemini-3.5-flash"
internal const val GEMINI_SUMMARY_CACHE_MAX_ENTRIES = 40

private val geminiCacheMutex = Mutex()
private val geminiSummaryCache = LinkedHashMap<String, GeminiResponseDto>()

private fun summaryCacheKey(target: SummaryTarget, model: String): String =
    "${target.bookId.trim().uppercase()}:${target.chapter}:${target.version.trim().uppercase()}:${model.trim().lowercase()}"

internal fun clearGeminiSummaryCacheForTesting() {
    if (geminiCacheMutex.tryLock()) {
        try {
            geminiSummaryCache.clear()
        } finally {
            geminiCacheMutex.unlock()
        }
    }
}

internal suspend fun geminiSummaryCacheSizeForTesting(): Int =
    geminiCacheMutex.withLock { geminiSummaryCache.size }

private suspend fun getCachedSummary(key: String): GeminiResponseDto? = geminiCacheMutex.withLock {
    val hit = geminiSummaryCache.remove(key) ?: return@withLock null
    geminiSummaryCache[key] = hit
    hit
}

private suspend fun putCachedSummary(key: String, response: GeminiResponseDto) {
    geminiCacheMutex.withLock {
        geminiSummaryCache.remove(key)
        geminiSummaryCache[key] = response
        while (geminiSummaryCache.size > GEMINI_SUMMARY_CACHE_MAX_ENTRIES) {
            val eldestKey = geminiSummaryCache.keys.firstOrNull() ?: break
            geminiSummaryCache.remove(eldestKey)
        }
    }
}

internal suspend fun generateContent(
    target: SummaryTarget,
    client: HttpClient = httpClientCloudGateway,
    refresh: Boolean = false,
) {
    val activeModel = CloudSessionManager.geminiModel.ifBlank { GEMINI_MODEL }
    val cacheKey = summaryCacheKey(target, activeModel)
    if (!refresh) {
        getCachedSummary(cacheKey)?.let { cached ->
            AnalyticsTracker.trackAiSummary(
                "ai_summary_cache_hit",
                mapOf("model" to activeModel),
            )
            GeminiModel.updateGeminiData(cached)
            return
        }
    }

    if (client === httpClientCloudGateway && CloudSessionManager.sessionToken.isBlank()) {
        CloudSessionManager.initializeStartupConfig()
        if (CloudSessionManager.sessionToken.isBlank()) {
            CloudSessionManager.refreshSessionToken()
        }
    }
    val summaryPath = ProtectedEndpoints.geminiSummaryPath()
    val requestBody = SummaryRequestDto(
        bookId = target.bookId,
        chapter = target.chapter,
        version = target.version,
        model = activeModel,
        refresh = if (refresh) true else null,
    )
    GeminiModel.updateGeminiData(GeminiResponseDto())

    try {
        var response = withContext(Dispatchers.IO) {
            client.post {
                url(path = summaryPath)
                expectSuccess = false
                timeout {
                    requestTimeoutMillis = TIMEOUT_LIMIT_GEMINI
                    socketTimeoutMillis = TIMEOUT_LIMIT_GEMINI
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
        // Automatically refresh the short-lived session token once on 401 when using the default gateway client
        if (response.status == HttpStatusCode.Unauthorized && client === httpClientCloudGateway) {
            val refreshedToken = CloudSessionManager.refreshSessionToken()
            if (!refreshedToken.isNullOrBlank()) {
                response = withContext(Dispatchers.IO) {
                    client.post {
                        url(path = summaryPath)
                        expectSuccess = false
                        timeout {
                            requestTimeoutMillis = TIMEOUT_LIMIT_GEMINI
                            socketTimeoutMillis = TIMEOUT_LIMIT_GEMINI
                        }
                        header(HttpHeaders.Authorization, "Bearer $refreshedToken")
                        contentType(ContentType.Application.Json)
                        setBody(requestBody)
                    }
                }
            }
        }
        currentCoroutineContext().ensureActive()
        if (!response.status.isSuccess()) {
            Napier.e("Gemini request failed (HTTP ${response.status.value})", tag = "GeminiServiceImp")
            AnalyticsTracker.trackAiSummary(
                "ai_summary_failed",
                mapOf("httpStatus" to response.status.value.toString(), "model" to activeModel),
            )
            GeminiModel.updateGeminiError(when (response.status.value) {
                402 -> "AI summary unavailable: check Gemini prepaid credits in Google AI Studio."
                401, 403 -> "Gemini rejected the API key or its permissions. Check the key and API restrictions."
                429 -> "Gemini quota or rate limit reached. Check quota and billing, or try again later."
                else -> "Gemini could not load the summary (HTTP ${response.status.value}). Please try again later."
            })
            return
        }
        val responseText = response.body<GeminiResponseDto>()
        currentCoroutineContext().ensureActive()
        if (responseText.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text.isNullOrBlank()) {
            AnalyticsTracker.trackAiSummary(
                "ai_summary_failed",
                mapOf("reason" to "empty_candidates", "model" to activeModel),
            )
            GeminiModel.updateGeminiError("Gemini returned no summary. Please try again.")
        } else {
            putCachedSummary(cacheKey, responseText)
            AnalyticsTracker.trackAiSummary(
                "ai_summary_generated",
                mapOf("model" to activeModel, "refresh" to refresh.toString()),
            )
            GeminiModel.updateGeminiData(responseText)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        Napier.e("Gemini request failed (${e::class.simpleName})", tag = "GeminiServiceImp")
        AnalyticsTracker.trackAiSummary(
            "ai_summary_failed",
            mapOf("reason" to (e::class.simpleName ?: "exception"), "model" to activeModel),
        )
        GeminiModel.updateGeminiError("AI summary could not load. Check your connection and try again.")
    }
}

internal suspend fun checkAnimationLastCalled(): Boolean {
    var showAnimation = false
    try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
                val lastCalledQuery = database.bibleBibleDatabaseQueries.getLastCalled()
                val lastCalledTime = lastCalledQuery.executeAsOneOrNull()?.time ?: 0
                val currentTime = Clock.System.now().toEpochMilliseconds()

                // Check if animation was called more than a day ago
                showAnimation = currentTime - lastCalledTime > 24 * 60 * 60 * 1000
                if (showAnimation) {
                    database.bibleBibleDatabaseQueries.insertLastCalled(time = currentTime)
                }
            }
        }
    } catch (e: Exception) {
        Napier.e("Error in checkAnimationLastCalled: ${e.message}", tag = "HomeTopBar")
    }
    return showAnimation
}
