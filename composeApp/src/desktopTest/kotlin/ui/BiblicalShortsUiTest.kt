package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import data.shorts.BiblicalShortDto
import data.shorts.InMemorySeenWordsStore
import data.shorts.InMemoryShortsPrefsBacking
import data.shorts.ShortsProgressManager
import data.shorts.defaultBiblicalShortsCatalog
import ui.shorts.BiblicalShortsEntryCard
import ui.shorts.BiblicalShortsScreen
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BiblicalShortsUiTest {

    @Test
    fun entryCardRendersUnderneathVerseAndTriggersOpenCallback() = runComposeUiTest {
        var opened = 0
        setContent {
            BibleBibleTheme {
                BiblicalShortsEntryCard(
                    onOpenShorts = { opened++ },
                    animateArrow = true,
                )
            }
        }
        onNodeWithText("Biblical Words").assertIsDisplayed()
        onNodeWithText("Watch").assertIsDisplayed()
        onNodeWithText("50 Reels").assertDoesNotExist()
        onNodeWithTag("biblical_shorts_entry_arrow").performClick()
        runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun shortsScreenShowsTutorialDefaultsSoundOffIncludesScrubberAndTriggersReadChapter() = runComposeUiTest {
        val sampleShorts = defaultBiblicalShortsCatalog("")
        var selectedCategory = "All"
        var openedChapter: BiblicalShortDto? = null
        var backClicks = 0
        val testProgressManager = ShortsProgressManager(
            backing = InMemoryShortsPrefsBacking(),
            seenStore = InMemorySeenWordsStore(),
        )

        setContent {
            BibleBibleTheme {
                BiblicalShortsScreen(
                    initialShorts = sampleShorts,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    onBack = { backClicks++ },
                    onReadChapter = { openedChapter = it },
                    enablePlaybackTimer = false,
                    progressManager = testProgressManager,
                )
            }
        }

        // First-time gesture tutorial renders and dismisses cleanly
        onNodeWithText("Welcome to Biblical Words").assertIsDisplayed()
        onNodeWithText("Double-tap center").assertIsDisplayed()
        onNodeWithText("Tap once").assertIsDisplayed()
        onNodeWithText("Swipe up").assertIsDisplayed()
        onNodeWithTag("shorts_tutorial_dismiss_btn").performClick()
        onNodeWithText("Welcome to Biblical Words").assertDoesNotExist()

        onNodeWithText("Biblical Words").assertIsDisplayed()
        // Sound defaults to OFF and toggles cleanly
        onNodeWithText("Sound Off").assertIsDisplayed()
        onNodeWithTag("shorts_mute_toggle").performClick()
        onNodeWithText("Sound On").assertIsDisplayed()

        // Decluttered bottom: Key Lesson removed, scrubber + bottom categories + weekly hint present
        onNodeWithText("Key Lesson:", substring = true, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithTag("shorts_progress_scrubber", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("shorts_category_row", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("shorts_swipe_hint", useUnmergedTree = true).assertIsDisplayed()

        // Advance to second short and tap Read Chapter
        val expectedSecond = sampleShorts[1]
        onNodeWithText("Next").performClick()
        onNodeWithTag("read_chapter_btn_${expectedSecond.id}").performClick()
        runOnIdle {
            assertEquals(expectedSecond.id, openedChapter?.id)
            assertEquals(expectedSecond.bookId, openedChapter?.bookId)
            assertEquals(expectedSecond.chapter, openedChapter?.chapter)
        }
    }
}
