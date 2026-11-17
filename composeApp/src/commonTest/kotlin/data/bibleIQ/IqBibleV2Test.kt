package data.bibleIQ

import data.AppHttpClientConfig
import data.createHttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class IqBibleV2Test {
    private fun client(body: String, status: HttpStatusCode = HttpStatusCode.OK, inspect: (io.ktor.client.request.HttpRequestData) -> Unit = {}) =
        createHttpClient(AppHttpClientConfig("v2-test.p.rapidapi.com", "X-RapidAPI-Key", "test-only", rapidApiHost = "v2-test.p.rapidapi.com"),
            engine = MockEngine { request ->
                inspect(request)
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }, enableLogging = false)

    @Test fun bbeRequestUsesV2PathAndMapsLocalBookNumbersToCanonicalIds() = runTest {
        val client = client("""{"version":"eng_bbe","data":[{"id":900,"book_number":44,"chapter_number":3,"verse_number":16,"text":"A BBE verse","extra":true}]}""") {
            assertEquals("/bibles/eng_bbe/JHN/3", it.url.encodedPath)
            assertEquals("v2-test.p.rapidapi.com", it.headers["X-RapidAPI-Host"])
            assertEquals("test-only", it.headers["X-RapidAPI-Key"])
            assertTrue(it.url.parameters.isEmpty())
        }
        try {
            val verse = IqBibleV2(client).chapter("JHN", 43, 3, "BBE").single()
            assertEquals("43", verse.b)
            assertEquals("43-3-16", verse.id)
            assertEquals("A BBE verse", verse.t)
        } finally { client.close() }
    }

    @Test fun chapterMetadataIsTranslationSpecific() = runTest {
        val client = client("""{"chapter_count":21,"chapters":[]}""") {
            assertEquals("/bibles/arb_svd/JHN/meta", it.url.encodedPath)
        }
        try { assertEquals(21, IqBibleV2(client).chapterCount("JHN", "SVD").chapterCount) }
        finally { client.close() }
    }

    @Test fun errorsEmptyChaptersAndWrongTranslationsAreRejected() = runTest {
        for ((body, status) in listOf(
            """{"message":"not subscribed"}""" to HttpStatusCode.Forbidden,
            """{"status":"error"}""" to HttpStatusCode.TooManyRequests,
            """{"version":"eng_kjv","data":[]}""" to HttpStatusCode.OK,
            """{"version":"eng_asv","data":[{"book_number":1,"chapter_number":1,"verse_number":1,"text":"Wrong translation"}]}""" to HttpStatusCode.OK,
        )) {
            val client = client(body, status)
            try { assertFailsWith<IllegalStateException> { IqBibleV2(client).chapter("GEN", 1, 1, "KJV") } }
            finally { client.close() }
        }
    }

    @Test fun dailyVerseDecodesObjectAndUsfmReference() = runTest {
        val client = client("""{"date":"2026-11-17","version":"eng_kjv","data":{"id":42,"book_number":43,"book_usfm":"JHN","chapter_number":3,"verse_number":16,"text":"Daily text"}}""") {
            assertEquals("/bibles/eng_kjv/verse-of-the-day", it.url.encodedPath)
        }
        try { assertEquals("JHN", IqBibleV2(client).dailyVerse("KJV").data.bookUsfm) }
        finally { client.close() }
    }

    @Test fun removedPreferencesFallBackToKjvAndCacheIsSeparatedFromV1() {
        assertEquals("KJV", normalizedTranslation("WEB"))
        assertEquals("BBE", normalizedTranslation("bbe"))
        assertEquals("v2:eng_bbe", v2CacheVersion("BBE"))
        assertEquals(listOf("KJV", "BBE", "RV1909", "SVD", "ASV"), supportedTranslations.keys.toList())
    }

    @Test fun dailyVerseCacheExpiresAtUtcDateChangeAndSeparatesTranslations() = runTest {
        var date = "2026-11-17"
        var calls = 0
        val repository = DailyVerseRepository({ version ->
            calls++
            DailyVerse(remoteTranslation(version), date, V2Verse(43, 3, 16, version, "JHN"))
        }, { date })
        assertEquals("KJV", repository.get("KJV").data.text)
        repository.get("kjv")
        assertEquals(1, calls)
        assertEquals("BBE", repository.get("BBE").data.text)
        assertEquals(2, calls)
        date = "2026-11-18"
        assertEquals(date, repository.get("KJV").date)
        assertEquals(3, calls)
    }

    @Test fun failedAndCancelledDailyRequestsCanBeRetried() = runTest {
        var calls = 0
        val repository = DailyVerseRepository({
            calls++
            when (calls) {
                1 -> throw IllegalStateException("offline")
                2 -> throw CancellationException("left home")
                else -> DailyVerse("eng_kjv", "2026-11-17", V2Verse(1, 1, 1, "Success", "GEN"))
            }
        }, { "2026-11-17" })
        assertFailsWith<IllegalStateException> { repository.get("KJV") }
        assertFailsWith<CancellationException> { repository.get("KJV") }
        assertEquals("Success", repository.get("KJV").data.text)
    }

    @Test fun yesterdayIsNotPresentedAsTodaysVerse() = runTest {
        val repository = DailyVerseRepository({ DailyVerse("eng_kjv", "2026-11-16", V2Verse(1, 1, 1, "Old", "GEN")) }, { "2026-11-17" })
        assertFailsWith<IllegalStateException> { repository.get("KJV") }
    }
}
