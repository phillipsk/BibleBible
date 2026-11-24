package data.config

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.analytics.AnalyticsTracker
import data.appPrefs.DEFAULT_TUTORIAL_REMINDER_DAYS
import data.bible.BibleState
import data.bible.BibleVersion
import data.bible.BibleVersions
import data.gemini.GEMINI_MODEL
import data.httpClientCloudGateway
import data.notifications.VotdNotificationController
import data.security.AppIntegrityProvider
import data.security.ChallengeResponseDto
import data.security.ClientAttestationDto
import email.kevinphillips.biblebible.getPlatformName
import email.kevinphillips.biblebible.security.ProtectedEndpoints
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.native.concurrent.ThreadLocal

private val validBookShimmerStyles = setOf(
    "soft_gold_sheen",
    "candle_breathe",
    "row_wave",
    "horizon_pulse",
    "electric",
    "none",
)

private val validTabletBookLayouts = setOf(
    "split_testaments",
    "testament_divider",
    "mobile_breaks",
)

private val validReaderLayoutStyles = setOf(
    "literary_serif",
    "verse_per_line",
    "classic_inline",
)

private val validHomePlansLayouts = setOf(
    "adaptive",
    "stacked",
    "twin_cards",
)

private fun parseUsfmSet(rawCsv: String, fallback: Set<String>): Set<String> {
    val parsed = rawCsv.split(",")
        .map { it.trim().uppercase() }
        .filter { it.isNotEmpty() }
        .toSet()
    return parsed.ifEmpty { fallback }
}

private val defaultMobileSectionBreaks = setOf("JOS", "JOB", "DAN", "MAL", "ROM", "PHM")
private val defaultTabletSectionBreaks = setOf("MAL")

@Serializable
internal data class CloudFeatureFlagsDto(
    @SerialName("biblicalShortsEnabled") val biblicalShortsEnabled: Boolean = true,
    @SerialName("verseOfTheDayEnabled") val verseOfTheDayEnabled: Boolean = true,
    @SerialName("inHouseWordsEnabled") val inHouseWordsEnabled: Boolean = true,
    @SerialName("bookShimmerStyle") val bookShimmerStyle: String = "soft_gold_sheen",
    @SerialName("bookShimmerPace") val bookShimmerPace: Float = 1.0f,
    @SerialName("bookShimmerIntensity") val bookShimmerIntensity: Float = 1.0f,
    @SerialName("bookSectionPaddingDp") val bookSectionPaddingDp: Int = 10,
    @SerialName("bookGridTabletLayout") val bookGridTabletLayout: String = "split_testaments",
    @SerialName("bookSectionBreaksMobile") val bookSectionBreaksMobile: String = "JOS,JOB,DAN,MAL,ROM,PHM",
    @SerialName("bookSectionBreaksTablet") val bookSectionBreaksTablet: String = "MAL",
    @SerialName("readerLayoutStyle") val readerLayoutStyle: String = "literary_serif",
    @SerialName("readerLineHeightMultiplier") val readerLineHeightMultiplier: Float = 1.68f,
    @SerialName("readerVerseGapDp") val readerVerseGapDp: Int = 14,
    @SerialName("readerDefaultFontSizeSp") val readerDefaultFontSizeSp: Float = 22.0f,
    @SerialName("readerRedLetterEnabled") val readerRedLetterEnabled: Boolean = true,
    @SerialName("homeSwipePreviewEnabled") val homeSwipePreviewEnabled: Boolean = true,
    @SerialName("homeSwipeToShortsEnabled") val homeSwipeToShortsEnabled: Boolean = true,
    @SerialName("homeScrollToGenesisEnabled") val homeScrollToGenesisEnabled: Boolean = true,
    @SerialName("shortsAutoAdvanceEnabled") val shortsAutoAdvanceEnabled: Boolean = true,
    @SerialName("votdNotificationsEnabled") val votdNotificationsEnabled: Boolean = true,
    @SerialName("readerTutorialEnabled") val readerTutorialEnabled: Boolean = true,
    @SerialName("readerTutorialReminderDays") val readerTutorialReminderDays: Int = DEFAULT_TUTORIAL_REMINDER_DAYS,
    @SerialName("biblicalShortsTutorialEnabled") val biblicalShortsTutorialEnabled: Boolean = true,
    @SerialName("biblicalShortsTutorialReminderDays") val biblicalShortsTutorialReminderDays: Int = DEFAULT_TUTORIAL_REMINDER_DAYS,
    @SerialName("elasticSearchEnabled") val elasticSearchEnabled: Boolean = true,
    @SerialName("searchMasonryTopicsEnabled") val searchMasonryTopicsEnabled: Boolean = true,
    @SerialName("readingPlansEnabled") val readingPlansEnabled: Boolean = true,
    @SerialName("readingPlansAutoCheckoffEnabled") val readingPlansAutoCheckoffEnabled: Boolean = true,
    @SerialName("onDeviceAiEnabled") val onDeviceAiEnabled: Boolean = true,
    @SerialName("cloudSearchAiFallbackEnabled") val cloudSearchAiFallbackEnabled: Boolean = true,
    @SerialName("homePlansLayout") val homePlansLayout: String = "adaptive",
)

