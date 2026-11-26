package data

import data.bible.BibleState
import data.config.CloudSessionManager
import email.kevinphillips.biblebible.security.ProtectedEndpoints
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal data class AppHttpClientConfig(
    val baseUrl: String,
    val apiKeyHeader: String = "",
    val apiKey: String = "",
    val path: String? = null,
    val tokenProvider: () -> String = { CloudSessionManager.sessionToken },
)

internal const val TIMEOUT_LIMIT = 20_000L
internal const val TIMEOUT_LIMIT_GEMINI = 30_000L

internal fun createHttpClient(
    config: AppHttpClientConfig,
    timeout: Long = TIMEOUT_LIMIT,
    engine: HttpClientEngine? = null,
    logger: Logger = Logger.SIMPLE,
    enableLogging: Boolean = !BibleState.RELEASE_BUILD,
): HttpClient {
    val integrityHeader = ProtectedEndpoints.headerClientIntegrity()
    val configure: HttpClientConfig<*>.() -> Unit = {
        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = timeout
            connectTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
        if (enableLogging) {
            install(Logging) {
                this.logger = logger
                level = LogLevel.INFO
                sanitizeHeader {
                    it.equals("x-goog-api-key", ignoreCase = true) ||
                        it.equals("X-RapidAPI-Key", ignoreCase = true) ||
                        it.equals("api-key", ignoreCase = true) ||
                        it.equals("X-Firebase-AppCheck", ignoreCase = true) ||
                        it.equals(integrityHeader, ignoreCase = true) ||
                        it.equals(HttpHeaders.Authorization, ignoreCase = true)
                }
            }
        }
        install(DefaultRequest)
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        defaultRequest {
            url {
                host = config.baseUrl
                protocol = URLProtocol.HTTPS
            }
            if (config.apiKeyHeader.isNotEmpty() && config.apiKey.isNotEmpty()) {
                header(config.apiKeyHeader, config.apiKey)
            } else {
                val bearer = config.tokenProvider().trim()
                if (bearer.isNotEmpty()) {
                    header(HttpHeaders.Authorization, "Bearer $bearer")
                }
            }
        }
    }
    return if (engine == null) HttpClient(configure) else HttpClient(engine, configure)
}

internal val httpClientCloudGateway: HttpClient by lazy {
    val config = AppHttpClientConfig(
        baseUrl = CloudSessionManager.gatewayHost,
    )
    createHttpClient(config)
}
