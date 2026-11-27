package data.plans

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReadingPlansTest {

    @AfterTest
    fun tearDown() {
        ReadingPlanRepository.resetForTesting()
    }

    @Test
    fun starterCatalogHasExpectedPlansWithoutDisallowedDashes() {
        val starter = BundledReadingPlans.starterPlans
        assertTrue(starter.size >= 5)
        assertTrue(starter.any { it.id == "peace_in_every_season" })
        assertTrue(starter.any { it.id == "proverbs_in_31_days" })
        assertTrue(starter.any { it.id == "psalms_of_comfort" })

        starter.forEach { plan ->
            assertTrue(plan.days.isNotEmpty(), "Plan ${plan.id} must have days")
            assertFalse("\u2014" in plan.title)
            assertFalse("--" in plan.subtitle)
            plan.days.forEach { day ->
                assertTrue(day.readings.isNotEmpty(), "Day ${day.dayNumber} must have readings")
                assertFalse("\u2014" in day.reflectionPrompt)
                assertFalse("--" in day.reflectionPrompt)
            }
        }
    }

    @Test
    fun completionTrackingUpdatesPercentageToggleAndNextPassage() = runTest {
        ReadingPlanRepository.resetForTesting(
            activeId = "psalms_of_comfort",
            completedIds = emptySet(),
        )
        val plan = ReadingPlanRepository.activeOrDefaultPlan()
        assertEquals("psalms_of_comfort", plan.id)
        val initial = ReadingPlanRepository.progressFor(plan)
        assertEquals(0, initial.percentComplete)
        assertEquals(7, initial.totalCount)

        val day1Reading = plan.days[0].readings.first()
        val day2Reading = plan.days[1].readings.first()

        ReadingPlanRepository.markReadingCompleted(
            planId = plan.id,
            dayNumber = 1,
            readingId = day1Reading.id,
            source = "auto_scroll",
        )
        assertTrue(day1Reading.id in ReadingPlanRepository.completedReadingIds)
        val afterOne = ReadingPlanRepository.progressFor(plan)
        assertEquals(1, afterOne.completedCount)
        assertEquals(14, afterOne.percentComplete)
        assertEquals(day2Reading.id, afterOne.nextReading?.id)

        val next = ReadingPlanRepository.nextReadingAfter(plan.id, day1Reading.id)
        assertNotNull(next)
        assertEquals(2, next.dayNumber)
        assertEquals(day2Reading.id, next.reading.id)

        ReadingPlanRepository.toggleReadingCompletion(
            planId = plan.id,
            dayNumber = 1,
            readingId = day1Reading.id,
        )
        assertFalse(day1Reading.id in ReadingPlanRepository.completedReadingIds)
    }
}
