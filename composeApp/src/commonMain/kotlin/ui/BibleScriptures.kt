package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.bibleIQ.BibleChapterUIState

@Composable
internal fun BibleScriptures(chapters: BibleChapterUIState, scrollState: ScrollState) {
    var fontSize by remember { mutableStateOf(16f) }
    val minTextSize = 16f
    val state = rememberTransformableState { zoomChange, _, _ ->
        fontSize *= zoomChange
    }

    Column(modifier = Modifier.verticalScroll(scrollState)) {
        chapters.text?.let {
            Text(
                text = it,
                fontSize = fontSize.coerceAtLeast(minTextSize).sp,
                modifier = Modifier
                    .padding(4.dp)
                    .transformable(state = state)
            )
        }
    }
}