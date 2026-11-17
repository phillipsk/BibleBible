package ui

import androidx.compose.ui.test.*
import data.bibleIQ.DailyVerse
import data.bibleIQ.V2Verse
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class VerseOfTheDayCardTest {
    @Test fun readyCardShowsVerseReferenceAndTranslation() = runComposeUiTest {
        setContent {
            BibleBibleTheme {
                VerseOfTheDayCard(DailyVerseState.Ready(DailyVerse("eng_bbe", "2026-11-17", V2Verse(43, 3, 16, "Daily passage", "JHN"))), "BBE", {})
            }
        }
        onNodeWithText("Verse of the day").assertIsDisplayed()
        onNodeWithText("Daily passage").assertIsDisplayed()
        onNodeWithText("3:16 · BBE", substring = true).assertIsDisplayed()
        onNodeWithText("Try again").assertDoesNotExist()
    }
    @Test fun unavailableCardOffersRetry() = runComposeUiTest {
        var retries = 0
        setContent { BibleBibleTheme { VerseOfTheDayCard(DailyVerseState.Unavailable, "KJV", { retries++ }) } }
        onNodeWithText("Try again").performClick()
        runOnIdle { assertEquals(1, retries) }
    }
    @Test fun loadingCardDoesNotShowStalePassage() = runComposeUiTest {
        setContent { BibleBibleTheme { VerseOfTheDayCard(DailyVerseState.Loading, "KJV", {}) } }
        onNodeWithText("Loading today’s verse…").assertIsDisplayed()
        onNodeWithText("Try again").assertDoesNotExist()
    }
}
