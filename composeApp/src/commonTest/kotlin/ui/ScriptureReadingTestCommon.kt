package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import data.bibleIQ.BibleChapterUIState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ScriptureReaderHarness {
    var width by mutableStateOf(280.dp)
    var height by mutableStateOf(320.dp)
    var fontSize by mutableStateOf(20f)
    var fontScale by mutableStateOf(1f)
    var visible by mutableStateOf(true)
    lateinit var reading: ScriptureReadingState
    lateinit var scroll: ScrollState
    lateinit var scope: CoroutineScope
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.prepareReader(): ScriptureReaderHarness {
    val harness = ScriptureReaderHarness()
    val chapter = BibleChapterUIState(
        bookId = 1, chapterId = 1,
        text = (1..150).joinToString(" ") { "[$it] In the beginning was the Word, and the Word was with God." }
    )
    setContent {
        val savedState = rememberSaveableStateHolder()
        val density = LocalDensity.current.density
        CompositionLocalProvider(LocalDensity provides Density(density, harness.fontScale)) {
            if (harness.visible) {
                savedState.SaveableStateProvider("scripture") {
                    harness.reading = rememberScriptureReadingState()
                    harness.scroll = rememberScrollState()
                    harness.scope = rememberCoroutineScope()
                    Box(Modifier.size(harness.width, harness.height)) {
                        BibleScriptures(
                            chapter, harness.scroll, harness.fontSize,
                            onFontSizeChanged = { harness.fontSize = it },
                            readingState = harness.reading
                        )
                    }
                }
            }
        }
    }
    mainClock.advanceTimeBy(64)
    waitForIdle()
    runOnIdle { harness.scope.launch { harness.scroll.scrollTo(1_200) } }
    waitForIdle()
    runOnIdle {
        assertTrue(harness.reading.anchor.character > 0,
            "Test must scroll into the chapter: scroll=${harness.scroll.value}, max=${harness.scroll.maxValue}, restoring=${harness.reading.restoring}, layout=${harness.reading.layout?.size}")
    }
    return harness
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.runScriptureReflowTest(change: String) {
    val harness = prepareReader()
    val original = runOnIdle { harness.reading.anchor }
    when (change) {
        "rotation" -> runOnIdle { harness.width = 560.dp; harness.height = 180.dp }
        "font-increase" -> runOnIdle { harness.fontSize = 32f }
        "font-decrease" -> runOnIdle { harness.fontSize = 14f }
        "font-scale" -> runOnIdle { harness.fontScale = 1.5f }
        "double-tap" -> onNodeWithTag("ScriptureReader").performTouchInput { doubleClick() }
        "recreation" -> {
            runOnIdle { harness.visible = false }
            waitForIdle()
            runOnIdle { harness.visible = true }
        }
        else -> error("Unknown reflow: $change")
    }
    mainClock.advanceTimeBy(64)
    waitForIdle()
    runOnIdle {
        assertEquals(original, harness.reading.anchor, "Reflow must preserve the text anchor")
        val layout = requireNotNull(harness.reading.layout)
        val expected = harness.reading.scrollOffset(layout).coerceAtMost(harness.scroll.maxValue)
        assertTrue(abs(harness.scroll.value - expected) <= 1, "Anchor should be at the same viewport location after $change")
        assertTrue(!harness.reading.restoring, "Restoration must finish")
    }
}
