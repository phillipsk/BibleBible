package ui.configs

import androidx.compose.runtime.Composable
import data.bibleIQ.BibleIQVersions

@Composable
internal fun BackLayerColumnConfigs(bibleVersionsList: BibleIQVersions) {
    BibleMenu(bibleVersionsList = bibleVersionsList)
}