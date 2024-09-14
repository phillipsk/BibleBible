package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
internal fun BibleScriptures(chapters: BibleChapterUIState, scrollState: ScrollState, ) {
    var fontSize by remember { mutableStateOf(16f) }
    val minTextSize = 16f
    var scale by remember(chapters.chapterId) { mutableStateOf(1f) }

    LazyColumn(
        modifier = Modifier
//            .verticalScroll(scrollState)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale *= zoom
                    fontSize = (fontSize * scale) //.coerceAtLeast(minTextSize)
                    scale = 1f
                }
            }
    ) {

        chapters.text?.let {
            item {
                Text(
                    text = it,
                    fontSize = fontSize.sp,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}