package data

import data.bibleIQ.BibleIQDataModel
import email.kevinphillips.biblebible.BuildKonfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.header
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

data class AppHttpClientConfig(
    val baseUrl: String,
    val apiKeyHeader: String,
    val apiKey: String,
    val path: String? = null
)

const val TIMEOUT_LIMIT = 20_000L
const val TIMEOUT_LIMIT_GEMINI = 30_000L

internal fun createHttpClient(
    config: AppHttpClientConfig,
    timeout: Long = TIMEOUT_LIMIT,
    engine: HttpClientEngine? = null,
    logger: Logger = Logger.SIMPLE,
    enableLogging: Boolean = !BibleIQDataModel.RELEASE_BUILD,
): HttpClient {
    val configure: HttpClientConfig<*>.() -> Unit = {
        install(HttpTimeout) {
            requestTimeoutMillis = timeout
            connectTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
        install(Resources)
        if (enableLogging) {
            install(Logging) {
                this.logger = logger
                level = LogLevel.INFO
                sanitizeHeader {
                    it.equals("x-goog-api-key", ignoreCase = true) ||
                        it.equals("X-RapidAPI-Key", ignoreCase = true) ||
                        it.equals("api-key", ignoreCase = true) ||
                        it.equals("Authorization", ignoreCase = true)
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
            if (config.apiKeyHeader.isNotEmpty()) {
                header(config.apiKeyHeader, config.apiKey)
            }
        }
    }
    return if (engine == null) HttpClient(configure) else HttpClient(engine, configure)
}

val httpClientBibleAPI: HttpClient by lazy {
    val config = AppHttpClientConfig(
        baseUrl = "api.scripture.api.bible/v1", apiKeyHeader = "api-key",
        apiKey = BuildKonfig.API_KEY_API_BIBLE
    )
    createHttpClient(config)
}

val httpClientBibleIQ: HttpClient by lazy {
    val config = AppHttpClientConfig(
        baseUrl = "iq-bible.p.rapidapi.com", apiKeyHeader = "X-RapidAPI-Key",
        apiKey = BuildKonfig.API_KEY
    )
    createHttpClient(config)
}

val httpClientGemini: HttpClient by lazy {
    val config = AppHttpClientConfig(
        baseUrl = "generativelanguage.googleapis.com", apiKeyHeader = "x-goog-api-key",
        apiKey = BuildKonfig.GEMINI_API_KEY
    )
    createHttpClient(config, timeout = TIMEOUT_LIMIT_GEMINI)
}
