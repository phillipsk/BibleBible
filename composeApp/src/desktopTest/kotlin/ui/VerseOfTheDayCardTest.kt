package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import data.bibleIQ.DailyVerse
import data.bibleIQ.DailyVerseData
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class VerseOfTheDayCardTest {
    @Test
    fun readyCardShowsVerseReferenceAndTranslation() = runComposeUiTest {
        setContent {
            BibleBibleTheme {
                VerseOfTheDayCard(
                    DailyVerseState.Ready(
                        DailyVerse("BSB", "2026-11-17", DailyVerseData(43, 3, 16, "Daily passage", "JHN"))
                    ),
                    "BSB",
                    {}
                )
            }
        }
        onNodeWithText("Verse of the day").assertIsDisplayed()
        onNodeWithText("Daily passage").assertIsDisplayed()
        onNodeWithText("3:16 · BSB", substring = true).assertIsDisplayed()
        onNodeWithText("Try again").assertDoesNotExist()
    }

    @Test
    fun unavailableCardOffersRetry() = runComposeUiTest {
        var retries = 0
        setContent { BibleBibleTheme { VerseOfTheDayCard(DailyVerseState.Unavailable, "KJV", { retries++ }) } }
        onNodeWithText("Try again").performClick()
        runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun loadingCardDoesNotShowStalePassage() = runComposeUiTest {
        setContent { BibleBibleTheme { VerseOfTheDayCard(DailyVerseState.Loading, "KJV", {}) } }
        onNodeWithText("Loading today’s verse…").assertIsDisplayed()
        onNodeWithText("Try again").assertDoesNotExist()
    }
}
