package data.apiBible

import data.apiBible.json.JSON_BIBLES_API_BIBLE_SELECT
import data.apiBible.json.JSON_BOOKS_API_BIBLE
import data.httpClientBibleAPI
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

const val LOCAL_DATA = true
const val USE_BIBLE_API = false

internal suspend fun getBiblesBibleAPI() {
    try {
        BibleAPIDataModel.bibleVersions.value = if (LOCAL_DATA) {
            Json.decodeFromString<BibleAPIBibles>(JSON_BIBLES_API_BIBLE_SELECT)
        } else {
            httpClientBibleAPI.get<GetBiblesAPIBible>(GetBiblesAPIBible()).body<BibleAPIBibles>()
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        // httpClient.close()
    }
}

internal suspend fun getBooksBibleAPI() {
    try {
        val getBooksAPIBible = if (LOCAL_DATA) {
            Json.decodeFromString<BibleAPIBook>(JSON_BOOKS_API_BIBLE)
        } else {
            httpClientBibleAPI.get(GetBooksAPIBible()).body<BibleAPIBook>()
        }
        BibleAPIDataModel.updateBooks(getBooksAPIBible)
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        // httpClient.close()
    }
}

internal suspend fun getChapterBibleAPI(chapterNumber: String, bibleId: String) {
    if (USE_BIBLE_API) {
        try {
            Napier.i("getChapterBibleAPI: $chapterNumber :: bibleId $bibleId", tag = "BB2452")
            Napier.d("start load", tag = "BB2452")
            Napier.d("end load", tag = "BB2452")

            Napier.d("remote api: $chapterNumber", tag = "BB2452")
            fetchChapter(chapterNumber, bibleId)?.let {
                withContext(Dispatchers.Main) {
                    BibleAPIDataModel.updateChapterContent(it)
                    Napier.d("end fetch ui updated", tag = "BB2452")
                }
                Napier.d("start delay", tag = "BB2452")
//            delay(3000)
                Napier.d("end delay", tag = "BB2452")
                Napier.d("start insertVerse", tag = "BB2452")
//                    insertBibleVerses(it)
                Napier.d("end insertVerse", tag = "BB2452")
            }

        } catch (e: Exception) {
            Napier.e("Error: ${e.message}", tag = "BB2452")
        } finally {
            Napier.v("getChapterBibleAPI() :: finally", tag = "BB2455")
        }
    } else {
//        getChapterBibleIQ(book = chapterNumber, version = bibleId)
    }
}

private suspend fun fetchChapter(chapter: String, bibleId: String): ChapterContent? {
    return try {
        Napier.d("start fetch :: $chapter", tag = "BB2452")
        withContext(Dispatchers.IO) {
            httpClientBibleAPI.get<GetChapterAPIBible>(
                GetChapterAPIBible(
                    chapter = chapter,
                    bibleId = bibleId
                )
            )
                .body<ChapterContent>()
        }.also {
            Napier.d("end fetch", tag = "BB2452")
        }
    } catch (e: Exception) {
        "Error fetching chapter: ${chapter + " :: " + e.message}".also {
//            BibleAPIDataModel.updateErrorSnackBar(it)
            Napier.e(it, tag = "BB2452")
        }
        null
    }
}