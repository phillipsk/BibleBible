package data

import data.bibleIQ.BibleIQDataModel
import email.kevinphillips.biblebible.BuildKonfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
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

data class HttpClientConfig(
    val baseUrl: String,
    val apiKeyHeader: String,
    val apiKey: String,
    val path: String? = null
)

private fun createHttpClient(config: HttpClientConfig): HttpClient {
    return HttpClient {
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
            header(config.apiKeyHeader, config.apiKey)
        }
    }
}

val httpClientBibleAPI: HttpClient by lazy {
    val config = HttpClientConfig(
        baseUrl = "api.scripture.api.bible/v1", apiKeyHeader = "api-key",
        apiKey = BuildKonfig.API_KEY_API_BIBLE
    )
    createHttpClient(config)
}

val httpClientBibleIQ: HttpClient by lazy {
    val config = HttpClientConfig(
        baseUrl = "iq-bible.p.rapidapi.com", apiKeyHeader = "X-RapidAPI-Key",
        apiKey = BuildKonfig.API_KEY
    )
    createHttpClient(config)
}

val httpClientGemini: HttpClient by lazy {
    val config = HttpClientConfig(
        baseUrl = "https://generativelanguage.googleapis.com/", apiKeyHeader = "X-RapidAPI-Key",
        apiKey = BuildKonfig.GEMINI_API_KEY
    )
    createHttpClient(config)
}