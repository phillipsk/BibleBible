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
import data.GeminiModel
import data.bibleIQ.BibleIQDataModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GeminiSummary(scrollState: ScrollState) {
    var content by remember { mutableStateOf(GeminiModel.geminiDataText) }
    val query = "Generate a summary of the bible chapter " +
        BibleIQDataModel.selectedBook.cleanedName + " " + BibleIQDataModel.bibleChapter?.chapterId
    LaunchedEffect(true) {
//        generateContent(query)
        withContext(Dispatchers.Main) {
            if (GeminiModel.geminiDataText?.isEmpty() == false) {
                content = GeminiModel.geminiDataText

            } else {
                BibleIQDataModel.updateErrorSnackBar("AI Summary could not connect. Please try again later.")
                BibleIQDataModel.showHomePage = true
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