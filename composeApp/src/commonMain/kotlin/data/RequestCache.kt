package data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Shares concurrent requests and keeps a bounded, in-memory cache of successful results. */
internal class RequestCache<K, V>(private val maxEntries: Int) {
    private val mutex = Mutex()
    private val values = LinkedHashMap<K, V>()
    private val pending = mutableMapOf<K, CompletableDeferred<V>>()

    init {
        require(maxEntries >= 0)
    }

    suspend fun get(key: K, refresh: Boolean = false, fetch: suspend () -> V): V {
        var owner = false
        val request = mutex.withLock {
            // A refresh already in progress also satisfies another refresh of the same key.
            pending[key]?.let { return@withLock it }
            if (!refresh && values.containsKey(key)) {
                return values.getValue(key)
            }
            CompletableDeferred<V>().also {
                pending[key] = it
                owner = true
            }
        }
        if (!owner) return request.await()

        try {
            val value = fetch()
            mutex.withLock {
                if (maxEntries > 0) {
                    values.remove(key)
                    values[key] = value
                    while (values.size > maxEntries) values.remove(values.keys.first())
                }
                request.complete(value)
                pending.remove(key)
            }
            return value
        } catch (error: Throwable) {
            // Cancellation must release waiters and allow a later retry.
            withContext(NonCancellable) {
                mutex.withLock {
                    request.completeExceptionally(error)
                    pending.remove(key)
                }
            }
            throw error
        }
    }
}
