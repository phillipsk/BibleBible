package data

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

val httpClient get() = HttpClient {
    install(Resources)
    install(Logging) { logger = Logger.SIMPLE }
    install(DefaultRequest)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
    defaultRequest {
        url {
            host = "iq-bible.p.rapidapi.com"
            protocol = URLProtocol.HTTPS
        }
        header("X-RapidAPI-Key", BuildKonfig.API_KEY)

    }
}