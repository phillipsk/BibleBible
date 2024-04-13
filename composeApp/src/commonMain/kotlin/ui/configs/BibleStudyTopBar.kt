package ui.configs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.bibleIQ.BibleIQDataModel
import org.jetbrains.compose.resources.ExperimentalResourceApi
import ui.configs.home.HomeTopBar

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun BibleStudyTopBar(onClick: () -> Unit, showBottomSheet: () -> Unit) {
    TopAppBar {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (BibleIQDataModel.showHomePage) {
                BibleBookTopBar()
            } else {
                ChapterTopBar(BibleIQDataModel.showHomePage, onClick, showBottomSheet)
            }
        }
    }
}

@Composable
private fun BibleBookTopBar() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, end = 8.dp)
    ) {
        HomeTopBar()
    }
}