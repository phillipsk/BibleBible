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
import androidx.compose.ui.test.pinch
import androidx.compose.ui.geometry.Offset
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
        BibleBibleTheme {
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
    if (change == "bottom-roundtrip") {
        runOnIdle { harness.scope.launch { harness.scroll.scrollTo(harness.scroll.maxValue) } }
        waitForIdle()
    }
    val original = runOnIdle { harness.reading.anchor }
    when (change) {
        "rotation" -> runOnIdle { harness.width = 560.dp; harness.height = 180.dp }
        "font-increase" -> runOnIdle { harness.fontSize = 32f }
        "font-decrease" -> runOnIdle { harness.fontSize = 14f }
        "font-scale" -> runOnIdle { harness.fontScale = 1.5f }
        "height" -> runOnIdle { harness.height = 560.dp }
        "pinch" -> onNodeWithTag("ScriptureReader").performTouchInput {
            pinch(
                start0 = Offset(center.x - 25f, center.y),
                end0 = Offset(center.x - 55f, center.y),
                start1 = Offset(center.x + 25f, center.y),
                end1 = Offset(center.x + 55f, center.y),
                durationMillis = 320
            )
        }
        "bottom-roundtrip" -> {
            runOnIdle { harness.width = 560.dp; harness.height = 560.dp }
            mainClock.advanceTimeBy(64)
            waitForIdle()
            runOnIdle { assertEquals(original, harness.reading.anchor, "Clamping must not overwrite the reading anchor") }
            runOnIdle { harness.width = 280.dp; harness.height = 320.dp }
        }
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
        val visibleTop = (harness.scroll.value - harness.reading.contentInset).coerceAtLeast(0).toFloat()
        val visibleLine = layout.getLineForVerticalPosition(visibleTop)
        assertTrue(original.character in layout.getLineStart(visibleLine) until layout.getLineEnd(visibleLine),
            "The first visible line must still contain the original text after $change")
        val lineHeight = layout.getLineBottom(visibleLine) - layout.getLineTop(visibleLine)
        val fraction = (visibleTop - layout.getLineTop(visibleLine)) / lineHeight
        assertTrue(abs(fraction - original.lineFraction) * lineHeight <= 1.1f,
            "The anchored line must keep its position within the viewport after $change")
        if (change == "pinch") assertTrue(harness.fontSize > 30f, "Pinch events must accumulate zoom")
        assertTrue(!harness.reading.restoring, "Restoration must finish")
    }
}
