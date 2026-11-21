package data.config

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.analytics.AnalyticsTracker
import data.bible.BibleState
import data.bible.BibleVersion
import data.bible.BibleVersions
import data.gemini.GEMINI_MODEL
import data.httpClientCloudGateway
import email.kevinphillips.biblebible.BuildKonfig
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.native.concurrent.ThreadLocal

@Serializable
data class CloudFeatureFlagsDto(
    val biblicalShortsEnabled: Boolean = true,
    val verseOfTheDayEnabled: Boolean = true,
    val inHouseWordsEnabled: Boolean = true,
)

@Serializable
data class CloudRuntimeConfigDto(
    val schemaVersion: Int = 1,
    val aiSummariesEnabled: Boolean = true,
    val geminiModel: String = GEMINI_MODEL,
    val defaultVersion: String = "KJV",
    val supportedVersions: List<BibleVersion> = emptyList(),
    val sessionToken: String = "",
    val tokenExpiresInSeconds: Long = 3600L,
    val features: CloudFeatureFlagsDto = CloudFeatureFlagsDto(),
) {
    override fun toString(): String =
        "CloudRuntimeConfigDto(schemaVersion=$schemaVersion, aiSummariesEnabled=$aiSummariesEnabled, geminiModel=$geminiModel, defaultVersion=$defaultVersion, versions=${supportedVersions.size}, features=$features, sessionToken=[redacted])"
}

@Serializable
data class CloudTokenResponseDto(
    val sessionToken: String = "",
    val tokenExpiresInSeconds: Long = 3600L,
) {
    override fun toString(): String = "CloudTokenResponseDto(sessionToken=[redacted])"
}

@ThreadLocal
object CloudSessionManager {
    private var gatewayUrlOverride: String? = null

    val configuredGatewayUrl: String
        get() = (gatewayUrlOverride ?: BuildKonfig.CLOUD_GATEWAY_URL).trim().trimEnd('/')

    val isGatewayConfigured: Boolean
        get() = parseSecureGatewayUrl(configuredGatewayUrl) != null

    val gatewayHost: String
        get() {
            val parsed = parseSecureGatewayUrl(configuredGatewayUrl) ?: return "unconfigured.invalid"
            val pathSuffix = parsed.encodedPath.trim('/')
            return if (pathSuffix.isEmpty()) parsed.host else "${parsed.host}/$pathSuffix"
        }

    var sessionToken: String by mutableStateOf("")
        internal set

    var aiSummariesEnabled: Boolean by mutableStateOf(BuildKonfig.AI_SUMMARIES_ENABLED)
        internal set

    var geminiModel: String by mutableStateOf(GEMINI_MODEL)
        internal set

    var featureFlags: CloudFeatureFlagsDto by mutableStateOf(CloudFeatureFlagsDto())
        internal set

    val biblicalShortsEnabled: Boolean
        get() = featureFlags.biblicalShortsEnabled

    val verseOfTheDayEnabled: Boolean
        get() = featureFlags.verseOfTheDayEnabled

    internal fun overrideGatewayUrlForTesting(url: String?) {
        gatewayUrlOverride = url
    }

    internal fun resetForTesting() {
        gatewayUrlOverride = null
        sessionToken = ""
        aiSummariesEnabled = BuildKonfig.AI_SUMMARIES_ENABLED
        geminiModel = GEMINI_MODEL
        featureFlags = CloudFeatureFlagsDto()
    }

    internal fun parseSecureGatewayUrl(rawUrl: String): Url? {
        if (rawUrl.isBlank()) return null
        return runCatching {
            val normalized = if ("://" in rawUrl) rawUrl else "https://$rawUrl"
            Url(normalized).also { parsed ->
                require(parsed.protocol == URLProtocol.HTTPS && parsed.host.isNotBlank())
                require(parsed.user.isNullOrEmpty() && parsed.password.isNullOrEmpty())
            }
        }.getOrNull()
    }

    suspend fun initializeStartupConfig(
        client: HttpClient = httpClientCloudGateway,
    ): CloudRuntimeConfigDto? {
        val baseUrl = configuredGatewayUrl.takeIf { parseSecureGatewayUrl(it) != null } ?: return null
        val endpoint = if ("://" in baseUrl) "$baseUrl/v1/config" else "https://$baseUrl/v1/config"
        return try {
            val response = withContext(Dispatchers.IO) {
                client.get {
                    expectSuccess = false
                    url(endpoint)
                }
            }
            if (!response.status.isSuccess()) {
                Napier.w("Cloud startup config returned HTTP ${response.status.value}", tag = "CloudConfig")
                AnalyticsTracker.track(
                    "cloud_config_fallback",
                    "lifecycle",
                    mapOf("httpStatus" to response.status.value.toString()),
                )
                return null
            }
            val config = response.body<CloudRuntimeConfigDto>()
            applyRuntimeConfig(config)
            AnalyticsTracker.track(
                "cloud_config_loaded",
                "lifecycle",
                mapOf(
                    "aiSummariesEnabled" to config.aiSummariesEnabled.toString(),
                    "geminiModel" to config.geminiModel,
                    "defaultVersion" to config.defaultVersion,
                ),
            )
            config
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Cloud startup config unavailable (${e::class.simpleName}); using local defaults", tag = "CloudConfig")
            AnalyticsTracker.track(
                "cloud_config_fallback",
                "lifecycle",
                mapOf("reason" to (e::class.simpleName ?: "exception")),
            )
            null
        }
    }

    suspend fun refreshSessionToken(
        client: HttpClient = httpClientCloudGateway,
    ): String? {
        val baseUrl = configuredGatewayUrl.takeIf { parseSecureGatewayUrl(it) != null } ?: return null
        val endpoint = if ("://" in baseUrl) "$baseUrl/v1/auth/token" else "https://$baseUrl/v1/auth/token"
        return try {
            val response = withContext(Dispatchers.IO) {
                client.post {
                    expectSuccess = false
                    url(endpoint)
                }
            }
            if (!response.status.isSuccess()) return null
            val tokenDto = response.body<CloudTokenResponseDto>()
            if (tokenDto.sessionToken.isNotBlank()) {
                sessionToken = tokenDto.sessionToken
                tokenDto.sessionToken
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    internal fun applyRuntimeConfig(config: CloudRuntimeConfigDto) {
        if (config.sessionToken.isNotBlank()) {
            sessionToken = config.sessionToken
        }
        aiSummariesEnabled = BuildKonfig.AI_SUMMARIES_ENABLED && config.aiSummariesEnabled
        featureFlags = config.features
        if (config.geminiModel.isNotBlank()) {
            geminiModel = config.geminiModel
        }
        if (config.supportedVersions.isNotEmpty()) {
            BibleState.updateBibleVersions(BibleVersions(config.supportedVersions))
        }
    }
}
