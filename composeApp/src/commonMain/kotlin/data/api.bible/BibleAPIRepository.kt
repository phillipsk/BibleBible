package data.api.bible

import data.api.bible.json.JSON_BIBLES_API_BIBLE
import data.api.bible.json.JSON_BOOKS_API_BIBLE
import data.bibleIQ.BibleIQ
import data.httpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.serialization.json.Json

const val LOCAL_DATA = true

suspend fun getBiblesBibleAPI() {
    try {
        BibleIQ.bibleVersions.value = if (LOCAL_DATA) {
            Json.decodeFromString<BibleAPIBibles>(JSON_BIBLES_API_BIBLE)
        } else {
            httpClient.get<GetBiblesAPIBible>(GetBiblesAPIBible()).body<BibleAPIBibles>()
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        httpClient.close()
    }
}
suspend fun getBooksBibleAPI() {
    try {
        val getBooksAPIBible = if (LOCAL_DATA) {
            Json.decodeFromString<BibleAPIBook>(JSON_BOOKS_API_BIBLE)
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