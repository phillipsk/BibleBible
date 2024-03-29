package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import data.bibleIQ.BibleChapterUIState

@Composable
internal fun BibleScriptures(
    chapters: BibleChapterUIState, scrollState: ScrollState, selectedFontSize: TextUnit
) {
    Column(modifier = Modifier.verticalScroll(scrollState)) {
        chapters.text?.let {
            Text(
                text = it,
                fontSize = selectedFontSize,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}