package data.bibleIQ

import JSON_BOOKS
import JSON_VERSIONS
import data.httpClientBibleIQ
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

const val LOCAL_DATA = true
internal suspend fun getBooksBibleIQ() {
    try {
        val books = if (LOCAL_DATA) {
            Json.decodeFromString<List<BibleIQBook>>(JSON_BOOKS)
        } else {
            httpClientBibleIQ.get(GetBooks()).body<List<BibleIQBook>>()
        }
        BibleIQDataModel.updateBibleBooks(BibleIQBooks(books))
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ092")
    } finally {
        // httpClient.close()
    }
}

internal suspend fun getVersionsBibleIQ() {
    try {
        val versions = if (LOCAL_DATA) {
            Json.decodeFromString<List<BibleIQVersion>>(JSON_VERSIONS)
        } else {
            httpClientBibleIQ.get(GetVersions()).body<List<BibleIQVersion>>()
        }
        BibleIQDataModel.updateBibleVersions(BibleIQVersions(versions))
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ092")
    } finally {
        // httpClient.close()
    }
}

internal suspend fun getChapterBibleIQ(
    book: Any,
    chapter: Int = 1,
    version: String? = BibleIQDataModel.selectedVersion
) {
    val bookId = if (book !is Int) BibleIQDataModel.getAPIBibleCardinal(book as String) else book
    Napier.v("getChapterBibleIQ: bookId: $bookId :: chapter $chapter", tag = "BB2452")
    val chapters: List<BibleChapter>
    withContext(Dispatchers.IO) {
        chapters =
            httpClientBibleIQ.get(
                GetChapter(
                    bookId = bookId,
                    chapterId = chapter.toString(),
                    versionId = version?.lowercase()
                )
            ).body<List<BibleChapter>>()
        Napier.v(
            "getChapterBibleIQ: ${chapters.firstOrNull()?.t?.take(100)}",
            tag = "BB2452"
        )

    }
    withContext(Dispatchers.Main) {
        Napier.v("getChapterBibleIQ :: update UI", tag = "BB2452")
        BibleIQDataModel.updateBibleChapter(chapters)
    }
}