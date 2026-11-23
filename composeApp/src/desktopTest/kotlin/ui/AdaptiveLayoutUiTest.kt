package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import data.shorts.BiblicalShortDto
import data.shorts.InMemorySeenWordsStore
import data.shorts.InMemoryShortsPrefsBacking
import data.shorts.ShortsProgressManager
import data.shorts.defaultBiblicalShortsCatalog
import ui.shorts.BiblicalShortsScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AdaptiveLayoutUiTest {

    @Test
    fun windowMetricsClassifyPhonesFoldablesAndTabletsAccurately() {
        val smallAndroid = computeAdaptiveWindowMetrics(360.dp, 640.dp)
        assertEquals(BibleWindowLayoutClass.COMPACT_PHONE, smallAndroid.layoutClass)
        assertEquals(3, smallAndroid.bookGridColumns)
        assertFalse(smallAndroid.isDualPaneShorts)

        val iphoneSe = computeAdaptiveWindowMetrics(375.dp, 667.dp)
        assertEquals(BibleWindowLayoutClass.COMPACT_PHONE, iphoneSe.layoutClass)
        assertEquals(3, iphoneSe.bookGridColumns)

        val iphone15Pro = computeAdaptiveWindowMetrics(393.dp, 852.dp)
        assertEquals(BibleWindowLayoutClass.COMPACT_PHONE, iphone15Pro.layoutClass)

        val pixel7 = computeAdaptiveWindowMetrics(412.dp, 915.dp)
        assertEquals(BibleWindowLayoutClass.COMPACT_PHONE, pixel7.layoutClass)

        val iphoneDuoUnfolded = computeAdaptiveWindowMetrics(760.dp, 840.dp)
        assertEquals(BibleWindowLayoutClass.FOLDABLE_DUO_UNFOLDED, iphoneDuoUnfolded.layoutClass)
        assertEquals(4, iphoneDuoUnfolded.bookGridColumns)
        assertEquals(28.dp, iphoneDuoUnfolded.hingeSafeGap)
        assertTrue(iphoneDuoUnfolded.isDualPaneShorts)
        assertTrue(iphoneDuoUnfolded.isWideHome)

        val innerFoldable = computeAdaptiveWindowMetrics(673.dp, 841.dp)
        assertEquals(BibleWindowLayoutClass.FOLDABLE_DUO_UNFOLDED, innerFoldable.layoutClass)
        assertTrue(innerFoldable.isDualPaneShorts)

        val ipadLandscape = computeAdaptiveWindowMetrics(1024.dp, 768.dp)
        assertEquals(BibleWindowLayoutClass.TABLET_WIDE, ipadLandscape.layoutClass)
        assertEquals(6, ipadLandscape.bookGridColumns)
        assertTrue(ipadLandscape.isDualPaneShorts)

        val pixelTabletPortrait = computeAdaptiveWindowMetrics(834.dp, 1194.dp)
        assertEquals(BibleWindowLayoutClass.FOLDABLE_DUO_UNFOLDED, pixelTabletPortrait.layoutClass)
        assertTrue(pixelTabletPortrait.isDualPaneShorts)
    }

    @Test
    fun compactPhoneShortsScreenKeepsFullBleedLayout() = runComposeUiTest {
        val sampleShorts = defaultBiblicalShortsCatalog("")
        val progressManager = dismissedTutorialManager()
        var clickedChapter: BiblicalShortDto? = null

        setContent {
            BibleBibleTheme {
                Box(modifier = Modifier.size(393.dp, 852.dp)) {
                    BiblicalShortsScreen(
                        initialShorts = sampleShorts,
                        onBack = {},
                        onReadChapter = { clickedChapter = it },
                        enablePlaybackTimer = false,
                        progressManager = progressManager,
                    )
                }
            }
        }

        onNodeWithTag("shorts_adaptive_dual_pane").assertDoesNotExist()
        onNodeWithTag("shorts_category_row", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithTag("shorts_progress_scrubber", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("shorts_progress_scrubber", useUnmergedTree = true).performSemanticsAction(
            SemanticsActions.SetProgress
        ) { setProgress ->
            setProgress(sampleShorts.first().effectiveScene2StartSeconds().toFloat() + 1f)
        }
        onNodeWithTag("short_card2_read_now_button", useUnmergedTree = true).performClick()
        runOnIdle { assertEquals(sampleShorts.first().id, clickedChapter?.id) }
    }

    @Test
    fun iphoneDuoUnfoldedRendersHingeSafeDualPaneTheaterLayout() = runComposeUiTest {
        val sampleShorts = defaultBiblicalShortsCatalog("")
        val progressManager = dismissedTutorialManager()
        var clickedChapter: BiblicalShortDto? = null

        setContent {
            BibleBibleTheme {
                Box(modifier = Modifier.size(760.dp, 840.dp)) {
                    BiblicalShortsScreen(
                        initialShorts = sampleShorts,
                        onBack = {},
                        onReadChapter = { clickedChapter = it },
                        enablePlaybackTimer = false,
                        progressManager = progressManager,
                    )
                }
            }
        }

        onNodeWithTag("shorts_adaptive_dual_pane").assertIsDisplayed()
        onNodeWithTag("shorts_theater_video_frame").assertIsDisplayed()
        onAllNodesWithText(sampleShorts.first().title).onFirst().assertIsDisplayed()
        onNodeWithTag("shorts_progress_scrubber", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("shorts_category_row", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("Sound Off").assertIsDisplayed()

        onNodeWithTag("biblical_shorts_pager").performTouchInput { swipeUp() }
        waitForIdle()
        onAllNodesWithText(sampleShorts[1].title).onFirst().assertIsDisplayed()
        onNodeWithTag("shorts_progress_scrubber", useUnmergedTree = true).performSemanticsAction(
            SemanticsActions.SetProgress
        ) { setProgress ->
            setProgress(sampleShorts[1].effectiveScene2StartSeconds().toFloat() + 1f)
        }
        onNodeWithTag("short_card2_read_now_button", useUnmergedTree = true).performClick()
        runOnIdle { assertEquals(sampleShorts[1].id, clickedChapter?.id) }
    }

    @Test
    fun tabletWideRendersTheaterFrameAndActionControlsCleanly() = runComposeUiTest {
        val sampleShorts = defaultBiblicalShortsCatalog("")
        val progressManager = dismissedTutorialManager()

        setContent {
            BibleBibleTheme {
                Box(modifier = Modifier.size(1024.dp, 768.dp)) {
                    BiblicalShortsScreen(
                        initialShorts = sampleShorts,
                        onBack = {},
                        onReadChapter = {},
                        enablePlaybackTimer = false,
                        progressManager = progressManager,
                    )
                }
            }
        }

        onNodeWithTag("shorts_adaptive_dual_pane").assertIsDisplayed()
        onNodeWithTag("shorts_theater_video_frame").assertIsDisplayed()
        onAllNodesWithText(sampleShorts.first().scriptureReference).onFirst().assertIsDisplayed()
        onNodeWithText("Faith & Courage", substring = true, ignoreCase = true).assertDoesNotExist()
        onNodeWithTag("dual_pane_read_chapter_btn").assertIsDisplayed()
        onNodeWithTag("shorts_category_row").assertDoesNotExist()
        onNodeWithTag("read_chapter_btn_${sampleShorts.first().id}").assertDoesNotExist()
        onNodeWithTag("next_short_btn_${sampleShorts.first().id}").assertDoesNotExist()
        onNodeWithTag("shorts_progress_scrubber").assertIsDisplayed()
        onNodeWithTag("shorts_mute_toggle").assertIsDisplayed()
    }

    @Test
    fun phoneLandscapeUsesCompactDualPaneWithoutCategoryBadge() = runComposeUiTest {
        val sampleShorts = defaultBiblicalShortsCatalog("")
        val progressManager = dismissedTutorialManager()
        var clickedChapter: BiblicalShortDto? = null

        setContent {
            BibleBibleTheme {
                Box(modifier = Modifier.size(844.dp, 390.dp)) {
                    BiblicalShortsScreen(
                        initialShorts = sampleShorts,
                        onBack = {},
                        onReadChapter = { clickedChapter = it },
                        enablePlaybackTimer = false,
                        progressManager = progressManager,
                    )
                }
            }
        }

        onNodeWithTag("shorts_adaptive_dual_pane").assertIsDisplayed()
        onNodeWithTag("shorts_theater_video_frame").assertIsDisplayed()
        onNodeWithText("Faith & Courage", substring = true, ignoreCase = true).assertDoesNotExist()
        onNodeWithTag("dual_pane_read_chapter_btn").assertIsDisplayed().performClick()
        runOnIdle { assertEquals(sampleShorts.first().id, clickedChapter?.id) }
    }

    private fun dismissedTutorialManager(): ShortsProgressManager = ShortsProgressManager(
        backing = InMemoryShortsPrefsBacking(),
        seenStore = InMemorySeenWordsStore(),
    ).apply {
        markTutorialDismissed()
    }
}
