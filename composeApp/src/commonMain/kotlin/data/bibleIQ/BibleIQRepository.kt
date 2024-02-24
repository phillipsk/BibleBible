package data.bibleIQ

import JSON_BOOKS
import JSON_VERSIONS
import data.httpClientBibleIQ
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    }
}

internal suspend fun getChapterBibleIQ(
    book: Any,
    chapter: Int = 1,
    version: String? = BibleIQDataModel.selectedVersion
): BibleChapterUIState? {
    val bookId = if (book !is Int) BibleIQDataModel.getAPIBibleCardinal(book as String) else book
    Napier.v("getChapterBibleIQ: bookId: $bookId :: chapter $chapter", tag = "BB2452")
    val chapterCount: ChapterCount?
    val chapters: List<BibleChapter> = httpClientBibleIQ.get(
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
    chapterCount = getChapterCountBibleIQ(bookId).await()

    return chapters.firstOrNull()?.b?.toInt()?.let { bookId ->
        BibleChapterUIState(
            id = chapters.firstOrNull()?.id,
            bookId = bookId,
            chapterId = chapters.firstOrNull()?.c?.toInt(),
            text = chapters.joinToString(" ") { "[${it.v}] ${it.t}" },
            chapterList = chapterCount.chapterCount?.let { count ->
                (1..count).toList()
            }
        )
    }
}

private suspend fun getChapterCountBibleIQ(bookId: Int) = coroutineScope {
    async(Dispatchers.IO) {
        httpClientBibleIQ.get(GetChapterCount(bookId)).body<ChapterCount>()
    }
}