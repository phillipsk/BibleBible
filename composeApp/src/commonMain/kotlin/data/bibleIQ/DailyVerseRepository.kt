package data.bibleIQ

import data.httpClientBibleIQ
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal val supportedTranslations = setOf("KJV", "BSB", "WEB", "BBE", "RV1909", "SVD")

internal fun normalizedTranslation(value: String?): String =
    value?.trim()?.uppercase()?.takeIf { it in supportedTranslations } ?: "KJV"

@Serializable
internal data class DailyVerseData(
    @SerialName("book_number") val bookNumber: Int,
    @SerialName("chapter_number") val chapter: Int,
    @SerialName("verse_number") val verse: Int,
    val text: String,
    @SerialName("book_usfm") val bookUsfm: String? = null,
)

@Serializable
internal data class DailyVerse(
    val version: String,
    val date: String,
    val data: DailyVerseData,
)

internal fun utcToday(): String =
    Clock.System.now().toLocalDateTime(TimeZone.UTC).date.toString()

/** Bounded per-date session cache; cancellation or errors never poison subsequent retries. */
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
        fetch(selected).also { daily ->
            check(daily.data.text.isNotBlank() && daily.data.chapter > 0 && daily.data.verse > 0) {
                "Daily verse unavailable."
            }
            cache[selected] = daily
        }
    }
}

internal suspend fun fetchDailyVerse(
    version: String,
    client: HttpClient = httpClientBibleIQ,
): DailyVerse {
    val selected = normalizedTranslation(version)
    val response = client.get(GetVerseOfTheDay(versionId = selected.lowercase()))
    check(response.status.isSuccess()) {
        "Bible service unavailable (${response.status.value}). Please try again."
    }
    return response.body()
}

internal val dailyVerseRepository by lazy {
    DailyVerseRepository(fetch = { version -> fetchDailyVerse(version) })
}
