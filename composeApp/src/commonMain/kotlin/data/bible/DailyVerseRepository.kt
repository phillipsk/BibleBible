package data.bible

import data.httpClientCloudGateway
import data.shorts.ShortsPrefsBacking
import data.shorts.platformShortsPrefsBacking
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

internal val supportedTranslations = setOf("KJV", "BSB", "WEB", "BBE", "RV1909", "SVD")

internal fun normalizedTranslation(value: String?): String =
    value?.trim()?.uppercase()?.takeIf { it in supportedTranslations } ?: "KJV"

@Serializable
internal data class DailyVerseData(
    @SerialName("book_number") val bookNumber: Int,
    @SerialName("chapter_number") val chapter: Int,
    @SerialName("verse_number") val verse: Int,
    @SerialName("text") val text: String,
    @SerialName("book_usfm") val bookUsfm: String? = null,
)

@Serializable
internal data class DailyVerse(
    @SerialName("version") val version: String,
    @SerialName("date") val date: String,
    @SerialName("data") val data: DailyVerseData,
)

internal fun utcToday(): String =
    Clock.System.now().toLocalDateTime(TimeZone.UTC).date.toString()

private val dailyVerseCacheJson = Json { ignoreUnknownKeys = true }

private fun dailyVersePrefKey(version: String): String = "daily_verse_cache_$version"

private fun DailyVerse.isValid(): Boolean =
    data.text.isNotBlank() && data.chapter > 0 && data.verse > 0

/** Bounded per-date session + offline persisted cache; cancellation or errors never poison subsequent retries. */
internal class DailyVerseRepository(
    private val fetch: suspend (String) -> DailyVerse,
    private val today: () -> String = ::utcToday,
    private val refreshFetch: suspend (String, DailyVerseData?) -> DailyVerse = { version, _ -> fetch(version) },
    private val prefs: ShortsPrefsBacking? = null,
) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, DailyVerse>()

    private fun readPersisted(selected: String): DailyVerse? {
        val raw = runCatching { prefs?.getString(dailyVersePrefKey(selected)) }.getOrNull() ?: return null
        return runCatching { dailyVerseCacheJson.decodeFromString<DailyVerse>(raw) }
            .getOrNull()
            ?.takeIf { it.isValid() }
    }

    private fun writePersisted(selected: String, daily: DailyVerse) {
        runCatching {
            prefs?.putString(dailyVersePrefKey(selected), dailyVerseCacheJson.encodeToString(daily))
        }
    }

    suspend fun get(version: String, refresh: Boolean = false): DailyVerse = mutex.withLock {
        val selected = normalizedTranslation(version)
        val date = today()
        cache.entries.removeAll { it.value.date != date }
        if (!refresh) {
            cache[selected]?.let { return@withLock it }
            readPersisted(selected)?.takeIf { it.date == date }?.let { persisted ->
                cache[selected] = persisted
                return@withLock persisted
            }
        }
        val previous = cache[selected] ?: readPersisted(selected)?.takeIf { it.date == date }
        val loaded = try {
            if (refresh) refreshFetch(selected, previous?.data) else fetch(selected)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!refresh) {
                readPersisted(selected)?.let { offlineFallback ->
                    return@withLock offlineFallback
                }
            }
            throw e
        }
        loaded.also { daily ->
            check(daily.isValid()) {
                "Daily verse unavailable."
            }
            if (refresh && previous != null) {
                check(daily.data.reference != previous.data.reference || daily.date != previous.date) {
                    "The gateway did not return a different verse."
                }
            }
            cache[selected] = daily
            writePersisted(selected, daily)
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
    val response = client.getVerseOfTheDay(versionId = selected.lowercase(), after = after?.reference) {
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
        prefs = platformShortsPrefsBacking(),
    )
}
