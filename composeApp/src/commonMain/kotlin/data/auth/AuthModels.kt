package data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val providers: List<String> = emptyList(),
)

sealed interface AuthState {
    data object SignedOut : AuthState
    data object Loading : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
    data class Error(val message: String, val code: String? = null) : AuthState
}

// ---- Firebase Auth REST DTOs ----

@Serializable
data class FirebaseSignInWithIdpRequestDto(
    val postBody: String,
    val requestUri: String = "http://localhost",
    val returnSecureToken: Boolean = true,
    val returnIdpCredential: Boolean = true,
)

@Serializable
data class FirebaseSignInWithIdpResponseDto(
    val idToken: String = "",
    val refreshToken: String = "",
    val localId: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val expiresIn: String? = null,
)

@Serializable
data class FirebaseSendOobCodeRequestDto(
    val requestType: String = "EMAIL_SIGNIN",
    val email: String,
    val continueUrl: String,
    val canHandleCodeInApp: Boolean = true,
)

@Serializable
data class FirebaseSendOobCodeResponseDto(
    val email: String? = null,
)

@Serializable
data class FirebaseSignInWithEmailLinkRequestDto(
    val email: String,
    val oobCode: String,
)

@Serializable
data class FirebaseRefreshTokenResponseDto(
    @SerialName("id_token") val idToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: String = "",
    @SerialName("user_id") val userId: String = "",
)

// ---- Cloud Run Gateway DTOs ----

@Serializable
data class GatewayAuthSessionRequestDto(
    val platform: String = "kmp",
)

@Serializable
data class GatewayUserDto(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val providers: List<String> = emptyList(),
    val createdAt: String? = null,
)

@Serializable
data class GatewayAuthSessionResponseDto(
    val sessionToken: String = "",
    val tokenExpiresInSeconds: Long = 3600L,
    val user: GatewayUserDto,
)

@Serializable
data class GatewayUserProfileResponseDto(
    val user: GatewayUserDto,
    val preferences: Map<String, String>? = null,
)

@Serializable
data class SyncRecordDto(
    val value: String? = null,
    val updatedAt: Long = 0L,
)

@Serializable
data class GatewaySyncResponseDto(
    val serverTime: String = "",
    val records: Map<String, SyncRecordDto> = emptyMap(),
)

@Serializable
data class GatewaySyncPutRequestDto(
    val records: Map<String, SyncRecordDto> = emptyMap(),
)
