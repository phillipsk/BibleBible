package ui.configs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.BibleIQVersions

@Composable
internal fun BackLayerConfigs(bibleVersionsList: BibleIQVersions) {
    Column(modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)) {
        BibleMenu(
            bibleVersionsList = bibleVersionsList,
            selectedVersion = BibleIQDataModel.selectedVersion
        )
    }
}