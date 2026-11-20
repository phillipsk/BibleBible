package data.shorts

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ShortsPlaybackLogicTest {

    @Test
    fun orderShortsForUserShufflesFreshCatalogAndRestoresSavedOrder() {
        val catalog = defaultBiblicalShortsCatalog("")
        val (shuffled, savedIds) = ShortsPlaybackLogic.orderShortsForUser(
            shorts = catalog,
            savedOrderIds = emptyList(),
            random = Random(42),
        )

        assertEquals(catalog.size, shuffled.size)
        assertEquals(catalog.map { it.id }.toSet(), savedIds.toSet())
        assertNotEquals(catalog.map { it.id }, savedIds)

        // Restoring with savedIds returns the exact same deck order
        val (restored, restoredIds) = ShortsPlaybackLogic.orderShortsForUser(
            shorts = catalog,
            savedOrderIds = savedIds,
            random = Random(99),
        )
        assertEquals(savedIds, restoredIds)
        assertEquals(shuffled.map { it.id }, restored.map { it.id })
    }

    @Test
    fun resolveResumeIndexReturnsSavedShortPosition() {
        val catalog = defaultBiblicalShortsCatalog("")
        val thirdId = catalog[2].id
        assertEquals(2, ShortsPlaybackLogic.resolveResumeIndex(catalog, thirdId))
        assertEquals(0, ShortsPlaybackLogic.resolveResumeIndex(catalog, "missing-id"))
        assertEquals(0, ShortsPlaybackLogic.resolveResumeIndex(catalog, null))
    }

    @Test
    fun swipeHintShowsTwicePerWeekOnFirstTwoPagesAndResetsAfterSevenDays() {
        var nowMs = 1_700_000_000_000L
        val backing = InMemoryShortsPrefsBacking()
        val manager = ShortsProgressManager(backing = backing, nowProvider = { nowMs })

        // First lesson: hint should show
        assertTrue(manager.shouldShowSwipeHint(pageIndex = 0))
        manager.markSwipeHintShown()

        // Second lesson: hint should show a second time
        assertTrue(manager.shouldShowSwipeHint(pageIndex = 1))
        manager.markSwipeHintShown()

        // Third view in same week: suppressed
        assertFalse(manager.shouldShowSwipeHint(pageIndex = 0))
        assertFalse(manager.shouldShowSwipeHint(pageIndex = 1))
        // Never shown on page >= 2
        assertFalse(manager.shouldShowSwipeHint(pageIndex = 2))

        // After 7 days, hint re-enables cleanly
        nowMs += SWIPE_HINT_COOLDOWN_MS + 1000L
        assertTrue(manager.shouldShowSwipeHint(pageIndex = 0))
        manager.markSwipeHintShown()
        assertTrue(manager.shouldShowSwipeHint(pageIndex = 1))
        manager.markSwipeHintShown()
        assertFalse(manager.shouldShowSwipeHint(pageIndex = 0))
    }
}
