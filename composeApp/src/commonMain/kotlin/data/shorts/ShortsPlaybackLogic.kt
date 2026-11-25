package data.shorts

import data.appPrefs.DEFAULT_TUTORIAL_REMINDER_DAYS
import data.appPrefs.TutorialReminderPrefs
import data.bible.currentLocalDateKey
import data.bible.currentTimeMillis
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import kotlin.random.Random

internal const val SWIPE_HINT_MAX_PER_WEEK = 2
internal const val SWIPE_HINT_COOLDOWN_MS = 7L * 24L * 60L * 60L * 1000L
internal const val GESTURE_LEGEND_MAX_PER_DAY = 2
internal const val SHORTS_AUTO_ADVANCE_AFTER_PLAYS = 2

internal interface SeenWordsStore {
    fun getSeenIds(): Set<String>
    fun markSeen(shortId: String, seenAtMillis: Long)
    fun clearAll()
}

internal class InMemorySeenWordsStore(initialSeen: Set<String> = emptySet()) : SeenWordsStore {
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

internal data class ShortEngagementRecord(
    val shortId: String,
    val viewCount: Long = 0L,
    val sameSessionReplayCount: Long = 0L,
    val manualRevisitCount: Long = 0L,
    val pauseCount: Long = 0L,
    val totalWatchTimeMs: Long = 0L,
    val lastWatchedAt: Long = 0L,
    val cycleSeen: Boolean = false,
)

internal interface ShortEngagementStore {
    fun getAll(): Map<String, ShortEngagementRecord>
    fun get(shortId: String): ShortEngagementRecord?
    fun upsert(record: ShortEngagementRecord)
    fun resetCycleSeen()
    fun clearAll()
}

internal class InMemoryShortEngagementStore(
    initial: Map<String, ShortEngagementRecord> = emptyMap(),
) : ShortEngagementStore {
    private val records = initial.toMutableMap()

    override fun getAll(): Map<String, ShortEngagementRecord> = records.toMap()

    override fun get(shortId: String): ShortEngagementRecord? = records[shortId]

    override fun upsert(record: ShortEngagementRecord) {
        if (record.shortId.isNotBlank()) {
            records[record.shortId] = record
        }
    }

    override fun resetCycleSeen() {
        records.entries.forEach { (id, record) ->
            if (record.cycleSeen) {
                records[id] = record.copy(cycleSeen = false)
            }
        }
    }

    override fun clearAll() {
        records.clear()
    }
}

internal class SqlDelightShortEngagementStore : ShortEngagementStore {
    private val database: BibleBibleDatabase? by lazy {
        runCatching {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(it) }
        }.getOrNull()
    }

    override fun getAll(): Map<String, ShortEngagementRecord> = runCatching {
        database?.bibleBibleDatabaseQueries
            ?.selectAllShortEngagements()
            ?.executeAsList()
            ?.associate { row ->
                row.short_id to ShortEngagementRecord(
                    shortId = row.short_id,
                    viewCount = row.view_count,
                    sameSessionReplayCount = row.same_session_replay_count,
                    manualRevisitCount = row.manual_revisit_count,
                    pauseCount = row.pause_count,
                    totalWatchTimeMs = row.total_watch_time_ms,
                    lastWatchedAt = row.last_watched_at,
                    cycleSeen = row.cycle_seen != 0L,
                )
            }
            ?: emptyMap()
    }.getOrDefault(emptyMap())

    override fun get(shortId: String): ShortEngagementRecord? = runCatching {
        database?.bibleBibleDatabaseQueries
            ?.selectShortEngagementById(shortId)
            ?.executeAsOneOrNull()
            ?.let { row ->
                ShortEngagementRecord(
                    shortId = row.short_id,
                    viewCount = row.view_count,
                    sameSessionReplayCount = row.same_session_replay_count,
                    manualRevisitCount = row.manual_revisit_count,
                    pauseCount = row.pause_count,
                    totalWatchTimeMs = row.total_watch_time_ms,
                    lastWatchedAt = row.last_watched_at,
                    cycleSeen = row.cycle_seen != 0L,
                )
            }
    }.getOrNull()

