import androidx.compose.runtime.MutableState
import email.kevinphillips.biblebible.BuildKonfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.get
import io.ktor.client.request.header
import io.ktor.http.URLProtocol
import io.ktor.resources.Resource
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val httpClient = HttpClient {
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

@Serializable
@Resource("/GetBooks")
private class GetBooks(val language: String = "english")

@Serializable
data class Book(private val b: String = "", @SerialName("n") val name: String = "") {
    val bookId = b.toInt()
}

@Resource("/GetChapters")
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

internal suspend fun getBooks(books: MutableState<List<Book>>) {
    try {
        val getBooks: List<Book> = httpClient.get(GetBooks()).body<List<Book>>()
        books.value = getBooks
    } catch (e: Exception) {
        // Handle exceptions here (e.g., network issues, API errors)
        println("Error: ${e.message}")
    } finally {
        httpClient.close() // Close the HttpClient when done
    }
}
