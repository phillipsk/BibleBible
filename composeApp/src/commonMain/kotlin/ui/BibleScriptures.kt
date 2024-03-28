package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiResponseDto
import data.bibleIQ.BibleChapterUIState
import data.bibleIQ.BibleIQDataModel
import data.generateContent
import email.kevinphillips.biblebible.BuildKonfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun BibleScriptures(chapters: BibleChapterUIState, scrollState: ScrollState) {
    var data by remember { mutableStateOf(GeminiResponseDto()) }
    LaunchedEffect(true) {
        BibleIQDataModel.geminiData = generateContent("Matthew 21", BuildKonfig.GEMINI_API_KEY)
        withContext(Dispatchers.Main) {
            data = BibleIQDataModel.geminiData
        }
    }
    if (data.candidates.isNullOrEmpty()) {
        LoadingScreen()
    } else {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            Text(
                text = data.candidates.toString(),
                fontSize = 20.sp,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}