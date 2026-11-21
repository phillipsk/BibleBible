package data.shorts

import data.bible.currentTimeMillis
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import kotlin.random.Random

internal const val SWIPE_HINT_MAX_PER_WEEK = 2
internal const val SWIPE_HINT_COOLDOWN_MS = 7L * 24L * 60L * 60L * 1000L

interface SeenWordsStore {
    fun getSeenIds(): Set<String>
    fun markSeen(shortId: String, seenAtMillis: Long)
    fun clearAll()
}

class InMemorySeenWordsStore(initialSeen: Set<String> = emptySet()) : SeenWordsStore {
    private val seen = initialSeen.toMutableSet()
    override fun getSeenIds(): Set<String> = seen.toSet()
    override fun markSeen(shortId: String, seenAtMillis: Long) {
        if (shortId.isNotBlank()) seen.add(shortId)
    }
    override fun clearAll() {
        seen.clear()
    }
}

internal class SqlDelightSeenWordsStore : SeenWordsStore {
    private val database: BibleBibleDatabase? by lazy {
        runCatching {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(it) }
        }.getOrNull()
    }

    override fun getSeenIds(): Set<String> = runCatching {
        database?.bibleBibleDatabaseQueries
            ?.selectSeenBiblicalWordIds()
            ?.executeAsList()
            ?.toSet()
            ?: emptySet()
    }.getOrDefault(emptySet())

    override fun markSeen(shortId: String, seenAtMillis: Long) {
        if (shortId.isBlank()) return
        runCatching {
            database?.bibleBibleDatabaseQueries?.insertSeenBiblicalWord(shortId, seenAtMillis)
        }
    }

    override fun clearAll() {
        runCatching {
            database?.bibleBibleDatabaseQueries?.clearSeenBiblicalWords()
        }
    }
}

internal data class RandomizedDeckResult(
    val orderedShorts: List<BiblicalShortDto>,
    val cycleResetRequired: Boolean,
)

internal object ShortsPlaybackLogic {
    /**
     * Builds a freshly randomized deck prioritizing unseen items first.
     * Once every item in [shorts] has been seen, signals [RandomizedDeckResult.cycleResetRequired]
     * and shuffles the entire catalog so clips can repeat cleanly in a fresh random order.
     * When [resumeShortId] is provided (e.g. returning from "Read Chapter"), that item is placed first.
     */
    fun buildRandomizedUnseenDeck(
        shorts: List<BiblicalShortDto>,
        seenIds: Set<String>,
        resumeShortId: String? = null,
        random: Random = Random.Default,
    ): RandomizedDeckResult {
        if (shorts.isEmpty()) return RandomizedDeckResult(emptyList(), cycleResetRequired = false)

        val catalogIds = shorts.mapTo(HashSet()) { it.id }
        val validSeen = seenIds.filterTo(HashSet()) { it in catalogIds }
        val unseen = shorts.filter { it.id !in validSeen }

        val cycleResetRequired = unseen.isEmpty()
        val baseDeck = if (cycleResetRequired) {
            shorts.shuffled(random)
        } else {
            val shuffledUnseen = unseen.shuffled(random)
            val shuffledSeen = shorts.filter { it.id in validSeen }.shuffled(random)
            shuffledUnseen + shuffledSeen
        }

        val finalDeck = if (!resumeShortId.isNullOrBlank()) {
            val target = baseDeck.find { it.id == resumeShortId }
            if (target != null) {
                listOf(target) + baseDeck.filter { it.id != resumeShortId }
            } else {
                baseDeck
            }
        } else {
            baseDeck
        }

        return RandomizedDeckResult(
            orderedShorts = finalDeck,
            cycleResetRequired = cycleResetRequired,
        )
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
    private val seenStore: SeenWordsStore = SqlDelightSeenWordsStore(),
    private val nowProvider: () -> Long = { currentTimeMillis() },
) {
    fun buildFreshSessionDeck(
        shorts: List<BiblicalShortDto>,
        resumeShortId: String? = null,
        random: Random = Random.Default,
    ): List<BiblicalShortDto> {
        val seenIds = seenStore.getSeenIds()
        val result = ShortsPlaybackLogic.buildRandomizedUnseenDeck(
            shorts = shorts,
            seenIds = seenIds,
            resumeShortId = resumeShortId,
            random = random,
        )
        if (result.cycleResetRequired) {
            seenStore.clearAll()
        }
        return result.orderedShorts
    }

    fun markShortSeen(shortId: String) {
        if (shortId.isNotBlank()) {
            seenStore.markSeen(shortId, nowProvider())
        }
    }

    fun getSeenIds(): Set<String> = seenStore.getSeenIds()

    fun shouldShowTutorial(): Boolean = backing.getInt(KEY_TUTORIAL_SEEN, 0) == 0

    fun markTutorialDismissed() {
        backing.putInt(KEY_TUTORIAL_SEEN, 1)
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
        private const val KEY_TUTORIAL_SEEN = "tutorial_seen_v1"
        private const val KEY_HINT_COUNT = "swipe_hint_count"
        private const val KEY_HINT_WINDOW_START = "swipe_hint_window_start_ms"
    }
}
