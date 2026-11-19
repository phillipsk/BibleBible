package data.analytics

import data.config.CloudSessionManager
import data.httpClientCloudGateway
import data.shorts.BiblicalShortDto
import email.kevinphillips.biblebible.getPlatformName
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
internal data class AnalyticsEventDto(
    val name: String,
    val category: String,
    val platform: String,
    val sessionId: String,
    val timestamp: String,
    val properties: Map<String, String> = emptyMap(),
)

@Serializable
internal data class AnalyticsBatchRequestDto(
    val events: List<AnalyticsEventDto>,
)

@Serializable
internal data class AnalyticsAckResponseDto(
    val status: String = "",
    val ingested: Int = 0,
    val totalEvents: Long = 0,
)

/**
 * Thread-safe first-party KMP telemetry collector. Buffers events in a lock-free
 * channel and batches uploads to the Google Cloud Run gateway `/v1/analytics/events`.
 */
internal object AnalyticsTracker {
    private val platformName: String = getPlatformName()
    private val sessionIdValue: String =
        "$platformName-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(1000, 9999)}"

    private val mutex = Mutex()
    private val eventChannel = Channel<AnalyticsEventDto>(capacity = 512)
    private val pendingQueue = mutableListOf<AnalyticsEventDto>()
    private val history = mutableListOf<AnalyticsEventDto>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    internal var remoteDispatchEnabled: Boolean = true
    private var workerStarted: Boolean = false

    val sessionId: String get() = sessionIdValue

    private fun ensureWorkerStarted() {
        if (workerStarted || !remoteDispatchEnabled) return
        workerStarted = true
        scope.launch {
            while (true) {
                val first = eventChannel.receive()
                val batch = mutableListOf(first)
                // Brief coalescing window so rapid taps batch into a single HTTP request
                delay(750L)
                while (batch.size < 25) {
                    val next = eventChannel.tryReceive().getOrNull() ?: break
                    batch.add(next)
                }
                mutex.withLock {
                    recordIntoBuffersLocked(batch)
                }
                if (remoteDispatchEnabled && CloudSessionManager.isGatewayConfigured) {
                    flushNow(httpClientCloudGateway)
                }
            }
        }
    }

    private fun recordIntoBuffersLocked(events: List<AnalyticsEventDto>) {
        if (events.isEmpty()) return
        history.addAll(events)
        if (history.size > 500) {
            history.subList(0, history.size - 500).clear()
        }
        pendingQueue.addAll(events)
        if (pendingQueue.size > 500) {
            pendingQueue.subList(0, pendingQueue.size - 500).clear()
        }
    }

    private fun drainChannelLocked() {
        val drained = mutableListOf<AnalyticsEventDto>()
        while (true) {
            val next = eventChannel.tryReceive().getOrNull() ?: break
            drained.add(next)
        }
        recordIntoBuffersLocked(drained)
    }

    fun track(
        name: String,
        category: String,
        properties: Map<String, String> = emptyMap(),
    ) {
        val cleanName = name.trim().lowercase()
        if (cleanName.isEmpty()) return
        val event = AnalyticsEventDto(
            name = cleanName,
            category = category.trim().lowercase().ifEmpty { "general" },
            platform = platformName,
            sessionId = sessionIdValue,
            timestamp = Clock.System.now().toString(),
            properties = properties,
        )
        ensureWorkerStarted()
        eventChannel.trySend(event)
    }

    fun trackScreenView(screen: String, properties: Map<String, String> = emptyMap()) {
        track(
            name = "screen_view",
            category = "navigation",
            properties = mapOf("screen" to screen) + properties,
        )
    }

    fun trackBookSelected(bookId: String, bookName: String, source: String) {
        track(
            name = "book_selected",
            category = "reading",
            properties = mapOf(
                "bookId" to bookId,
                "bookName" to bookName,
                "source" to source,
            ),
        )
    }

    fun trackChapterLoaded(
        bookId: Int,
        chapter: Int,
        version: String,
        source: String,
    ) {
        track(
            name = "chapter_loaded",
            category = "reading",
            properties = mapOf(
                "bookId" to bookId.toString(),
                "chapter" to chapter.toString(),
                "version" to version.uppercase(),
                "source" to source,
            ),
        )
    }

    fun trackShortViewed(shortItem: BiblicalShortDto) {
        track(
            name = "short_viewed",
            category = "biblical_shorts",
            properties = mapOf(
                "shortId" to shortItem.id,
                "order" to shortItem.order.toString(),
                "title" to shortItem.title,
                "category" to shortItem.category,
                "bookUsfm" to shortItem.bookUsfm,
                "chapter" to shortItem.chapter.toString(),
            ),
        )
    }

    fun trackTranslationChanged(fromVersion: String, toVersion: String) {
        track(
            name = "translation_changed",
            category = "preferences",
            properties = mapOf(
                "fromVersion" to fromVersion.uppercase(),
                "toVersion" to toVersion.uppercase(),
            ),
        )
    }

    fun trackFontSizeChanged(fontSize: Float, source: String) {
        track(
            name = "font_size_changed",
            category = "preferences",
            properties = mapOf(
                "fontSize" to fontSize.toInt().toString(),
                "source" to source,
            ),
        )
    }

    fun trackAiSummary(action: String, properties: Map<String, String> = emptyMap()) {
        track(
            name = action,
            category = "ai_summary",
            properties = properties,
        )
    }

    fun trackError(source: String, message: String) {
        track(
            name = "app_error",
            category = "error",
            properties = mapOf(
                "source" to source,
                "message" to message.take(160),
            ),
        )
    }

    suspend fun snapshotEvents(): List<AnalyticsEventDto> = mutex.withLock {
        drainChannelLocked()
        history.toList()
    }

    suspend fun flushNow(client: HttpClient = httpClientCloudGateway): Int {
        val batch = mutex.withLock {
            drainChannelLocked()
            if (pendingQueue.isEmpty()) return 0
            pendingQueue.toList()
        }
        val baseUrl = CloudSessionManager.configuredGatewayUrl
            .takeIf { CloudSessionManager.parseSecureGatewayUrl(it) != null }
            ?: return 0
        val endpoint = if ("://" in baseUrl) "$baseUrl/v1/analytics/events" else "https://$baseUrl/v1/analytics/events"

        return try {
            val response = withContext(Dispatchers.IO) {
                client.post {
                    expectSuccess = false
                    url(endpoint)
                    header("X-Client-Platform", platformName)
                    contentType(ContentType.Application.Json)
                    setBody(AnalyticsBatchRequestDto(batch))
                }
            }
            if (response.status.isSuccess()) {
                mutex.withLock {
                    val removeCount = batch.size.coerceAtMost(pendingQueue.size)
                    repeat(removeCount) { pendingQueue.removeAt(0) }
                }
                batch.size
            } else {
                0
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.d("Analytics flush skipped (${e::class.simpleName})", tag = "Analytics")
            0
        }
    }

    suspend fun resetForTesting() {
        mutex.withLock {
            while (eventChannel.tryReceive().isSuccess) {
                // drain channel
            }
            pendingQueue.clear()
            history.clear()
            remoteDispatchEnabled = true
        }
    }
}
