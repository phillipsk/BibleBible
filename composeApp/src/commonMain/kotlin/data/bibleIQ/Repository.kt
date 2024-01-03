package data.bibleIQ

import androidx.compose.runtime.MutableState
import data.httpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get

suspend fun getBooks(books: MutableState<List<Book>>) {
    try {
        val getBooks: List<Book> = httpClient.get(GetBooks()).body<List<Book>>()
        BibleIQ.books.value = getBooks
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        httpClient.close()
    }
}

suspend fun getVersions() {
    try {
        val versions = httpClient.get(GetVersions()).body<List<BibleVersion>>()
        BibleIQ.bibleVersions.value = versions
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        httpClient.close()
    }
}