package data.bibleIQ

import data.httpClientBibleIQ
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** UI/persisted abbreviations stay stable; only remote addressing changes. */
internal val supportedTranslations = linkedMapOf(
    "KJV" to "eng_kjv", "BBE" to "eng_bbe", "RV1909" to "spa_rv",
    "SVD" to "arb_svd", "ASV" to "eng_asv"
)
internal fun normalizedTranslation(value: String): String =
    value.uppercase().takeIf { it in supportedTranslations } ?: "KJV"
internal fun remoteTranslation(value: String): String = supportedTranslations.getValue(normalizedTranslation(value))
internal fun v2CacheVersion(value: String): String = "v2:${remoteTranslation(value)}"

@Serializable internal data class V2Verse(
    @SerialName("book_number") val bookNumber: Int,
    @SerialName("chapter_number") val chapter: Int,
    @SerialName("verse_number") val verse: Int,
    val text: String,
    @SerialName("book_usfm") val bookUsfm: String? = null,
)
@Serializable internal data class V2Passage(val version: String, val data: List<V2Verse>)
@Serializable internal data class V2BookMeta(@SerialName("chapter_count") val chapterCount: Long)
@Serializable internal data class DailyVerse(val version: String, val date: String, val data: V2Verse)

internal class IqBibleV2(private val client: HttpClient) {
    private suspend inline fun <reified T> read(path: String): T {
        val response = client.get("/api/v2/$path")
        check(response.status.isSuccess()) { "Bible service unavailable (${response.status.value}). Please try again." }
        return response.body()
    }

    suspend fun chapter(bookUsfm: String, canonicalBookId: Int, chapter: Int, version: String): List<BibleChapter> {
        require(bookUsfm.matches(Regex("[A-Z0-9]{3}")) && canonicalBookId in 1..66 && chapter > 0)
        val remoteVersion = remoteTranslation(version)
        val response = read<V2Passage>("bibles/$remoteVersion/$bookUsfm/$chapter")
        check(response.version == remoteVersion && response.data.isNotEmpty()) { "No scripture returned for this chapter." }
        return response.data.map { verse ->
            check(verse.chapter == chapter && verse.verse > 0 && verse.text.isNotBlank()) { "Invalid scripture response." }
            // Provider-local book numbers/row ids are not our canonical cache/navigation ids.
            BibleChapter("$canonicalBookId-$chapter-${verse.verse}", canonicalBookId.toString(), chapter.toString(), verse.verse.toString(), verse.text)
        }
    }

    suspend fun chapterCount(bookUsfm: String, version: String): ChapterCount {
        val meta = read<V2BookMeta>("bibles/${remoteTranslation(version)}/$bookUsfm/meta")
        check(meta.chapterCount in 1..150) { "Invalid chapter count." }
        return ChapterCount(meta.chapterCount)
    }

    suspend fun dailyVerse(version: String): DailyVerse {
        val remoteVersion = remoteTranslation(version)
        val response = read<DailyVerse>("bibles/$remoteVersion/verse-of-the-day")
        check(response.version == remoteVersion && response.data.text.isNotBlank() &&
            response.data.chapter > 0 && response.data.verse > 0 &&
            response.data.bookUsfm?.matches(Regex("[A-Z0-9]{3}")) == true) { "Daily verse unavailable." }
        return response
    }
}

internal fun utcToday(): String = Clock.System.now().toLocalDateTime(TimeZone.UTC).date.toString()

/** Bounded session cache; cancellation/errors never poison the next attempt. */
internal class DailyVerseRepository(
    private val fetch: suspend (String) -> DailyVerse,
    private val today: () -> String = ::utcToday,
) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, DailyVerse>()
    suspend fun get(version: String): DailyVerse = mutex.withLock {
        val selected = normalizedTranslation(version)
        val date = today()
        cache.entries.removeAll { it.value.date != date }
        cache[selected]?.let { return@withLock it }
        fetch(selected).also {
            check(it.date == today()) { "Daily verse is not available for today. Please try again." }
            cache[selected] = it
        }
    }
}

internal val iqBibleV2 by lazy { IqBibleV2(httpClientBibleIQ) }
internal val dailyVerseRepository by lazy { DailyVerseRepository(iqBibleV2::dailyVerse) }
