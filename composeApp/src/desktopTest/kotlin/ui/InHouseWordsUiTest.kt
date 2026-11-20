package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import data.inhouse.InHouseWordDto
import data.inhouse.InHouseWordsRepository
import ui.inhouse.InHouseWordsEntryCard
import ui.inhouse.InHouseWordsScreen
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class InHouseWordsUiTest {

    @Test
    fun entryCardRendersAndTriggersOpenCallback() = runComposeUiTest {
        var opened = 0
        setContent {
            BibleBibleTheme {
                InHouseWordsEntryCard(onOpenInHouseWords = { opened++ })
            }
        }
        onNodeWithText("In-House Words").assertIsDisplayed()
        onNodeWithText("Church").assertIsDisplayed()
        onNodeWithTag("in_house_words_entry_arrow").performClick()
        runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun inHouseScreenFiltersCategoriesAndTriggersReadChapterAndBack() = runComposeUiTest {
        var backClicks = 0
        var openedWord: InHouseWordDto? = null

        setContent {
            BibleBibleTheme {
                InHouseWordsScreen(
                    onBack = { backClicks++ },
                    onReadChapter = { openedWord = it },
                    initialWords = InHouseWordsRepository.fallbackWords,
                )
            }
        }

        onNodeWithText("In-House Words").assertIsDisplayed()
        onNodeWithText("Walking in Peace Through Uncertainty").assertIsDisplayed()

        // Filter to Morning Prayer
        onNodeWithTag("in_house_category_Morning Prayer").performClick()
        onNodeWithText("Fresh Mercies Before the Day Begins").assertIsDisplayed()
        onNodeWithText("Walking in Peace Through Uncertainty").assertDoesNotExist()

        // Click Read Chapter on the Morning Prayer card
        onNodeWithTag("in_house_read_chapter_ihw_starter_morning_mercy").performClick()
        runOnIdle {
            assertEquals("LAM", openedWord?.bookUsfm)
            assertEquals(3, openedWord?.chapter)
        }

        // Back button callback works
        onNodeWithTag("in_house_back_button").performClick()
        runOnIdle { assertEquals(1, backClicks) }
    }
}
