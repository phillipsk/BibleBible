package data.bibleIQ

import JSON_BOOKS
import JSON_VERSIONS
import androidx.compose.runtime.MutableState
import data.httpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.serialization.json.Json

const val READ_JSON = true
suspend fun getBooks(books: MutableState<List<BibleBook>>) {
    try {
        val getBooks = if (READ_JSON) {
            Json.decodeFromString<List<BibleBook>>(JSON_BOOKS)
        } else {
            httpClient.get(GetBooks()).body<List<BibleBook>>()
        }
        BibleIQ.books.value = getBooks
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        httpClient.close()
    }
}

suspend fun getVersions() {
    try {
        val versions = if (READ_JSON) {
            Json.decodeFromString<List<BibleVersion>>(JSON_VERSIONS)
        } else {
            httpClient.get(GetVersions()).body<List<BibleVersion>>()
        }
        BibleIQ.bibleVersions.value = versions
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        httpClient.close()
    }
}