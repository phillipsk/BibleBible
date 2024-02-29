package data.bibleIQ

import JSON_BOOKS
import JSON_VERSIONS
import data.httpClientBibleIQ
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    version: String = BibleIQDataModel.selectedVersion
) {
    val bookId = if (book !is Int) BibleIQDataModel.getAPIBibleCardinal(book as String) else book
    Napier.v("getChapterBibleIQ: bookId: $bookId :: chapter $chapter", tag = "IQ093")
    val chapters: List<BibleChapter>
    var chapterCount: ChapterCount?

    Napier.i("getChapterBibleAPI: $chapter :: version $version", tag = "IQ093")
    Napier.d("start load", tag = "IQ093")
    val cachedData = loadVerseData(chapter.toString(), version)
    Napier.d("end load", tag = "IQ093")
    Napier.v("cachedData value ${cachedData?.firstOrNull()?.v?.take(100)}", tag = "IQ093")

    if (cachedData == null || cachedData.firstOrNull()?.v.isNullOrEmpty()) {
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
                tag = "IQ093"
            )
            chapterCount = getChapterCountBibleIQ(bookId).await()

            withContext(Dispatchers.Main) {
                Napier.v("getChapterBibleIQ :: update UI", tag = "IQ093")
                BibleIQDataModel.updateBibleChapter(chapters, chapterCount)
            }
            chapters.forEach {
                insertBibleVerses(it, version)
            }
        }
    } else {
        chapterCount = getChapterCountBibleIQ(bookId).await()

        withContext(Dispatchers.Main) {
            Napier.v("getChapterBibleIQ :: update UI", tag = "IQ093")
            BibleIQDataModel.updateBibleChapter(cachedData, chapterCount)
        }
    }

}

private suspend fun getChapterCountBibleIQ(bookId: Int) = coroutineScope {
    async(Dispatchers.IO) {
        httpClientBibleIQ.get(GetChapterCount(bookId)).body<ChapterCount>()
    }
}

private suspend fun insertBibleVerses(chapterContent: BibleChapter, version: String) {
    return try {
        withContext(Dispatchers.IO) {
            Napier.d("inside insert load before delay", tag = "IQ093")
            Napier.d("inside insert load end delay", tag = "IQ093")
            Napier.v("insertVerse chapterContent  :: ${chapterContent.id}", tag = "IQ093")
            val database = BibleBibleDatabase(driver = DriverFactory.createDriver())
            chapterContent.let {
                database.bibleBibleDatabaseQueries.insertVerse(
                    uuid = it.id + version.lowercase(),
                    id = it.id ?: "",
                    b = it.b ?: "",
                    c = it.c ?: "",
                    v = it.v ?: "",
                    t = it.t ?: "",
                    version = version.lowercase()
                )
                Napier.v("insertVerse chapterContent  :: finish", tag = "IQ093")
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ093")
    }
}

private suspend fun loadVerseData(selectedChapter: String, version: String): List<BibleChapter>? {
    return try {
        withContext(Dispatchers.IO) {
            Napier.d("inside start load before delay", tag = "IQ093")
            // delay(3000)
            Napier.d("inside start load end delay", tag = "IQ093")

            val database = BibleBibleDatabase(driver = DriverFactory.createDriver())
            val bibleQueries = database.bibleBibleDatabaseQueries
                .selectVersesById(selectedChapter, version.lowercase()).executeAsOneOrNull()

            Napier.v("bibleQueries selectedChapter $selectedChapter :: hello world", tag = "IQ093")
            Napier.v("bibleQueries ${bibleQueries?.v?.take(100)}", tag = "IQ093")

            bibleQueries?.let {
                listOf(
                    BibleChapter(id = it.id, b = it.b, c = it.c, v = it.v, t = it.t)
                )
            }.also {
                Napier.d("UI ready to update from DB query end", tag = "IQ093")
            }
        }
    } catch (e: Exception) {
        Napier.e("Error: ${e.message}", tag = "IQ093")
        null
    }
}