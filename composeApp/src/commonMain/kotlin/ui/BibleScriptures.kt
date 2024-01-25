package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.apiBible.ChapterContent

@Composable
internal fun BibleScriptures(chapters: ChapterContent) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.verticalScroll(scrollState)) {
        chapters.data?.let {
            Text(
                text = it.uiContent,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}