    override fun upsert(record: ShortEngagementRecord) {
        if (record.shortId.isBlank()) return
        runCatching {
            database?.bibleBibleDatabaseQueries?.upsertShortEngagement(
                short_id = record.shortId,
                view_count = record.viewCount,
                same_session_replay_count = record.sameSessionReplayCount,
                manual_revisit_count = record.manualRevisitCount,
                pause_count = record.pauseCount,
                total_watch_time_ms = record.totalWatchTimeMs,
                last_watched_at = record.lastWatchedAt,
                cycle_seen = if (record.cycleSeen) 1L else 0L,
            )
        }
    }

    override fun resetCycleSeen() {
        runCatching {
            database?.bibleBibleDatabaseQueries?.resetShortEngagementCycle()
        }
    }

    override fun clearAll() {
        runCatching {
            database?.bibleBibleDatabaseQueries?.clearShortEngagements()
        }
    }
}

internal data class RandomizedDeckResult(
    val orderedShorts: List<BiblicalShortDto>,
    val cycleResetRequired: Boolean,
)

internal object ShortsPlaybackLogic {
    /**
     * Builds an engagement-aware randomized deck:
     * 1. Unseen clips in the current cycle always appear first in randomized order.
     * 2. Seen clips follow, ordered by fewest lifetime views and oldest watch timestamp.
     * 3. Once all clips have been seen at least once in the cycle, signals
     *    [RandomizedDeckResult.cycleResetRequired] and orders repeats by least-watched and
     *    oldest-watched while pushing the most recently watched clip to the back when possible.
     */
    fun buildPrioritizedDeck(
        shorts: List<BiblicalShortDto>,
        engagements: Map<String, ShortEngagementRecord>,
        seenIdsFallback: Set<String> = emptySet(),
        resumeShortId: String? = null,
        random: Random = Random.Default,
    ): RandomizedDeckResult {
        if (shorts.isEmpty()) return RandomizedDeckResult(emptyList(), cycleResetRequired = false)

        val unseen = shorts.filter { item ->
            val rec = engagements[item.id]
            if (rec != null) !rec.cycleSeen else item.id !in seenIdsFallback
        }
        val cycleResetRequired = unseen.isEmpty()

        val unseenIds = unseen.mapTo(HashSet()) { it.id }
        val baseDeck = if (cycleResetRequired) {
            val sortedRepeats = shorts.shuffled(random).sortedWith(
                compareBy<BiblicalShortDto>(
                    { engagements[it.id]?.viewCount ?: if (it.id in seenIdsFallback) 1L else 0L },
                    { engagements[it.id]?.lastWatchedAt ?: 0L },
                )
            )
            if (sortedRepeats.size > 1) {
                val mostRecentId = sortedRepeats
                    .mapNotNull { item -> engagements[item.id]?.lastWatchedAt?.let { ts -> item.id to ts } }
                    .maxByOrNull { it.second }
                    ?.first
                if (mostRecentId != null && sortedRepeats.first().id == mostRecentId) {
                    sortedRepeats.drop(1) + sortedRepeats.first()
                } else {
                    sortedRepeats
                }
            } else {
                sortedRepeats
            }
        } else {
            val shuffledUnseen = unseen.shuffled(random)
            val orderedSeen = shorts
                .filter { it.id !in unseenIds }
                .shuffled(random)
                .sortedWith(
                    compareBy<BiblicalShortDto>(
                        { engagements[it.id]?.viewCount ?: 1L },
                        { engagements[it.id]?.lastWatchedAt ?: 0L },
                    )
                )
            shuffledUnseen + orderedSeen
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

    fun buildRandomizedUnseenDeck(
        shorts: List<BiblicalShortDto>,
        seenIds: Set<String>,
        resumeShortId: String? = null,
        random: Random = Random.Default,
    ): RandomizedDeckResult = buildPrioritizedDeck(
        shorts = shorts,
        engagements = emptyMap(),
        seenIdsFallback = seenIds,
        resumeShortId = resumeShortId,
        random = random,
    )

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

    fun shouldShowGestureLegend(
        shownCount: Int,
        savedDayKey: String?,
        todayKey: String,
    ): Boolean {
        if (savedDayKey.isNullOrBlank() || savedDayKey != todayKey) return true
        return shownCount < GESTURE_LEGEND_MAX_PER_DAY
    }

    fun recordGestureLegendShown(
        shownCount: Int,
        savedDayKey: String?,
        todayKey: String,
    ): Pair<Int, String> {
        return if (savedDayKey.isNullOrBlank() || savedDayKey != todayKey) {
            1 to todayKey
        } else {
            (shownCount + 1) to todayKey
        }
    }

    fun shouldAutoAdvanceAfterPlay(
        completedPlays: Int,
        autoAdvanceAfterPlays: Int = SHORTS_AUTO_ADVANCE_AFTER_PLAYS,
    ): Boolean = completedPlays >= autoAdvanceAfterPlays
}

internal class ShortsProgressManager(
    private val backing: ShortsPrefsBacking = platformShortsPrefsBacking(),
    private val seenStore: SeenWordsStore = SqlDelightSeenWordsStore(),
    engagementStore: ShortEngagementStore? = null,
    private val nowProvider: () -> Long = { currentTimeMillis() },
    private val dayKeyProvider: () -> String = { currentLocalDateKey(nowProvider()) },
) {
    private val resolvedEngagementStore: ShortEngagementStore = engagementStore
        ?: if (seenStore is SqlDelightSeenWordsStore) SqlDelightShortEngagementStore() else InMemoryShortEngagementStore()

    fun buildFreshSessionDeck(
        shorts: List<BiblicalShortDto>,
        resumeShortId: String? = null,
        random: Random = Random.Default,
    ): List<BiblicalShortDto> {
        val engagements = resolvedEngagementStore.getAll()
        val seenIds = seenStore.getSeenIds()
        val result = ShortsPlaybackLogic.buildPrioritizedDeck(
            shorts = shorts,
            engagements = engagements,
            seenIdsFallback = seenIds,
            resumeShortId = resumeShortId,
            random = random,
        )
        if (result.cycleResetRequired) {
            resolvedEngagementStore.resetCycleSeen()
            seenStore.clearAll()
        }
        return result.orderedShorts
    }

    fun markShortSeen(shortId: String) {
        if (shortId.isNotBlank()) {
            recordShortImpression(shortId, isManualRevisit = false)
        }
    }

    fun recordShortImpression(
        shortId: String,
        isManualRevisit: Boolean = false,
    ): ShortEngagementRecord {
        if (shortId.isBlank()) return ShortEngagementRecord(shortId = shortId)
        val now = nowProvider()
        val previous = resolvedEngagementStore.get(shortId) ?: ShortEngagementRecord(shortId = shortId)
        val updated = previous.copy(
            viewCount = previous.viewCount + 1L,
            manualRevisitCount = previous.manualRevisitCount + if (isManualRevisit) 1L else 0L,
            lastWatchedAt = now,
            cycleSeen = true,
        )
        resolvedEngagementStore.upsert(updated)
        seenStore.markSeen(shortId, now)
        return updated
    }

    fun recordShortLoopReplay(shortId: String): ShortEngagementRecord? {
        if (shortId.isBlank()) return null
        val now = nowProvider()
        val previous = resolvedEngagementStore.get(shortId) ?: ShortEngagementRecord(shortId = shortId, viewCount = 1L, cycleSeen = true)
        val updated = previous.copy(
            sameSessionReplayCount = previous.sameSessionReplayCount + 1L,
            lastWatchedAt = now,
            cycleSeen = true,
        )
        resolvedEngagementStore.upsert(updated)
        return updated
    }

    fun recordShortPause(shortId: String): ShortEngagementRecord? {
        if (shortId.isBlank()) return null
        val previous = resolvedEngagementStore.get(shortId) ?: ShortEngagementRecord(shortId = shortId, viewCount = 1L, cycleSeen = true)
        val updated = previous.copy(
            pauseCount = previous.pauseCount + 1L,
        )
        resolvedEngagementStore.upsert(updated)
        return updated
    }

    fun recordShortWatchTime(shortId: String, watchTimeMs: Long): ShortEngagementRecord? {
        if (shortId.isBlank()) return null
        val clamped = watchTimeMs.coerceIn(0L, 3_600_000L)
        val previous = resolvedEngagementStore.get(shortId) ?: ShortEngagementRecord(shortId = shortId, viewCount = 1L, cycleSeen = true)
        if (clamped <= 0L) return previous
        val updated = previous.copy(
            totalWatchTimeMs = previous.totalWatchTimeMs + clamped,
            lastWatchedAt = nowProvider(),
        )
        resolvedEngagementStore.upsert(updated)
        return updated
    }

    fun getEngagement(shortId: String): ShortEngagementRecord? =
        resolvedEngagementStore.get(shortId)

    fun getAllEngagements(): Map<String, ShortEngagementRecord> =
        resolvedEngagementStore.getAll()

    fun unseenCount(catalogIds: Collection<String>): Int {
        if (catalogIds.isEmpty()) return 0
        val engagements = resolvedEngagementStore.getAll()
        val seenIds = seenStore.getSeenIds()
        return catalogIds.count { id ->
            val rec = engagements[id]
            if (rec != null) !rec.cycleSeen else id !in seenIds
        }
    }

    fun getSeenIds(): Set<String> {
        val fromEngagement = resolvedEngagementStore.getAll()
            .values
            .filter { it.cycleSeen }
            .mapTo(HashSet()) { it.shortId }
        return fromEngagement + seenStore.getSeenIds()
    }

    private val tutorialPrefs = TutorialReminderPrefs(backing, KEY_TUTORIAL_SEEN, nowProvider)

    fun shouldShowTutorial(
        enabled: Boolean = true,
        reminderDays: Int = DEFAULT_TUTORIAL_REMINDER_DAYS,
    ): Boolean = tutorialPrefs.shouldShow(enabled, reminderDays)

    fun markTutorialDismissed() = tutorialPrefs.markDismissed()

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

    fun shouldShowGestureLegend(): Boolean =
        ShortsPlaybackLogic.shouldShowGestureLegend(
            shownCount = backing.getInt(KEY_GESTURE_LEGEND_COUNT, 0),
            savedDayKey = backing.getString(KEY_GESTURE_LEGEND_DAY),
            todayKey = dayKeyProvider(),
        )

    fun markGestureLegendShown() {
        val (newCount, dayKey) = ShortsPlaybackLogic.recordGestureLegendShown(
            shownCount = backing.getInt(KEY_GESTURE_LEGEND_COUNT, 0),
            savedDayKey = backing.getString(KEY_GESTURE_LEGEND_DAY),
            todayKey = dayKeyProvider(),
        )
        backing.putInt(KEY_GESTURE_LEGEND_COUNT, newCount)
        backing.putString(KEY_GESTURE_LEGEND_DAY, dayKey)
    }

    companion object {
        private const val KEY_TUTORIAL_SEEN = "tutorial_seen_v1"
        private const val KEY_HINT_COUNT = "swipe_hint_count"
        private const val KEY_HINT_WINDOW_START = "swipe_hint_window_start_ms"
        private const val KEY_GESTURE_LEGEND_COUNT = "gesture_legend_count"
        private const val KEY_GESTURE_LEGEND_DAY = "gesture_legend_day"
    }
}
