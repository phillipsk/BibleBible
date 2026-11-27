package data.shorts

import data.analytics.AnalyticsTracker
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShortsPlaybackLogicTest {

    @Test
    fun buildRandomizedUnseenDeckNeverRepeatsSeenUntilAllViewedThenResetsCycle() {
        val catalog = defaultBiblicalShortsCatalog("").take(4)
        val seenStore = InMemorySeenWordsStore()
        val manager = ShortsProgressManager(
            backing = InMemoryShortsPrefsBacking(),
            seenStore = seenStore,
        )

        // Visit 1: all 4 unseen -> shuffled, pick first and mark seen
        val deck1 = manager.buildFreshSessionDeck(catalog, random = Random(11))
        assertEquals(4, deck1.size)
        val firstSeen = deck1.first().id
        manager.markShortSeen(firstSeen)

        // Visit 2: firstSeen must NOT be first (unseen 3 are placed ahead of seen items)
        val deck2 = manager.buildFreshSessionDeck(catalog, random = Random(22))
        assertNotEquals(firstSeen, deck2.first().id)
        assertTrue(deck2.take(3).none { it.id == firstSeen })

        // Mark all remaining 3 seen
        deck2.take(3).forEach { manager.markShortSeen(it.id) }
        assertEquals(4, manager.getSeenIds().size)

        // Visit 3: all 4 have been seen -> cycle automatically resets and reshuffles full catalog
        val deck3 = manager.buildFreshSessionDeck(catalog, random = Random(33))
        assertEquals(4, deck3.size)
        assertEquals(0, manager.getSeenIds().size) // cleared for fresh cycle
    }

    @Test
    fun buildRandomizedUnseenDeckHonorsResumeShortIdWhenReturningFromChapter() {
        val catalog = defaultBiblicalShortsCatalog("").take(5)
        val targetId = catalog[3].id
        val result = ShortsPlaybackLogic.buildRandomizedUnseenDeck(
            shorts = catalog,
            seenIds = setOf(targetId),
            resumeShortId = targetId,
            random = Random(7),
        )
        assertEquals(targetId, result.orderedShorts.first().id)
        assertFalse(result.cycleResetRequired)
    }

    @Test
    fun tutorialAndWeeklySwipeHintPersistAndResetCleanly() {
        var nowMs = 1_700_000_000_000L
        val backing = InMemoryShortsPrefsBacking()
        val manager = ShortsProgressManager(
            backing = backing,
            seenStore = InMemorySeenWordsStore(),
            nowProvider = { nowMs },
        )

        // Tutorial shows once until dismissed
        assertTrue(manager.shouldShowTutorial())
        manager.markTutorialDismissed()
        assertFalse(manager.shouldShowTutorial())

        // Weekly swipe hint: shows twice per 7-day window on pages 0..1
        assertTrue(manager.shouldShowSwipeHint(pageIndex = 0))
        manager.markSwipeHintShown()
        assertTrue(manager.shouldShowSwipeHint(pageIndex = 1))
        manager.markSwipeHintShown()
        assertFalse(manager.shouldShowSwipeHint(pageIndex = 0))

        // After 7 days, hint re-enables cleanly
        nowMs += SWIPE_HINT_COOLDOWN_MS + 1000L
        assertTrue(manager.shouldShowSwipeHint(pageIndex = 0))
    }

    @Test
    fun gestureLegendShowsForTwoReelsPerDayThenResetsNextDay() {
        var currentDay = "2026-11-24"
        val backing = InMemoryShortsPrefsBacking()
        val manager = ShortsProgressManager(
            backing = backing,
            seenStore = InMemorySeenWordsStore(),
            dayKeyProvider = { currentDay },
        )

        // Reel 1 today
        assertTrue(manager.shouldShowGestureLegend())
        manager.markGestureLegendShown()

        // Reel 2 today
        assertTrue(manager.shouldShowGestureLegend())
        manager.markGestureLegendShown()

        // Reel 3+ today -> hidden
        assertFalse(manager.shouldShowGestureLegend())
        assertFalse(manager.shouldShowGestureLegend())

        // Next day -> reappears for 2 reels again
        currentDay = "2026-11-25"
        assertTrue(manager.shouldShowGestureLegend())
        manager.markGestureLegendShown()
        assertTrue(manager.shouldShowGestureLegend())
        manager.markGestureLegendShown()
        assertFalse(manager.shouldShowGestureLegend())
    }

    @Test
    fun autoAdvanceTriggersAfterTwoCompletedPlays() {
        assertFalse(ShortsPlaybackLogic.shouldAutoAdvanceAfterPlay(0))
        assertFalse(ShortsPlaybackLogic.shouldAutoAdvanceAfterPlay(1))
        assertTrue(ShortsPlaybackLogic.shouldAutoAdvanceAfterPlay(2))
        assertTrue(ShortsPlaybackLogic.shouldAutoAdvanceAfterPlay(3))
    }

    @Test
    fun prioritizedDeckShowsLeastWatchedRepeatsAndPreservesLifetimeEngagementAcrossCycleReset() = runTest {
        AnalyticsTracker.remoteDispatchEnabled = false
        AnalyticsTracker.resetForTesting()
        var nowMs = 1_700_000_000_000L
        val catalog = defaultBiblicalShortsCatalog("").take(4)
        val engagementStore = InMemoryShortEngagementStore()
        val manager = ShortsProgressManager(
            backing = InMemoryShortsPrefsBacking(),
            seenStore = InMemorySeenWordsStore(),
            engagementStore = engagementStore,
            nowProvider = { nowMs },
        )

        // Watch all 4 clips, pausing and replaying clip 0 extra times
        catalog.forEachIndexed { idx, item ->
            nowMs += 10_000L
            manager.recordShortImpression(item.id, isManualRevisit = idx == 0)
            manager.recordShortWatchTime(item.id, 12_000L + idx * 1_000L)
            if (idx == 0) {
                manager.recordShortPause(item.id)
                manager.recordShortLoopReplay(item.id)
                manager.recordShortImpression(item.id, isManualRevisit = true)
            }
        }
        assertEquals(0, manager.unseenCount(catalog.map { it.id }))

        // Re-entering Shorts resets cycle_seen without losing lifetime view/pause/replay/watch-time counts
        val repeatDeck = manager.buildFreshSessionDeck(catalog, random = Random(99))
        assertEquals(4, repeatDeck.size)
        // Most-recently-watched clip (catalog[3]) must not be placed first when other clips have fewer views
        assertNotEquals(catalog.last().id, repeatDeck.first().id)
        // Clip 0 has highest viewCount (2), so least-watched clips appear before clip 0
        assertEquals(catalog[0].id, repeatDeck.last().id)

        val clip0Stats = manager.getEngagement(catalog[0].id)
        assertNotNull(clip0Stats)
        assertEquals(2L, clip0Stats.viewCount)
        assertEquals(2L, clip0Stats.manualRevisitCount)
        assertEquals(1L, clip0Stats.sameSessionReplayCount)
        assertEquals(1L, clip0Stats.pauseCount)
        assertEquals(12_000L, clip0Stats.totalWatchTimeMs)
        assertFalse(clip0Stats.cycleSeen)

        AnalyticsTracker.trackShortViewed(
            shortItem = catalog[0],
            advanceSource = "manual_back",
            viewCount = clip0Stats.viewCount,
            isRepeat = true,
            isManualRevisit = true,
            unseenRemaining = 2,
        )
        AnalyticsTracker.trackShortWatchSummary(
            shortItem = catalog[0],
            watchTimeMs = 12_000L,
            completedLoops = 1,
            pauseCountInSession = 1,
            wasManualRevisit = true,
            lifetimeViews = clip0Stats.viewCount,
            lifetimeWatchTimeMs = clip0Stats.totalWatchTimeMs,
            exitReason = "swiped",
        )
        val events = AnalyticsTracker.snapshotEvents()
        assertTrue(events.any { it.name == "short_viewed" && it.properties["isManualRevisit"] == "true" })
        assertTrue(events.any { it.name == "short_watch_summary" && it.properties["watchTimeMs"] == "12000" })
    }
}
