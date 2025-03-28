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
    var localFontSize by remember { mutableStateOf(selectedFontSize) }
    var previousFontSize by remember { mutableStateOf(selectedFontSize) }
    val minTextSize = 12f
    val maxTextSize = 40f
    val doubleTapFontSize = 30f
    var scale by remember { mutableStateOf(1f) }
    var isZooming by remember { mutableStateOf(false) }
    var scrollPositionBeforeZoom by remember { mutableStateOf(0f) }
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
                        if (event.changes.size > 1) {
                            isZooming = true
                            Napier.v("BibleScriptures :: isZooming $isZooming", tag = "ZZ1943")
                            scrollPositionBeforeZoom = scrollState.value.toFloat() / scrollState.maxValue.toFloat()

                            val zoomChange = event.calculateZoom()
                            scale *= zoomChange
                            localFontSize = (localFontSize * scale).coerceIn(minTextSize, maxTextSize)
                            onFontSizeChanged(localFontSize)

                            scale = 1f
                        } else if (isZooming && event.changes.size == 1) {
                            isZooming = false
                            Napier.v("BibleScriptures :: isZooming $isZooming", tag = "ZZ1943")

                            coroutineScope.launch {
                                val newScrollPosition = (scrollPositionBeforeZoom * scrollState.maxValue).toInt()
                                scrollState.scrollTo(newScrollPosition)
                            }

                            coroutineScope.launch {
                                delay(300)
                                Napier.v("BibleScriptures :: debounce fontSize $localFontSize", tag = "AP8243")
                                updateUserPreferences(localFontSize, BibleIQDataModel.selectedVersion)
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scrollPositionBeforeZoom = scrollState.value.toFloat() / scrollState.maxValue.toFloat()

                        if (localFontSize == doubleTapFontSize) {
                            localFontSize = previousFontSize
                        } else {
                            previousFontSize = localFontSize
                            localFontSize = doubleTapFontSize
                        }
                        onFontSizeChanged(localFontSize)

                        coroutineScope.launch {
                            val newScrollPosition = (scrollPositionBeforeZoom * scrollState.maxValue).toInt()
                            scrollState.scrollTo(newScrollPosition)
                        }

                        coroutineScope.launch {
                            updateUserPreferences(localFontSize, BibleIQDataModel.selectedVersion)
                        }
                    }
                )
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