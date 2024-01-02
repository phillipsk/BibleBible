package data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import data.api.bible.Response
import email.kevinphillips.biblebible.BuildKonfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.native.concurrent.ThreadLocal

val httpClient = HttpClient {
    install(Logging) { logger = Logger.SIMPLE }
    install(DefaultRequest)
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        })
    }
/*    defaultRequest {
        url {
            host = "iq-bible.p.rapidapi.com"
            protocol = URLProtocol.HTTPS
        }
        header("X-RapidAPI-Key", BuildKonfig.API_KEY)

    }*/
}

@Serializable
private class GetBooks(val language: String = "english")

@Serializable
data class Book(private val b: String = "", @SerialName("n") val name: String = "") {
    val bookId = b.toInt()
}

private class GetChapters()

@Serializable
data class Chapter(
    private val id: String,
    private val b: String,
    private val c: String,
    private val v: String,
    @SerialName("t") val verse: String
) {
    val chapterVerseUUID = id.toLong()
    val bookId = b.toInt()
    val chapterId = c.toInt()
    val verseId = v.toInt()
}

internal suspend fun getBooks(books: MutableState<String>) {
    try {
//        val getBooks: List<Book> = httpClient.get(GetBooks()).body<List<Book>>()
//        Bible.books.value = getBooks
        val response = HttpClient().get("https://iq-bible.p.rapidapi.com/GetBooks?language=english") {
            header("X-RapidAPI-Key", BuildKonfig.API_KEY)
        }.body<String>()

        books.value = response
        val s: Response = httpClient.get("https://api.scripture.api.bible/v1/bibles?language=eng") {
            header("api-key", BuildKonfig.API_KEY_API_BIBLE)
        }.body<Response>()
        Bible.responseBibleApi.value = s
    } catch (e: Exception) {
        // Handle exceptions here (e.g., network issues, API errors)
        println("Error: ${e.message}")
    } finally {
        httpClient.close() // Close the HttpClient when done
    }
}

@ThreadLocal
object Bible {
    var responseBibleApi = mutableStateOf(Response())
    var books = mutableStateOf(listOf<Book>())
}