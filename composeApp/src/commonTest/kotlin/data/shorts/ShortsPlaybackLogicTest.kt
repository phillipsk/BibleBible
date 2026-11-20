package data.shorts

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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
}