@Serializable
internal data class CloudRuntimeConfigDto(
    @SerialName("schemaVersion") val schemaVersion: Int = 1,
    @SerialName("aiSummariesEnabled") val aiSummariesEnabled: Boolean = true,
    @SerialName("geminiModel") val geminiModel: String = GEMINI_MODEL,
    @SerialName("defaultVersion") val defaultVersion: String = "KJV",
    @SerialName("supportedVersions") val supportedVersions: List<BibleVersion> = emptyList(),
    @SerialName("sessionToken") val sessionToken: String = "",
    @SerialName("tokenExpiresInSeconds") val tokenExpiresInSeconds: Long = 3600L,
    @SerialName("attestationRequired") val attestationRequired: Boolean = false,
    @SerialName("features") val features: CloudFeatureFlagsDto = CloudFeatureFlagsDto(),
) {
    override fun toString(): String =
        "CloudRuntimeConfigDto(schemaVersion=$schemaVersion, aiSummariesEnabled=$aiSummariesEnabled, geminiModel=$geminiModel, defaultVersion=$defaultVersion, versions=${supportedVersions.size}, attestationRequired=$attestationRequired, features=$features, sessionToken=[redacted])"
}

@Serializable
internal data class CloudTokenResponseDto(
    @SerialName("sessionToken") val sessionToken: String = "",
    @SerialName("tokenExpiresInSeconds") val tokenExpiresInSeconds: Long = 3600L,
    @SerialName("tier") val tier: String = "anon",
    @SerialName("attested") val attested: Boolean = false,
) {
    override fun toString(): String = "CloudTokenResponseDto(tier=$tier, attested=$attested, sessionToken=[redacted])"
}

@ThreadLocal
internal object CloudSessionManager {
    private var gatewayUrlOverride: String? = null
    internal var customAttesterForTesting: (suspend (String) -> ClientAttestationDto?)? = null

    val configuredGatewayUrl: String
        get() = (gatewayUrlOverride ?: ProtectedEndpoints.gatewayUrl()).trim().trimEnd('/')

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

    var attestationRequired: Boolean by mutableStateOf(false)
        internal set

    var aiSummariesEnabled: Boolean by mutableStateOf(email.kevinphillips.biblebible.BuildKonfig.AI_SUMMARIES_ENABLED)
        internal set

    var geminiModel: String by mutableStateOf(GEMINI_MODEL)
        internal set

    var featureFlags: CloudFeatureFlagsDto by mutableStateOf(CloudFeatureFlagsDto())
        internal set

    val biblicalShortsEnabled: Boolean
        get() = featureFlags.biblicalShortsEnabled

    val verseOfTheDayEnabled: Boolean
        get() = featureFlags.verseOfTheDayEnabled

    val bookShimmerStyle: String
        get() = featureFlags.bookShimmerStyle
            .trim()
            .lowercase()
            .takeIf { it in validBookShimmerStyles }
            ?: "soft_gold_sheen"

    val bookShimmerPace: Float
        get() = featureFlags.bookShimmerPace
            .takeIf { it.isFinite() }
            ?.coerceIn(0.25f, 3.0f)
            ?: 1.0f

    val bookShimmerIntensity: Float
        get() = featureFlags.bookShimmerIntensity
            .takeIf { it.isFinite() }
            ?.coerceIn(0f, 2.0f)
            ?: 1.0f

    val bookSectionPaddingDp: Int
        get() = featureFlags.bookSectionPaddingDp.coerceIn(0, 48)

    val bookGridTabletLayout: String
        get() = featureFlags.bookGridTabletLayout
            .trim()
            .lowercase()
            .takeIf { it in validTabletBookLayouts }
            ?: "split_testaments"

    val bookSectionBreaksMobile: Set<String>
        get() = parseUsfmSet(featureFlags.bookSectionBreaksMobile, defaultMobileSectionBreaks)

    val bookSectionBreaksTablet: Set<String>
        get() = parseUsfmSet(featureFlags.bookSectionBreaksTablet, defaultTabletSectionBreaks)

    val readerLayoutStyle: String
        get() = featureFlags.readerLayoutStyle
            .trim()
            .lowercase()
            .takeIf { it in validReaderLayoutStyles }
            ?: "literary_serif"

