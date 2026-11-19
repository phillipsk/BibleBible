package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import data.shorts.BiblicalShortDto
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
        onNodeWithText("Biblical Shorts · 50 Life Lessons").assertIsDisplayed()
        onNodeWithText("50 Reels").assertIsDisplayed()
        onNodeWithTag("biblical_shorts_entry_arrow").performClick()
        runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun shortsScreenDisplaysCuratedShortsFiltersAndTriggersReadChapter() = runComposeUiTest {
        val sampleShorts = defaultBiblicalShortsCatalog("")
        var selectedCategory = "All"
        var openedChapter: BiblicalShortDto? = null
        var backClicks = 0

        setContent {
            BibleBibleTheme {
                BiblicalShortsScreen(
                    initialShorts = sampleShorts,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    onBack = { backClicks++ },
                    onReadChapter = { openedChapter = it },
                    enablePlaybackTimer = false,
                )
            }
        }

        onNodeWithText("Biblical Shorts").assertIsDisplayed()
        onNodeWithText("Creation & The Power of Light").assertIsDisplayed()
        onNodeWithText("Genesis 1:3-5").assertIsDisplayed()
        onNodeWithText("Key Lesson:", substring = true).assertIsDisplayed()

        onNodeWithText("Next Short").performClick()
        onNodeWithText("Noah's Quiet Perseverance").assertIsDisplayed()

        onNodeWithTag("read_chapter_btn_short-02-noah-perseverance").performClick()
        runOnIdle {
            assertEquals("short-02-noah-perseverance", openedChapter?.id)
            assertEquals(1, openedChapter?.bookId)
            assertEquals(6, openedChapter?.chapter)
        }
    }
}
