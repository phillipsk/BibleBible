package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.apiBible.ChapterContent

@Composable
internal fun BibleScriptures(chapters: ChapterContent, scrollState: ScrollState) {
    Column(modifier = Modifier.verticalScroll(scrollState)) {
        chapters.data?.let {
            Text(
                text = it.uiContent,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}