package data.bible

import data.httpClientCloudGateway
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock

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
    private val refreshFetch: suspend (String, DailyVerseData?) -> DailyVerse = { version, _ -> fetch(version) },
) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, DailyVerse>()

    suspend fun get(version: String, refresh: Boolean = false): DailyVerse = mutex.withLock {
        val selected = normalizedTranslation(version)
        val date = today()
        cache.entries.removeAll { it.value.date != date }
        if (!refresh) cache[selected]?.let { return@withLock it }
        val previous = cache[selected]
        val loaded = if (refresh) refreshFetch(selected, previous?.data) else fetch(selected)
        loaded.also { daily ->
            check(daily.data.text.isNotBlank() && daily.data.chapter > 0 && daily.data.verse > 0) {
                "Daily verse unavailable."
            }
            if (refresh && previous != null) {
                check(daily.data.reference != previous.data.reference || daily.date != previous.date) {
                    "The gateway did not return a different verse."
                }
            }
            cache[selected] = daily
        }
    }
}

private val DailyVerseData.reference: String
    get() = "$bookNumber.$chapter.$verse"

internal suspend fun fetchDailyVerse(
    version: String,
    client: HttpClient = httpClientCloudGateway,
    after: DailyVerseData? = null,
): DailyVerse {
    val selected = normalizedTranslation(version)
    val response = client.get(GetVerseOfTheDay(versionId = selected.lowercase(), after = after?.reference)) {
        if (after != null) header(HttpHeaders.CacheControl, "no-cache")
    }
    check(response.status.isSuccess()) {
        "Bible service unavailable (${response.status.value}). Please try again."
    }
    return response.body()
}

internal val dailyVerseRepository by lazy {
    DailyVerseRepository(
        fetch = { version -> fetchDailyVerse(version) },
        refreshFetch = { version, after -> fetchDailyVerse(version, after = after) },
    )
}
