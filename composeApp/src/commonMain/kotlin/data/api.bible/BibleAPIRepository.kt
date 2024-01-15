package data.api.bible

import data.api.bible.json.JSON_BIBLES_API_BIBLE
import data.api.bible.json.JSON_BOOKS_API_BIBLE
import data.httpClient
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

const val LOCAL_DATA = true

suspend fun getBiblesBibleAPI() {
    try {
        BibleAPIDataModel.bibleVersions.value = if (LOCAL_DATA) {
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
        BibleAPIDataModel.books.value = getBooksAPIBible
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        httpClient.close()
    }
}


suspend fun getChapterBibleAPI(chapterNumber: String? = null) {
    try {
        val chapter = chapterNumber ?: BibleAPIDataModel.selectedChapter
        Napier.i("getChapterBibleAPI: $chapter")
        withContext(Dispatchers.Main) {
            BibleAPIDataModel.chapter.value =
                httpClient.get(GetChapterAPIBible(chapter = chapter)).body<ChapterContent>()
        }
        Napier.i("getChapterBibleAPI: ${BibleAPIDataModel.chapter.value.data?.cleanedContent?.take(130)}")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        httpClient.close()
    }
}