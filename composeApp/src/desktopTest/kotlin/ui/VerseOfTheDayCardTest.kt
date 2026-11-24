package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import data.bible.BibleBooksResponse
import data.bible.BibleState
import androidx.compose.ui.test.runComposeUiTest
import data.bible.DailyVerse
import data.bible.DailyVerseData
import data.bible.DailyVerseRepository
import data.bible.json.BUNDLED_BIBLE_BOOKS_JSON
import data.config.CloudFeatureFlagsDto
import data.config.CloudRuntimeConfigDto
import data.config.CloudSessionManager
import kotlinx.serialization.json.Json
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

    @Test
    fun readyCardTappingNavigatesToVerseChapter() = runComposeUiTest {
        var clicked: DailyVerse? = null
        val verse = DailyVerse("BSB", "2026-11-19", DailyVerseData(43, 3, 16, "For God so loved the world", "JHN"))
        setContent {
            BibleBibleTheme {
                VerseOfTheDayCard(
                    state = DailyVerseState.Ready(verse),
                    version = "BSB",
                    onRetry = {},
                    onVerseClick = { clicked = it },
                )
            }
        }
        onNodeWithText("For God so loved the world").performClick()
        runOnIdle {
            assertEquals(43, clicked?.data?.bookNumber)
            assertEquals(3, clicked?.data?.chapter)
        }
    }
    @Test
    fun pullingHomeRefreshesTheVerseAndAnUnsuccessfulRetryKeepsItReadable() = runComposeUiTest {
        var refreshes = 0
        val day = "2026-11-24"
        val repository = DailyVerseRepository(
            fetch = { DailyVerse(it, day, DailyVerseData(23, 40, 31, "Initial verse", "ISA")) },
            today = { day },
            refreshFetch = { version, after ->
                refreshes++
                assertEquals(if (refreshes == 1) 31 else 30, after?.verse)
                if (refreshes == 2) error("Offline")
                DailyVerse(version, day, DailyVerseData(23, 40, 30, "Refreshed verse", "ISA"))
            },
        )
        CloudSessionManager.applyRuntimeConfig(
            CloudRuntimeConfigDto(
                features = CloudFeatureFlagsDto(biblicalShortsEnabled = false, bookShimmerStyle = "none"),
            ),
        )
        try {
            BibleState.updateBooks(Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON))
            setContent {
                BibleBibleTheme {
                    Box(Modifier.size(412.dp, 800.dp).testTag("daily_verse_home")) {
                        BibleHomeContent(rememberLazyGridState(), verseRepository = repository)
                    }
                }
            }
            onNodeWithText("Initial verse").assertIsDisplayed()
            fun pullHome() {
                onNodeWithTag("daily_verse_home").performTouchInput {
                    swipe(Offset(centerX, height * 0.21f), Offset(centerX, height * 0.72f))
                }
            }
            pullHome()
            onNodeWithText("Refreshed verse").assertIsDisplayed()
            pullHome()
            onNodeWithText("Pull to refresh").assertDoesNotExist()
            onNodeWithText("Refreshed verse").assertIsDisplayed()
            runOnIdle { assertEquals(2, refreshes) }
        } finally {
            CloudSessionManager.resetForTesting()
        }
    }

}
