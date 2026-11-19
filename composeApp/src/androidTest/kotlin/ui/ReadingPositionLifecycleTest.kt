package ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ReadingPositionLifecycleTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun keepsTextAnchorAfterActualPortraitToLandscapeRotation() {
        ActivityScenario.launch(ReaderPositionTestActivity::class.java).use { activity ->
            activity.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            awaitReader { it.orientation == Configuration.ORIENTATION_PORTRAIT }
            val before = scrollIntoChapter()

            activity.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            val after = awaitReader {
                it.orientation == Configuration.ORIENTATION_LANDSCAPE && it.generation > before.generation
            }

            assertAnchorPreserved(before, after)
        }
    }

    @Test
    fun keepsTextAnchorAfterActivityScenarioRecreate() {
        ActivityScenario.launch(ReaderPositionTestActivity::class.java).use { activity ->
            awaitReader()
            val before = scrollIntoChapter()

            activity.recreate()
            val after = awaitReader { it.generation > before.generation }

            assertAnchorPreserved(before, after)
        }
    }

    private fun scrollIntoChapter(): ReaderPositionSnapshot {
        compose.runOnIdle { ReaderPositionTestProbe.scrollTo(1_200) }
        return awaitReader { it.character > 0 && it.scrollOffset >= 1_000 }
    }

    private fun awaitReader(
        condition: (ReaderPositionSnapshot) -> Boolean = { true }
    ): ReaderPositionSnapshot {
        // TCG cloud emulators can be slow; require actual layout/readiness rather
        // than assuming an Activity PID or a completed launch means it rendered.
        compose.waitUntil(timeoutMillis = 120_000) {
            ReaderPositionTestProbe.snapshot()?.let {
                !it.restoring && it.lineCount > 1 && it.maxScrollOffset > 1_200 && condition(it)
            } == true
        }
        compose.waitForIdle()
        return compose.runOnIdle { requireNotNull(ReaderPositionTestProbe.snapshot()) }
    }

    private fun assertAnchorPreserved(before: ReaderPositionSnapshot, after: ReaderPositionSnapshot) {
        assertTrue("A new Activity instance must be created", after.generation > before.generation)
        assertEquals("The same scripture character must remain anchored", before.character, after.character)
        assertEquals("The intra-line position must be retained", before.lineFraction, after.lineFraction, 0.001f)
        assertTrue(
            "The original reading character must actually be in the first visible text line",
            before.character >= after.firstVisibleLineStart && before.character <= after.firstVisibleLineEnd
        )
        assertEquals("The viewport must retain the original position within the line", before.lineFraction, after.viewportLineFraction, 0.06f)
        assertTrue("The anchor must map to the new layout", abs(after.scrollOffset - after.expectedScrollOffset) <= 1)
        assertFalse("Restoration must finish", after.restoring)
    }
}
