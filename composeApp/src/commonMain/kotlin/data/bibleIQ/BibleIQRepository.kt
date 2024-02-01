package data.bibleIQ

import JSON_BOOKS
import JSON_VERSIONS
import data.httpClientBibleIQ
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.serialization.json.Json

const val LOCAL_DATA = true
internal suspend fun getBooksBibleIQ() {
    try {
        val getBooks = if (LOCAL_DATA) {
            Json.decodeFromString<List<BibleBook>>(JSON_BOOKS)
        } else {
            httpClientBibleIQ.get(GetBooks()).body<List<BibleBook>>()
        }
//        BibleIQ.books.value = getBooks // TODO: uncomment for use
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        // httpClient.close()
    }
}

internal suspend fun getVersionsBibleIQ() {
    try {
        val versions = if (LOCAL_DATA) {
            Json.decodeFromString<List<BibleVersion>>(JSON_VERSIONS)
        } else {
            httpClientBibleIQ.get(GetVersions()).body<List<BibleVersion>>()
        }
//        BibleIQ.bibleVersions.value = versions
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        // httpClient.close()
    }
}

internal suspend fun getChapterBibleIQ(book: Any, version: String? = null, chapter: Int = 1) {
    val bookId = if (book !is Int) BibleIQDataModel.getAPIBibleCardinal(book as String) else book
    try {
        Napier.v("getChapterBibleIQ: bookId: $bookId :: chapter $chapter", tag = "BB2452")
        val chapters = httpClientBibleIQ.get(GetChapter(bookId = bookId, chapterId = chapter.toString())).body<List<BibleChapter>>()
        Napier.v("getChapterBibleIQ: ${chapters.firstOrNull()?.t?.take(100)}", tag = "BB2452")
        BibleIQDataModel.updateBibleChapter(chapters)
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        // httpClient.close()
    }
}