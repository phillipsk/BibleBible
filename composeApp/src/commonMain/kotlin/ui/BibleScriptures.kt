package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.api.bible.ChapterContent
import data.bibleIQ.BibleIQ

@Composable
fun BibleScriptures() {
    val chapters: ChapterContent = BibleIQ.chapter.value
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        chapters.data?.content?.let {
            Text(
                it,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}