    val readerLineHeightMultiplier: Float
        get() = featureFlags.readerLineHeightMultiplier
            .takeIf { it.isFinite() }
            ?.coerceIn(1.2f, 2.2f)
            ?: 1.68f

    val readerVerseGapDp: Int
        get() = featureFlags.readerVerseGapDp.coerceIn(0, 32)

    val readerDefaultFontSizeSp: Float
        get() = featureFlags.readerDefaultFontSizeSp
            .takeIf { it.isFinite() }
            ?.coerceIn(14f, 32f)
            ?: 22.0f

    val readerRedLetterEnabled: Boolean
        get() = featureFlags.readerRedLetterEnabled

    val elasticSearchEnabled: Boolean
        get() = featureFlags.elasticSearchEnabled

    val searchMasonryTopicsEnabled: Boolean
        get() = featureFlags.searchMasonryTopicsEnabled

    val readingPlansEnabled: Boolean
        get() = featureFlags.readingPlansEnabled

    val readingPlansAutoCheckoffEnabled: Boolean
        get() = featureFlags.readingPlansAutoCheckoffEnabled

    val onDeviceAiEnabled: Boolean
        get() = featureFlags.onDeviceAiEnabled

    val cloudSearchAiFallbackEnabled: Boolean
        get() = featureFlags.cloudSearchAiFallbackEnabled

    val homePlansLayout: String
        get() = featureFlags.homePlansLayout
            .trim()
            .lowercase()
            .takeIf { it in validHomePlansLayouts }
            ?: "adaptive"

    internal fun overrideGatewayUrlForTesting(url: String?) {
        gatewayUrlOverride = url
    }

    internal fun resetForTesting() {
        gatewayUrlOverride = null
        customAttesterForTesting = null
        sessionToken = ""
        attestationRequired = false
        aiSummariesEnabled = email.kevinphillips.biblebible.BuildKonfig.AI_SUMMARIES_ENABLED
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
        val prefix = if ("://" in baseUrl) baseUrl else "https://$baseUrl"
        val endpoint = "$prefix${ProtectedEndpoints.configPath()}"
        return try {
            val response = withContext(Dispatchers.IO) {
                client.get {
                    expectSuccess = false
                    header(ProtectedEndpoints.headerClientPlatform(), getPlatformName())
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
            if (config.attestationRequired && sessionToken.isBlank()) {
                refreshSessionToken(client)
            }
            VotdNotificationController.applyRemindersAvailability(config.features.votdNotificationsEnabled)
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
        val prefix = if ("://" in baseUrl) baseUrl else "https://$baseUrl"
        val challengeEndpoint = "$prefix${ProtectedEndpoints.authChallengePath()}"
        val tokenEndpoint = "$prefix${ProtectedEndpoints.authTokenPath()}"
        return try {
            var attestationPayload: ClientAttestationDto? = null
            if (attestationRequired || customAttesterForTesting != null || client === httpClientCloudGateway) {
                val challengeResp = withContext(Dispatchers.IO) {
                    runCatching {
                        client.get {
                            expectSuccess = false
                            header(ProtectedEndpoints.headerClientPlatform(), getPlatformName())
                            url(challengeEndpoint)
                        }
                    }.getOrNull()
                }
                if (challengeResp != null && challengeResp.status.isSuccess()) {
                    val nonce = runCatching { challengeResp.body<ChallengeResponseDto>().nonce }.getOrDefault("")
                    if (nonce.isNotBlank()) {
                        attestationPayload = customAttesterForTesting?.invoke(nonce)
                            ?: AppIntegrityProvider.attestChallenge(nonce)
                    }
                }
            }

            val runtimeStatus = AppIntegrityProvider.inspectRuntimeEnvironment()
            val response = withContext(Dispatchers.IO) {
                client.post {
                    expectSuccess = false
                    header(ProtectedEndpoints.headerClientPlatform(), getPlatformName())
                    header(
                        ProtectedEndpoints.headerClientIntegrity(),
                        if (runtimeStatus.compromised) "compromised" else "ok",
                    )
                    if (attestationPayload != null) {
                        contentType(ContentType.Application.Json)
                        setBody(attestationPayload)
                    }
                    url(tokenEndpoint)
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
        attestationRequired = config.attestationRequired
        if (config.sessionToken.isNotBlank()) {
            sessionToken = config.sessionToken
        }
        aiSummariesEnabled = email.kevinphillips.biblebible.BuildKonfig.AI_SUMMARIES_ENABLED && config.aiSummariesEnabled
        featureFlags = config.features
        if (config.geminiModel.isNotBlank()) {
            geminiModel = config.geminiModel
        }
        if (config.supportedVersions.isNotEmpty()) {
            BibleState.updateBibleVersions(BibleVersions(config.supportedVersions))
        }
    }
}
