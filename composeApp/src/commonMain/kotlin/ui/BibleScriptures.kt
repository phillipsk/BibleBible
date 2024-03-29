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
import data.bibleIQ.BibleChapterUIState
import data.bibleIQ.BibleIQDataModel
import data.gemini.generateContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun BibleScriptures(chapters: BibleChapterUIState, scrollState: ScrollState) {
    var content by remember { mutableStateOf(BibleIQDataModel.geminiDataText) }
    val query =
        BibleIQDataModel.selectedBook.cleanedName + " " + BibleIQDataModel.bibleChapter?.chapterId
    LaunchedEffect(true) {
        generateContent(query)
        if (BibleIQDataModel.geminiDataText?.isEmpty() == false) {
            withContext(Dispatchers.Main) {
                content = BibleIQDataModel.geminiDataText
            }
        }
    }
    if (content.isNullOrEmpty()) {
        LoadingScreen()
    } else {
        content?.let {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                Text(
                    text = it,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}