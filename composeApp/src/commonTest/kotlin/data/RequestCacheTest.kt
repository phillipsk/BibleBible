package data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class RequestCacheTest {
    @Test
    fun concurrentRequestsAndRefreshesShareOneFetch() = runTest {
        val cache = RequestCache<String, String>(2)
        var calls = 0
        val release = CompletableDeferred<Unit>()
        val fetch: suspend () -> String = { calls++; release.await(); "summary" }
        val first = async { cache.get("chapter", refresh = true, fetch = fetch) }
        runCurrent()
        val second = async { cache.get("chapter", refresh = true, fetch = fetch) }
        runCurrent()
        assertEquals(1, calls)
        release.complete(Unit)
        assertEquals("summary", first.await())
        assertEquals("summary", second.await())
        assertEquals("summary", cache.get("chapter") { error("Must use cache") })
    }

    @Test
    fun chapterVersionIsPartOfCacheKey() = runTest {
        val cache = RequestCache<Pair<Int, String>, String>(2)
        assertEquals("KJV", cache.get(1 to "kjv") { "KJV" })
        assertEquals("ASV", cache.get(1 to "asv") { "ASV" })
    }

    @Test
    fun failedRefreshKeepsPreviousSuccessAndAllowsRetry() = runTest {
        val cache = RequestCache<String, String>(2)
        cache.get("chapter") { "original" }
        assertFailsWith<IllegalStateException> {
            cache.get("chapter", refresh = true) { error("Quota exceeded") }
        }
        assertEquals("original", cache.get("chapter") { error("Must use cache") })
        assertEquals("replacement", cache.get("chapter", refresh = true) { "replacement" })
    }

    @Test
    fun failedInitialRequestIsNotCached() = runTest {
        val cache = RequestCache<String, String>(2)
        assertFailsWith<IllegalStateException> { cache.get("chapter") { error("Offline") } }
        assertEquals("retry", cache.get("chapter") { "retry" })
    }

    @Test
    fun cacheEvictionBoundsMemory() = runTest {
        val cache = RequestCache<Int, String>(2)
        cache.get(1) { "one" }
        cache.get(2) { "two" }
        cache.get(3) { "three" }
        assertEquals("refetched", cache.get(1) { "refetched" })
    }

    @Test
    fun cancelledOwnerReleasesWaitersAndAllowsRetry() = runTest {
        val cache = RequestCache<String, String>(2)
        val release = CompletableDeferred<Unit>()
        val owner = async { cache.get("chapter") { release.await(); "unused" } }
        runCurrent()
        val waiter = async { cache.get("chapter") { error("Duplicate request") } }
        runCurrent()
        owner.cancel()
        assertFailsWith<CancellationException> { waiter.await() }
        assertEquals("retry", cache.get("chapter") { "retry" })
    }
}
