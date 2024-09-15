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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.appPrefs.updateUserPreferences
import data.bibleIQ.BibleChapterUIState
import data.bibleIQ.BibleIQDataModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun BibleScriptures(
    chapters: BibleChapterUIState,
    scrollState: ScrollState,
    selectedFontSize: Float,
    onFontSizeChanged: (Float) -> Unit
) {
    var localFontSize by remember { mutableStateOf(selectedFontSize) } // TODO: change to selectedFontSize
    val minTextSize = 12f
    val maxTextSize = 40f
    val halfwayFontSize = (18f + maxTextSize) / 2
    var scale by remember { mutableStateOf(1f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedFontSize) {
        localFontSize = selectedFontSize
    }

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
                            // Debounce database update to avoid frequent writes
                            coroutineScope.launch {
                                delay(300)
                                Napier.v("BibleScriptures :: debounce fontSize $newFontSize", tag = "AP8243")
                                updateUserPreferences(newFontSize, BibleIQDataModel.selectedVersion)
                            }
                            scale = 1f
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Zoom to halfway font size on double-tap
                        val newFontSize = if (localFontSize == halfwayFontSize) {
                            22f
                        } else {
                            halfwayFontSize
                        }
                        localFontSize = newFontSize
                        onFontSizeChanged(newFontSize)
                        coroutineScope.launch {
                            updateUserPreferences(newFontSize, BibleIQDataModel.selectedVersion)
                        }
                    }
                )
            }
    ) {
        chapters.text?.let {
            Text(
                text = it,
                fontSize = selectedFontSize.sp,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}