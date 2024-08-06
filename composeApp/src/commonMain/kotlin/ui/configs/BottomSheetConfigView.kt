package ui.configs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.apiBible.BibleAPIDataModel
import data.apiBible.ReadingHistoryUIState
import data.apiBible.getReadingHistory
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.BibleIQVersions

@Composable
internal fun BottomSheetConfigView(
    bibleVersionsList: BibleIQVersions,
    showAISummary: Boolean,
    readingHistory: List<ReadingHistoryUIState>?,
) {
    LaunchedEffect(true) {
        getReadingHistory()
    }
    val readingHistory = BibleAPIDataModel.readingHistory
    Column(modifier = Modifier.padding(4.dp).height(550.dp)) {
        if (!showAISummary) {
            BibleMenu(
                bibleVersionsList = bibleVersionsList,
                selectedVersion = BibleIQDataModel.selectedVersion
            )
        }
        FontSizeSlider(BibleIQDataModel.fontSizeOptions, BibleIQDataModel.selectedFontSize)
        if (!readingHistory.isNullOrEmpty()) {
            Text(text = "Reading History", fontSize = 20.sp)
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                readingHistory.forEach {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it.toString())
                }
            }
        }
    }
}