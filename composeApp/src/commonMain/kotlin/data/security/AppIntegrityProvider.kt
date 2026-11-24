package data.security

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChallengeResponseDto(
    @SerialName("nonce") val nonce: String = "",
    @SerialName("expiresInSeconds") val expiresInSeconds: Int = 120,
)

@Serializable
internal data class ClientAttestationDto(
    @SerialName("platform") val platform: String,
    @SerialName("mode") val mode: String? = null,
    @SerialName("nonce") val nonce: String,
    @SerialName("integrityToken") val integrityToken: String? = null,
    @SerialName("keyId") val keyId: String? = null,
    @SerialName("attestationObjectB64") val attestationObjectB64: String? = null,
    @SerialName("clientDataB64") val clientDataB64: String? = null,
    @SerialName("assertionObjectB64") val assertionObjectB64: String? = null,
)

internal data class RuntimeResilienceStatus(
    val compromised: Boolean,
    val signals: List<String>,
)

internal expect object AppIntegrityProvider {
    fun inspectRuntimeEnvironment(): RuntimeResilienceStatus
    suspend fun attestChallenge(nonce: String): ClientAttestationDto?
}
