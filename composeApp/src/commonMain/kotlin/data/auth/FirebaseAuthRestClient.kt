package data.auth

import data.httpClientCloudGateway
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class FirebaseAuthException(message: String, val code: String? = null) : Exception(message)

@Serializable
private data class FirebaseErrorResponseDto(
    val error: FirebaseErrorDetailDto? = null,
)

@Serializable
private data class FirebaseErrorDetailDto(
    val code: Int = 0,
    val message: String = "",
)

class FirebaseAuthRestClient(
    private val client: HttpClient = httpClientCloudGateway,
    private val apiKeyProvider: () -> String,
) {
    private val identityToolkitBase = "https://identitytoolkit.googleapis.com/v1/accounts"
    private val secureTokenUrl = "https://securetoken.googleapis.com/v1/token"

    private fun getApiKey(): String {
        val key = apiKeyProvider().trim()
        if (key.isBlank()) throw FirebaseAuthException("Firebase Web API key is not configured.", "missing_api_key")
        return key
    }

    suspend fun signInWithGoogleIdToken(idToken: String): FirebaseSignInWithIdpResponseDto =
        withContext(Dispatchers.IO) {
            val key = getApiKey()
            val postBody = "id_token=$idToken&providerId=google.com"
            executeIdpSignIn(key, postBody)
        }

    suspend fun signInWithAppleIdToken(idToken: String, rawNonce: String? = null): FirebaseSignInWithIdpResponseDto =
        withContext(Dispatchers.IO) {
            val key = getApiKey()
            val nonceParam = if (!rawNonce.isNullOrBlank()) "&nonce=$rawNonce" else ""
            val postBody = "id_token=$idToken&providerId=apple.com$nonceParam"
            executeIdpSignIn(key, postBody)
        }

    private suspend fun executeIdpSignIn(key: String, postBody: String): FirebaseSignInWithIdpResponseDto {
        val endpoint = "$identityToolkitBase:signInWithIdp?key=$key"
        val requestBody = FirebaseSignInWithIdpRequestDto(
            postBody = postBody,
            requestUri = "http://localhost",
            returnSecureToken = true,
            returnIdpCredential = true,
        )

        val response = client.post {
            expectSuccess = false
            url(endpoint)
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        if (!response.status.isSuccess()) {
            val errorMsg = extractErrorMessage(response)
            Napier.e("Firebase signInWithIdp error: $errorMsg", tag = "FirebaseAuth")
            throw FirebaseAuthException(errorMsg, "sign_in_failed")
        }

        return response.body<FirebaseSignInWithIdpResponseDto>()
    }

    suspend fun sendEmailSignInLink(email: String, continueUrl: String): Unit =
        withContext(Dispatchers.IO) {
            val key = getApiKey()
            val endpoint = "$identityToolkitBase:sendOobCode?key=$key"
            val requestBody = FirebaseSendOobCodeRequestDto(
                requestType = "EMAIL_SIGNIN",
                email = email.trim(),
                continueUrl = continueUrl,
                canHandleCodeInApp = true,
            )

            val response = client.post {
                expectSuccess = false
                url(endpoint)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (!response.status.isSuccess()) {
                val errorMsg = extractErrorMessage(response)
                Napier.e("Firebase sendOobCode error: $errorMsg", tag = "FirebaseAuth")
                throw FirebaseAuthException(errorMsg, "send_email_failed")
            }
        }

    suspend fun completeEmailSignIn(email: String, oobCode: String): FirebaseSignInWithIdpResponseDto =
        withContext(Dispatchers.IO) {
            val key = getApiKey()
            val endpoint = "$identityToolkitBase:signInWithEmailLink?key=$key"
            val requestBody = FirebaseSignInWithEmailLinkRequestDto(
                email = email.trim(),
                oobCode = oobCode.trim(),
            )

            val response = client.post {
                expectSuccess = false
                url(endpoint)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (!response.status.isSuccess()) {
                val errorMsg = extractErrorMessage(response)
                Napier.e("Firebase completeEmailSignIn error: $errorMsg", tag = "FirebaseAuth")
                throw FirebaseAuthException(errorMsg, "email_link_invalid")
            }

            return@withContext response.body<FirebaseSignInWithIdpResponseDto>()
        }

    suspend fun refreshIdToken(refreshToken: String): FirebaseRefreshTokenResponseDto =
        withContext(Dispatchers.IO) {
            val key = getApiKey()
            val endpoint = "$secureTokenUrl?key=$key"

            val response = client.submitForm(
                url = endpoint,
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken)
                }
            ) {
                expectSuccess = false
            }

            if (!response.status.isSuccess()) {
                val errorMsg = extractErrorMessage(response)
                Napier.w("Firebase refreshIdToken failed: $errorMsg", tag = "FirebaseAuth")
                throw FirebaseAuthException(errorMsg, "refresh_failed")
            }

            return@withContext response.body<FirebaseRefreshTokenResponseDto>()
        }

    private suspend fun extractErrorMessage(response: io.ktor.client.statement.HttpResponse): String {
        return runCatching {
            val errDto = response.body<FirebaseErrorResponseDto>()
            errDto.error?.message?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: "HTTP ${response.status.value}"
    }
}
