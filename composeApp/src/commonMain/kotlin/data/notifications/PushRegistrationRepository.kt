package data.notifications

import data.analytics.AnalyticsTracker
import data.bible.currentTimeMillis
import data.config.CloudSessionManager
import data.httpClientCloudGateway
import email.kevinphillips.biblebible.security.ProtectedEndpoints
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
internal data class PushDeviceRegistrationRequest(
    val token: String,
    val deviceId: String,
    val platform: String,
    val appBuild: String,
    val versionId: String,
    val timezone: String,
    val reminderHour: Int,
    val reminderMinute: Int,
    val remindersEnabled: Boolean,
    val topics: List<String>,
)

@Serializable
internal data class PushDeviceUnregisterRequest(
    val token: String = "",
    val deviceId: String = "",
)

internal object NotificationDeliveryPolicy {
    /**
     * Only schedule the local daily Verse of the Day fallback when reminders are enabled
     * and the device does not yet hold an active remote push token, preventing duplicate
     * local + server 6:00 AM notifications.
     */
    fun shouldScheduleLocalDailyVotd(
        remindersEnabled: Boolean,
        hasRemotePushToken: Boolean,
    ): Boolean = remindersEnabled && !hasRemotePushToken
}

internal object PushRegistrationRepository {
    private val baseTopics = listOf(
        "votd_daily",
        "announcements",
        "biblical_words",
        "reading_engagement",
    )

    private val registrationMutex = Mutex()
    private var lastSuccessfulFingerprint: String? = null
    private var lastRegisteredAtMs: Long = 0L

    internal var dedupeWindowMs: Long = 5L * 60L * 1000L
    internal var nowProvider: () -> Long = { currentTimeMillis() }

    fun defaultTopics(isDebugBuild: Boolean): List<String> =
        if (isDebugBuild) baseTopics + "debug_test" else baseTopics

    fun allKnownTopics(): List<String> = baseTopics + "debug_test"

    internal fun resetForTesting() {
        lastSuccessfulFingerprint = null
        lastRegisteredAtMs = 0L
        dedupeWindowMs = 5L * 60L * 1000L
        nowProvider = { currentTimeMillis() }
    }

    suspend fun registerDeviceToken(
        token: String,
        platform: String,
        isDebugBuild: Boolean,
        timezone: String,
        reminderHour: Int,
        reminderMinute: Int,
        remindersEnabled: Boolean,
        versionId: String,
        deviceId: String = AnalyticsTracker.deviceId,
        client: HttpClient = httpClientCloudGateway,
    ): Boolean {
        val cleanToken = token.trim()
        if (cleanToken.length < 8) return false
        val baseUrl = CloudSessionManager.configuredGatewayUrl
            .takeIf { CloudSessionManager.parseSecureGatewayUrl(it) != null }
            ?: return false
        val path = ProtectedEndpoints.pushRegisterPath()
        val endpoint = if ("://" in baseUrl) "$baseUrl$path" else "https://$baseUrl$path"
        val request = PushDeviceRegistrationRequest(
            token = cleanToken,
            deviceId = deviceId,
            platform = platform.lowercase(),
            appBuild = if (isDebugBuild) "debug" else "release",
            versionId = versionId.uppercase().ifBlank { "KJV" },
            timezone = timezone.ifBlank { "America/New_York" },
            reminderHour = reminderHour.coerceIn(0, 23),
            reminderMinute = reminderMinute.coerceIn(0, 59),
            remindersEnabled = remindersEnabled,
            topics = defaultTopics(isDebugBuild),
        )
        val fingerprint = buildString {
            append(request.token).append('|')
            append(request.deviceId).append('|')
            append(request.platform).append('|')
            append(request.appBuild).append('|')
            append(request.versionId).append('|')
            append(request.timezone).append('|')
            append(request.reminderHour).append(':').append(request.reminderMinute).append('|')
            append(request.remindersEnabled).append('|')
            append(request.topics.joinToString(","))
        }
        return registrationMutex.withLock {
            val nowMs = nowProvider()
            val elapsedMs = nowMs - lastRegisteredAtMs
            if (fingerprint == lastSuccessfulFingerprint && elapsedMs in 0L..dedupeWindowMs) {
                return@withLock true
            }
            try {
                val response = withContext(Dispatchers.IO) {
                    client.post {
                        expectSuccess = false
                        url(endpoint)
                        header(ProtectedEndpoints.headerClientPlatform(), platform.lowercase())
                        header(ProtectedEndpoints.headerClientDeviceId(), deviceId)
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }
                }
                if (isDebugBuild) {
                    println("[Push] registerDeviceToken status=${response.status.value}")
                }
                val ok = response.status.isSuccess()
                if (ok) {
                    lastSuccessfulFingerprint = fingerprint
                    lastRegisteredAtMs = nowProvider()
                }
                ok
            } catch (ce: CancellationException) {
                throw ce
            } catch (err: Exception) {
                if (isDebugBuild) {
                    println("[Push] registerDeviceToken error=${err::class.simpleName}: ${err.message}")
                }
                Napier.w("Push registration sync failed: ${err.message}")
                false
            }
        }
    }

    suspend fun unregisterDeviceToken(
        token: String,
        platform: String,
        deviceId: String = AnalyticsTracker.deviceId,
        client: HttpClient = httpClientCloudGateway,
    ): Boolean {
        val cleanToken = token.trim()
        if (cleanToken.length < 8 && deviceId.isBlank()) return false
        val baseUrl = CloudSessionManager.configuredGatewayUrl
            .takeIf { CloudSessionManager.parseSecureGatewayUrl(it) != null }
            ?: return false
        val path = ProtectedEndpoints.pushUnregisterPath()
        val endpoint = if ("://" in baseUrl) "$baseUrl$path" else "https://$baseUrl$path"
        return registrationMutex.withLock {
            lastSuccessfulFingerprint = null
            lastRegisteredAtMs = 0L
            try {
                val response = withContext(Dispatchers.IO) {
                    client.post {
                        expectSuccess = false
                        url(endpoint)
                        header(ProtectedEndpoints.headerClientPlatform(), platform.lowercase())
                        header(ProtectedEndpoints.headerClientDeviceId(), deviceId)
                        contentType(ContentType.Application.Json)
                        setBody(PushDeviceUnregisterRequest(token = cleanToken, deviceId = deviceId))
                    }
                }
                response.status.isSuccess()
            } catch (ce: CancellationException) {
                throw ce
            } catch (err: Exception) {
                Napier.w("Push unregistration failed: ${err.message}")
                false
            }
        }
    }
}
