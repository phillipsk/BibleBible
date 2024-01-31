package data

import email.kevinphillips.biblebible.BuildKonfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.header
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val httpClient
    get() = HttpClient {
        install(Resources)
        install(Logging) {
            level = LogLevel.HEADERS
            logger = Logger.SIMPLE
        }
        install(DefaultRequest)
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000 // Request timeout: 5 seconds
            connectTimeoutMillis = 3000 // Connect timeout: 3 seconds
            socketTimeoutMillis = 15000 // Socket timeout: 15 seconds
        }
//        install(HttpRetry) {
//            maxAttempts = 3 // Number of retry attempts
//        }
        defaultRequest {
            url {
                host = "api.scripture.api.bible"
                path("v1/")
                protocol = URLProtocol.HTTPS
            }
            header("api-key", BuildKonfig.API_KEY_API_BIBLE)

        }
    }