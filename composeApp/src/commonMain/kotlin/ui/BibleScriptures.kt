package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.appPrefs.updateUserPreferences
import data.bibleIQ.BibleChapterUIState
import data.bibleIQ.BibleIQDataModel
import email.kevinphillips.biblebible.isDesktopPlatform
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
internal fun BibleScriptures(
    chapters: BibleChapterUIState,
    scrollState: ScrollState,
    selectedFontSize: Float,
    onFontSizeChanged: (Float) -> Unit,
    readingState: ScriptureReadingState = rememberScriptureReadingState(),
) {
    var previousFontSize by remember { mutableStateOf(selectedFontSize) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val currentFontSize by rememberUpdatedState(selectedFontSize)
    val changeFontSize by rememberUpdatedState(onFontSizeChanged)
    readingState.contentInset = with(LocalDensity.current) { 4.dp.roundToPx() }

    LaunchedEffect(scrollState, readingState) {
        var interactionActive = false
        snapshotFlow { scrollState.value to scrollState.isScrollInProgress }.collect { (offset, inProgress) ->
            if (inProgress && !interactionActive) {
                readingState.beginScrollInteraction()
                interactionActive = true
            }
            if (interactionActive) readingState.capture(offset)
            if (!inProgress) interactionActive = false
        }
    }

    // Wait for the scroll container's new range before restoring the visible text.
    // Keep our own restoration out of the capture flow to avoid cumulative drift.
    LaunchedEffect(textLayout, viewportSize, selectedFontSize, readingState.zoomReleaseGeneration, readingState) {
        val layout = textLayout ?: return@LaunchedEffect
        // A release can arrive before the final font change has been measured.
        // Only the effect for that measured font may unlock anchor capture.
        if (layout.layoutInput.style.fontSize != selectedFontSize.sp) return@LaunchedEffect
        val viewport = viewportSize
        val zoomGeneration = readingState.zoomReleaseGeneration
        try {
            withFrameNanos { }
            scrollState.scrollTo(readingState.scrollOffset(layout))
            readingState.restoredScrollOffset = scrollState.value
            withFrameNanos { }
        } finally {
            if (textLayout === layout && viewportSize == viewport &&
                zoomGeneration == readingState.zoomReleaseGeneration) {
                readingState.restoring = false
                if (!readingState.zooming) readingState.awaitingZoomRestore = false
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { currentFontSize }
            .distinctUntilChanged()
            .drop(1)
            .debounce(300)
            .collect { updateUserPreferences(it, BibleIQDataModel.selectedVersion) }
    }

    Column(
        modifier = Modifier
            .testTag("ScriptureReader")
            .onSizeChanged { size ->
                if (size != viewportSize) {
                    readingState.restoring = true
                    viewportSize = size
                }
            }
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                try {
                    awaitPointerEventScope {
                        var gestureFontSize = currentFontSize
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.count { it.pressed } > 1) {
                                if (!readingState.zooming) {
                                    gestureFontSize = currentFontSize
                                    readingState.beginZoom(scrollState.value)
                                }
                                val zoom = event.calculateZoom()
                                if (zoom.isFinite() && zoom != 1f) {
                                    val maximum = if (isDesktopPlatform()) 72f else 40f
                                    gestureFontSize = (gestureFontSize * zoom).coerceIn(12f, maximum)
                                    changeFontSize(gestureFontSize)
                                    event.changes.forEach { it.consume() }
                                }
                            } else readingState.endZoom()
                        }
                    }
                } finally {
                    // Disposal/cancellation must not leave a reused reader locked.
                    readingState.endZoom()
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    val nextSize = if (currentFontSize == 30f) previousFontSize else {
                        previousFontSize = currentFontSize
                        30f
                    }
                    changeFontSize(nextSize)
                })
            }
    ) {
        chapters.text?.let { text ->
            Text(
                text = text,
                fontSize = selectedFontSize.sp,
                lineHeight = (selectedFontSize * 1.52f).sp,
                modifier = Modifier.testTag("ScriptureText"),
                onTextLayout = { result ->
                    if (!sameTextGeometry(textLayout, result)) {
                        readingState.restoring = true
                        readingState.layout = result
                        textLayout = result
                    }
                }
            )
        }
    }
}

private fun sameTextGeometry(previous: TextLayoutResult?, next: TextLayoutResult): Boolean {
    if (previous == null || previous.layoutInput != next.layoutInput ||
        previous.size != next.size || previous.lineCount != next.lineCount) return false
    // Typeface resolution can change line wrapping without changing TextLayoutInput.
    return (0 until next.lineCount).all { line ->
        previous.getLineStart(line) == next.getLineStart(line) &&
            previous.getLineEnd(line) == next.getLineEnd(line) &&
            previous.getLineTop(line) == next.getLineTop(line) &&
            previous.getLineBottom(line) == next.getLineBottom(line)
    }
}
