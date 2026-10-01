package data

import data.bibleIQ.BibleIQDataModel
import email.kevinphillips.biblebible.BuildKonfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
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

private fun createHttpClient(config: AppHttpClientConfig, timeout: Long = TIMEOUT_LIMIT): HttpClient {
    return HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = timeout
            connectTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
        install(Resources)
        if (!BibleIQDataModel.RELEASE_BUILD) {
            install(Logging) {
                logger = Logger.SIMPLE
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
}

import data.security.SecretObfuscator

val httpClientBibleAPI: HttpClient by lazy {
    val config = AppHttpClientConfig(
        baseUrl = "api.scripture.api.bible/v1", apiKeyHeader = "api-key",
        apiKey = SecretObfuscator.deobfuscate(BuildKonfig.API_KEY_API_BIBLE)
    )
    createHttpClient(config)
}

val httpClientBibleIQ: HttpClient by lazy {
    val config = AppHttpClientConfig(
        baseUrl = "iq-bible.p.rapidapi.com", apiKeyHeader = "X-RapidAPI-Key",
        apiKey = SecretObfuscator.deobfuscate(BuildKonfig.API_KEY)
    )
    createHttpClient(config)
}

val httpClientGemini: HttpClient by lazy {
    val config = AppHttpClientConfig(
        baseUrl = "generativelanguage.googleapis.com", apiKeyHeader = "",
        apiKey = SecretObfuscator.deobfuscate(BuildKonfig.GEMINI_API_KEY)
    )
    createHttpClient(config, timeout = TIMEOUT_LIMIT_GEMINI)
}