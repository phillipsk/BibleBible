package ui.configs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.apiBible.ReadingHistoryUIState
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.BibleIQVersions

@Composable
internal fun BottomSheetConfigs(
    bibleVersionsList: BibleIQVersions,
    showAISummary: Boolean,
    readingHistory: List<ReadingHistoryUIState>?
) {
    Column(
        modifier = Modifier.padding(4.dp).height(450.dp).verticalScroll(rememberScrollState()),
    ) {
        if (!showAISummary) {
            BibleMenu(
                bibleVersionsList = bibleVersionsList,
                selectedVersion = BibleIQDataModel.selectedVersion
            )
        }
        FontSizeSlider(BibleIQDataModel.fontSizeOptions, BibleIQDataModel.selectedFontSize)

        Column(
            modifier = Modifier.padding(4.dp)
        ) {
            readingHistory?.forEach {
                Text(text = it.toString())
            }
        }
    }
}