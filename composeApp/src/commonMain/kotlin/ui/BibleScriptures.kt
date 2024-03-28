package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiResponseDto
import data.bibleIQ.BibleChapterUIState
import data.generateContent
import email.kevinphillips.biblebible.BuildKonfig

@Composable
internal fun BibleScriptures(chapters: BibleChapterUIState, scrollState: ScrollState) {
    var data: GeminiResponseDto? = null
    LaunchedEffect(true) {
        data = generateContent("Matthew 21", BuildKonfig.GEMINI_API_KEY)
    }
    Column(modifier = Modifier.verticalScroll(scrollState)) {
        data?.let {
            Text(
                text = it.candidates.toString(),
                fontSize = 20.sp,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}