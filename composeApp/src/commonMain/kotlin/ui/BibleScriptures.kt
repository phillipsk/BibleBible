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
import androidx.compose.ui.text.TextLayoutResult
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
    val currentFontSize by rememberUpdatedState(selectedFontSize)
    val changeFontSize by rememberUpdatedState(onFontSizeChanged)
    readingState.contentInset = with(LocalDensity.current) { 4.dp.roundToPx() }

    LaunchedEffect(scrollState, readingState) {
        snapshotFlow { scrollState.value }.collect { readingState.capture(it) }
    }

    // Wait for the scroll container's new range before restoring the visible text.
    // Keep our own restoration out of the capture flow to avoid cumulative drift.
    LaunchedEffect(textLayout, readingState) {
        val layout = textLayout ?: return@LaunchedEffect
        try {
            withFrameNanos { }
            scrollState.scrollTo(readingState.scrollOffset(layout))
            withFrameNanos { }
        } finally {
            if (textLayout === layout) readingState.restoring = false
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
            .verticalScroll(scrollState)
            .padding(4.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } > 1) {
                            val zoom = event.calculateZoom()
                            if (zoom.isFinite() && zoom != 1f) {
                                val maximum = if (isDesktopPlatform()) 72f else 40f
                                changeFontSize((currentFontSize * zoom).coerceIn(12f, maximum))
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
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
                modifier = Modifier.testTag("ScriptureText"),
                onTextLayout = { result ->
                    if (textLayout?.layoutInput != result.layoutInput) {
                        readingState.restoring = true
                        readingState.layout = result
                        textLayout = result
                    }
                }
            )
        }
    }
}
