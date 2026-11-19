package data.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FirebaseAuthRestClientTest {

    private fun createMockClient(responseBody: String, statusCode: HttpStatusCode = HttpStatusCode.OK): HttpClient {
        val engine = MockEngine { _ ->
            respond(
                content = responseBody,
                status = statusCode,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Test
    fun testSignInWithGoogleIdTokenSuccess() = runTest {
        val mockJson = """
            {
              "idToken": "fake-firebase-id-token-123",
              "refreshToken": "fake-refresh-token-456",
              "localId": "uid-abc-789",
              "email": "test@biblebible.app",
              "displayName": "Test Apostle"
            }
        """.trimIndent()

        val client = FirebaseAuthRestClient(createMockClient(mockJson)) { "test-api-key" }
        val response = client.signInWithGoogleIdToken("fake-google-token")

        assertEquals("fake-firebase-id-token-123", response.idToken)
        assertEquals("fake-refresh-token-456", response.refreshToken)
        assertEquals("uid-abc-789", response.localId)
        assertEquals("test@biblebible.app", response.email)
    }

    @Test
    fun testSendEmailSignInLinkSuccess() = runTest {
        val mockJson = """{ "email": "test@biblebible.app" }"""
        val client = FirebaseAuthRestClient(createMockClient(mockJson)) { "test-api-key" }
        client.sendEmailSignInLink("test@biblebible.app", "https://biblebible.app/auth/complete")
    }

    @Test
    fun testRefreshTokenSuccess() = runTest {
        val mockJson = """
            {
              "id_token": "new-id-token-321",
              "refresh_token": "new-refresh-token-654",
              "expires_in": "3600",
              "user_id": "uid-abc-789"
            }
        """.trimIndent()

        val client = FirebaseAuthRestClient(createMockClient(mockJson)) { "test-api-key" }
        val refreshed = client.refreshIdToken("old-refresh-token")

        assertEquals("new-id-token-321", refreshed.idToken)
        assertEquals("new-refresh-token-654", refreshed.refreshToken)
        assertEquals("uid-abc-789", refreshed.userId)
    }

    @Test
    fun testSignInFailureThrowsFirebaseAuthException() = runTest {
        val errorJson = """
            {
              "error": {
                "code": 400,
                "message": "INVALID_IDP_RESPONSE"
              }
            }
        """.trimIndent()

        val client = FirebaseAuthRestClient(createMockClient(errorJson, HttpStatusCode.BadRequest)) { "test-api-key" }

        val ex = assertFailsWith<FirebaseAuthException> {
            client.signInWithGoogleIdToken("invalid-token")
        }
        assertEquals("INVALID_IDP_RESPONSE", ex.message)
    }
}
