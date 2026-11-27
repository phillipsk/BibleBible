package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import data.config.CloudSessionManager
import data.plans.ActivePlanReadingContext
import data.plans.ReadingPlanRepository
import ui.plans.PlanReaderCompletionBar
import ui.plans.PlanReaderTopStrip
import ui.plans.ReadingPlanBannerCard
import ui.plans.ReadingPlanTwinCard
import ui.plans.ReadingPlansScreen
import ui.search.InlineElasticSearchBox
import ui.search.InlineElasticSearchResults
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SearchAndReadingPlansUiTest {

    @AfterTest
    fun tearDown() {
        CloudSessionManager.resetForTesting()
        ReadingPlanRepository.resetForTesting()
    }

    @Test
    fun inlineElasticSearchFiltersPinnedStripMasonryWallAndVerses() = runComposeUiTest {
        var jumpedRef = ""

        var query by mutableStateOf("")
        var focused by mutableStateOf(false)

        setContent {
            BibleBibleTheme {
                Box(modifier = Modifier.size(412.dp, 980.dp)) {
                    Column {
                        InlineElasticSearchBox(
                            query = query,
                            isFocused = focused,
                            onQueryChange = { query = it },
                            onFocusChanged = { focused = it },
                        )
                        if (query.trim().isNotEmpty()) {
                            InlineElasticSearchResults(
                                query = query,
                                onSelectBook = {},
                                onJumpToReference = { bookId, _, _, chapter, verseStart, _, _ ->
                                    jumpedRef = "$bookId $chapter:$verseStart"
                                },
                                onOpenPlan = {},
                            )
                        }
                    }
                }
            }
        }

        onNodeWithTag("elastic_search_box").assertIsDisplayed()
        onNodeWithTag("inline_search_input").performTextInput("John 3:16")
        waitForIdle()

        onNodeWithTag("inline_search_results").assertIsDisplayed()
        onNodeWithTag("search_pinned_book_strip").assertIsDisplayed()
        onNodeWithTag("search_book_tile_JHN").assertIsDisplayed()
        onNodeWithTag("search_direct_ref_card").assertIsDisplayed()
        onNodeWithTag("search_masonry_wall").assertIsDisplayed()
        onNodeWithTag("search_direct_ref_card").performClick()
        runOnIdle { assertEquals("JHN 3:16", jumpedRef) }

        onNodeWithTag("inline_search_clear_btn").performClick()
        onNodeWithTag("inline_search_input").performTextInput("peace")
        waitForIdle()

        onNodeWithTag("spotlight_filter_topics").performClick()
        onNodeWithTag("search_masonry_wall").assertIsDisplayed()
        onNodeWithTag("search_ai_routing_pill").assertIsDisplayed()
    }

    @Test
    fun readingPlansCardsScreenToggleAndReaderBarsOperateCleanly() = runComposeUiTest {
        ReadingPlanRepository.resetForTesting(activeId = "psalms_of_comfort")
        var openCount = 0
        var openedReadingId = ""
        var backToPlanTapped = false
        var nextTapped = false

        val plan = ReadingPlanRepository.activeOrDefaultPlan()
        val firstDay = plan.days.first()
        val firstReading = firstDay.readings.first()
        val ctx = ActivePlanReadingContext(
            planId = plan.id,
            planTitle = plan.title,
            dayNumber = firstDay.dayNumber,
            dayTitle = firstDay.title,
            reading = firstReading,
            reflectionPrompt = firstDay.reflectionPrompt,
            totalReadingsInPlan = plan.days.size,
        )

        setContent {
            BibleBibleTheme {
                Box(modifier = Modifier.size(412.dp, 1600.dp)) {
                    Column {
                        ReadingPlanBannerCard(onOpenPlans = { openCount++ })
                        ReadingPlanTwinCard(onOpenPlans = { openCount++ })
                        PlanReaderTopStrip(context = ctx, scrollProgress = 0.65f)
                        PlanReaderCompletionBar(
                            visible = true,
                            hasNextPassage = true,
                            onBackToPlan = { backToPlanTapped = true },
                            onNextPassage = { nextTapped = true },
                        )
                        ReadingPlansScreen(
                            onBack = {},
                            onOpenReading = { openedReadingId = it.reading.id },
                            initialPlanId = "psalms_of_comfort",
                        )
                    }
                }
            }
        }

        onNodeWithTag("reading_plan_entry_card").performClick()
        onNodeWithTag("reading_plan_twin_card").performClick()
        runOnIdle { assertEquals(2, openCount) }

        onNodeWithTag("plan_reader_top_strip").assertIsDisplayed()
        onNodeWithTag("plan_reader_status_pill").assertTextContains("Scroll to finish")
        onNodeWithTag("plan_reader_next_btn").performClick()
        onNodeWithText("Plan").performClick()
        runOnIdle {
            assertTrue(nextTapped)
            assertTrue(backToPlanTapped)
        }

        onNodeWithTag("reading_plans_screen").assertIsDisplayed()
        onNodeWithTag("plan_progress_percent").assertTextContains("0%")
        onNodeWithTag("plan_toggle_${firstReading.id}").performClick()
        waitForIdle()
        onNodeWithTag("plan_progress_percent").assertTextContains("14%")
        onNodeWithTag("plan_reader_status_pill").assertTextContains("Checked Off")

        onNodeWithTag("plan_open_${firstReading.id}").performClick()
        runOnIdle { assertEquals(firstReading.id, openedReadingId) }
    }
}
