package ui.configs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.BibleIQVersions

@Composable
internal fun BottomSheetConfigs(bibleVersionsList: BibleIQVersions, showAISummary: Boolean) {
    Column(
        modifier = Modifier.padding(4.dp),
    ) {
        if (!showAISummary) {
            BibleMenu(
                bibleVersionsList = bibleVersionsList,
                selectedVersion = BibleIQDataModel.selectedVersion
            )
        }
        FontSizeSlider(BibleIQDataModel.fontSizeOptions, BibleIQDataModel.selectedFontSize)
    }
}