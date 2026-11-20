package data.shorts

import data.apiBible.currentTimeMillis
import kotlin.random.Random

internal const val SWIPE_HINT_MAX_PER_WEEK = 2
internal const val SWIPE_HINT_COOLDOWN_MS = 7L * 24L * 60L * 60L * 1000L

internal object ShortsPlaybackLogic {
    fun orderShortsForUser(
        shorts: List<BiblicalShortDto>,
        savedOrderIds: List<String>,
        random: Random = Random.Default,
    ): Pair<List<BiblicalShortDto>, List<String>> {
        if (shorts.isEmpty()) return emptyList<BiblicalShortDto>() to emptyList()
        if (savedOrderIds.isEmpty()) {
            val shuffled = shorts.shuffled(random)
            return shuffled to shuffled.map { it.id }
        }
        val byId = shorts.associateBy { it.id }
        val restored = savedOrderIds.mapNotNull { byId[it] }
        val seenIds = restored.mapTo(HashSet()) { it.id }
        val additions = shorts.filter { it.id !in seenIds }.shuffled(random)
        val combined = restored + additions
        return combined to combined.map { it.id }
    }

    fun resolveResumeIndex(
        orderedShorts: List<BiblicalShortDto>,
        lastShortId: String?,
    ): Int {
        if (orderedShorts.isEmpty() || lastShortId.isNullOrBlank()) return 0
        val index = orderedShorts.indexOfFirst { it.id == lastShortId }
        return if (index >= 0) index else 0
    }

    fun shouldShowSwipeHint(
        pageIndex: Int,
        shownCount: Int,
        windowStartMs: Long,
        nowMs: Long,
    ): Boolean {
        if (pageIndex !in 0..1) return false
        val windowExpired = windowStartMs <= 0L || (nowMs - windowStartMs) >= SWIPE_HINT_COOLDOWN_MS
        return windowExpired || shownCount < SWIPE_HINT_MAX_PER_WEEK
    }

    fun recordSwipeHintShown(
        shownCount: Int,
        windowStartMs: Long,
        nowMs: Long,
    ): Pair<Int, Long> {
        val windowExpired = windowStartMs <= 0L || (nowMs - windowStartMs) >= SWIPE_HINT_COOLDOWN_MS
        return if (windowExpired) {
            1 to nowMs
        } else {
            (shownCount + 1) to windowStartMs
        }
    }
}

internal class ShortsProgressManager(
    private val backing: ShortsPrefsBacking = platformShortsPrefsBacking(),
    private val nowProvider: () -> Long = { currentTimeMillis() },
) {
    fun getOrCreateOrderedShorts(
        shorts: List<BiblicalShortDto>,
        random: Random = Random.Default,
    ): List<BiblicalShortDto> {
        val rawSaved = backing.getString(KEY_DECK_ORDER).orEmpty()
        val savedIds = rawSaved.split(',').filter { it.isNotBlank() }
        val (ordered, updatedIds) = ShortsPlaybackLogic.orderShortsForUser(shorts, savedIds, random)
        if (updatedIds != savedIds && updatedIds.isNotEmpty()) {
            backing.putString(KEY_DECK_ORDER, updatedIds.joinToString(","))
        }
        return ordered
    }

    fun getLastShortId(): String? = backing.getString(KEY_LAST_SHORT_ID)?.takeIf { it.isNotBlank() }

    fun saveLastShortId(shortId: String) {
        if (shortId.isNotBlank()) {
            backing.putString(KEY_LAST_SHORT_ID, shortId)
        }
    }

    fun shouldShowSwipeHint(pageIndex: Int): Boolean =
        ShortsPlaybackLogic.shouldShowSwipeHint(
            pageIndex = pageIndex,
            shownCount = backing.getInt(KEY_HINT_COUNT, 0),
            windowStartMs = backing.getLong(KEY_HINT_WINDOW_START, 0L),
            nowMs = nowProvider(),
        )

    fun markSwipeHintShown() {
        val (newCount, newStart) = ShortsPlaybackLogic.recordSwipeHintShown(
            shownCount = backing.getInt(KEY_HINT_COUNT, 0),
            windowStartMs = backing.getLong(KEY_HINT_WINDOW_START, 0L),
            nowMs = nowProvider(),
        )
        backing.putInt(KEY_HINT_COUNT, newCount)
        backing.putLong(KEY_HINT_WINDOW_START, newStart)
    }

    companion object {
        private const val KEY_DECK_ORDER = "deck_order"
        private const val KEY_LAST_SHORT_ID = "last_short_id"
        private const val KEY_HINT_COUNT = "swipe_hint_count"
        private const val KEY_HINT_WINDOW_START = "swipe_hint_window_start_ms"
    }
}
