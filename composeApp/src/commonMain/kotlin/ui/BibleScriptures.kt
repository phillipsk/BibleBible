package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.bibleIQ.BibleChapterUIState

@Composable
internal fun BibleScriptures(
    chapters: BibleChapterUIState,
    scrollState: ScrollState,
    fontSize: Float,
    onFontSizeChanged: (Float) -> Unit
) {
    var localFontSize by remember { mutableStateOf(fontSize) }
    val minTextSize = 12f
    val maxTextSize = 40f
    var scale by remember { mutableStateOf(1f) }

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        // Detect pinch gesture with more than one finger
                        if (event.changes.size > 1) {
                            val zoomChange = event.calculateZoom()
                            scale *= zoomChange
                            // Calculate new font size based on zoom
                            val newFontSize = (localFontSize * scale).coerceIn(minTextSize, maxTextSize)
                            localFontSize = newFontSize
                            onFontSizeChanged(newFontSize)
                            scale = 1f
                        }
                    }
                }
            }
    ) {
        chapters.text?.let {
            Text(
                text = it,
                fontSize = localFontSize.sp,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}