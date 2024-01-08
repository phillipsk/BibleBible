package data.api.bible

import data.bibleIQ.BibleIQ
import data.httpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.serialization.json.Json

const val READ_JSON = false
suspend fun getBooksBibleAPI() {
    try {
        val getBooksAPIBible = if (READ_JSON) {
            Json.decodeFromString<BibleAPIBook>("")
        } else {
            httpClient.get(GetBooksAPIBible()).body<BibleAPIBook>()
        }
        BibleIQ.books.value = getBooksAPIBible
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        httpClient.close()
    }
}


suspend fun getChapterBibleAPI() {
    try {
        BibleIQ.chapter.value = httpClient.get(GetChapterAPIBible()).body<ChapterContent>()
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        httpClient.close()
    }
}