package data.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.analytics.AnalyticsTracker
import data.config.CloudSessionManager
import data.httpClientCloudGateway
import email.kevinphillips.biblebible.getPlatformName
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object AuthRepository {
    private var restClient: FirebaseAuthRestClient? = null
    private var tokenStore: SecureTokenStore = SecureTokenStore()
    private var customHttpClient: HttpClient? = null

    var authState: AuthState by mutableStateOf(AuthState.SignedOut)
        private set

    val currentUser: AuthUser?
        get() = (authState as? AuthState.SignedIn)?.user

    val isSignedIn: Boolean
        get() = currentUser != null

    private fun getClient(): FirebaseAuthRestClient {
        val configured = restClient
        if (configured != null) return configured
        val client = customHttpClient ?: httpClientCloudGateway
        val created = FirebaseAuthRestClient(client) {
            CloudSessionManager.authConfig?.firebaseApiKey ?: ""
        }
        restClient = created
        return created
    }

    internal fun configureForTesting(
        client: FirebaseAuthRestClient,
        store: SecureTokenStore,
    ) {
        restClient = client
        tokenStore = store
    }

    internal fun resetForTesting() {
        restClient = null
        authState = AuthState.SignedOut
    }

    suspend fun restoreSession() {
        if (!CloudSessionManager.isGatewayConfigured) return
        val storedRefreshToken = tokenStore.getRefreshToken() ?: return

        authState = AuthState.Loading
        try {
            val refreshed = getClient().refreshIdToken(storedRefreshToken)
            if (refreshed.idToken.isNotBlank()) {
                if (refreshed.refreshToken.isNotBlank() && refreshed.refreshToken != storedRefreshToken) {
                    tokenStore.saveRefreshToken(refreshed.refreshToken)
                }
                exchangeAndApplySession(refreshed.idToken)
            } else {
                signOutLocal()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Session restoration failed: ${e.message}", tag = "AuthRepository")
            signOutLocal()
        }
    }

    suspend fun signInWithGoogle(googleIdToken: String) {
        AnalyticsTracker.track("auth_sign_in_started", "auth", mapOf("provider" to "google"))
        authState = AuthState.Loading
        try {
            val response = getClient().signInWithGoogleIdToken(googleIdToken)
            if (response.refreshToken.isNotBlank()) {
                tokenStore.saveRefreshToken(response.refreshToken)
            }
            exchangeAndApplySession(response.idToken)
            AnalyticsTracker.track("auth_sign_in_succeeded", "auth", mapOf("provider" to "google"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.message ?: "Sign in with Google failed"
            Napier.e("Google sign in failed: $msg", tag = "AuthRepository")
            AnalyticsTracker.track("auth_sign_in_failed", "auth", mapOf("provider" to "google", "reason" to (e::class.simpleName ?: "error")))
            authState = AuthState.Error(msg)
        }
    }

    suspend fun signInWithApple(appleIdToken: String, rawNonce: String? = null) {
        AnalyticsTracker.track("auth_sign_in_started", "auth", mapOf("provider" to "apple"))
        authState = AuthState.Loading
        try {
            val response = getClient().signInWithAppleIdToken(appleIdToken, rawNonce)
            if (response.refreshToken.isNotBlank()) {
                tokenStore.saveRefreshToken(response.refreshToken)
            }
            exchangeAndApplySession(response.idToken)
            AnalyticsTracker.track("auth_sign_in_succeeded", "auth", mapOf("provider" to "apple"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.message ?: "Sign in with Apple failed"
            Napier.e("Apple sign in failed: $msg", tag = "AuthRepository")
            AnalyticsTracker.track("auth_sign_in_failed", "auth", mapOf("provider" to "apple", "reason" to (e::class.simpleName ?: "error")))
            authState = AuthState.Error(msg)
        }
    }

    suspend fun sendEmailSignInLink(email: String) {
        AnalyticsTracker.track("auth_sign_in_started", "auth", mapOf("provider" to "email_link"))
        val continueUrl = CloudSessionManager.authConfig?.emailLinkContinueUrl
            ?: "${CloudSessionManager.configuredGatewayUrl}/v1/auth/email/complete"
        getClient().sendEmailSignInLink(email, continueUrl)
    }

    suspend fun completeEmailSignInLink(email: String, oobCode: String) {
        authState = AuthState.Loading
        try {
            val response = getClient().completeEmailSignIn(email, oobCode)
            if (response.refreshToken.isNotBlank()) {
                tokenStore.saveRefreshToken(response.refreshToken)
            }
            exchangeAndApplySession(response.idToken)
            AnalyticsTracker.track("auth_sign_in_succeeded", "auth", mapOf("provider" to "email_link"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.message ?: "Invalid or expired sign-in link"
            Napier.e("Email link sign in failed: $msg", tag = "AuthRepository")
            AnalyticsTracker.track("auth_sign_in_failed", "auth", mapOf("provider" to "email_link", "reason" to (e::class.simpleName ?: "error")))
            authState = AuthState.Error(msg)
        }
    }

    private suspend fun exchangeAndApplySession(firebaseIdToken: String) = withContext(Dispatchers.IO) {
        val baseUrl = CloudSessionManager.configuredGatewayUrl
        val endpoint = "$baseUrl/v1/auth/session"
        val client = customHttpClient ?: httpClientCloudGateway

        val response = client.post {
            expectSuccess = false
            url(endpoint)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $firebaseIdToken")
            setBody(GatewayAuthSessionRequestDto(platform = getPlatformName().lowercase()))
        }

        if (!response.status.isSuccess()) {
            throw FirebaseAuthException("Session token exchange failed (HTTP ${response.status.value})", "session_exchange_failed")
        }

        val sessionDto = response.body<GatewayAuthSessionResponseDto>()
        CloudSessionManager.userSessionToken = sessionDto.sessionToken

        val user = AuthUser(
            uid = sessionDto.user.uid,
            email = sessionDto.user.email,
            displayName = sessionDto.user.displayName,
            photoUrl = sessionDto.user.photoUrl,
            providers = sessionDto.user.providers,
        )
        authState = AuthState.SignedIn(user)
    }

    suspend fun signOut() {
        signOutLocal()
        AnalyticsTracker.track("auth_signed_out", "auth", emptyMap())
    }

    private suspend fun signOutLocal() {
        tokenStore.clearRefreshToken()
        CloudSessionManager.userSessionToken = null
        authState = AuthState.SignedOut
    }

    suspend fun deleteAccount() = withContext(Dispatchers.IO) {
        val userToken = CloudSessionManager.userSessionToken
        if (!userToken.isNullOrBlank()) {
            val baseUrl = CloudSessionManager.configuredGatewayUrl
            val endpoint = "$baseUrl/v1/me"
            val client = customHttpClient ?: httpClientCloudGateway

            try {
                client.delete {
                    expectSuccess = false
                    url(endpoint)
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                }
            } catch (e: Exception) {
                Napier.w("Remote account deletion request failed: ${e.message}", tag = "AuthRepository")
            }
        }
        signOutLocal()
        AnalyticsTracker.track("auth_account_deleted", "auth", emptyMap())
    }
}
