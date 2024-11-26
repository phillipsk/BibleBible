package ui.configs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.BottomSheetState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.apiBible.BibleAPIDataModel
import data.apiBible.getReadingHistory
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.BibleIQVersions
import email.kevinphillips.biblebible.isDesktopPlatform
import io.github.aakira.napier.Napier

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BottomSheetConfigView(
    bibleVersionsList: BibleIQVersions,
    showAISummary: Boolean,
    bottomSheetState: BottomSheetState,
) {
    LaunchedEffect(true) {
        getReadingHistory()
    }
    val readingHistory = BibleAPIDataModel.readingHistory
    Napier.v("LaunchedEffect :: count :: ${readingHistory?.size}", tag = "RH1283")
    val bottomSheetHeight = if (isDesktopPlatform()) 150.dp else 550.dp
    Column(modifier = Modifier.padding(4.dp).height(bottomSheetHeight)) {
        if (!showAISummary) {
            BibleMenu(
                bibleVersionsList = bibleVersionsList,
                selectedVersion = BibleIQDataModel.selectedVersion,
                bottomSheetState = bottomSheetState
            )
        }
        FontSizeSlider(bottomSheetState)
        if (!readingHistory.isNullOrEmpty()) {
            Text(text = "Reading History",
                fontFamily = MaterialTheme.typography.h1.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(readingHistory) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it.toString())
                }
            }
        }
    }
}