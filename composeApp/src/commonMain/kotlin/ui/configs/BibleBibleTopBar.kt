package ui.configs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.bibleIQ.BibleIQDataModel
import ui.configs.home.HomeTopBar

@Composable
internal fun BibleBibleTopBar(onClick: () -> Unit, showBottomSheet: () -> Unit) {
    TopAppBar(
        modifier = Modifier
            .background(MaterialTheme.colors.primarySurface)
            .statusBarsPadding(),
        elevation = 4.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (BibleIQDataModel.showHomePage) {
                BibleBookTopBar()
            } else {
                ChapterTopBar(
                    showHomePage = BibleIQDataModel.showHomePage,
                    onBackClick = onClick,
                    onHomeEmblemClick = BibleIQDataModel.onHomeClick,
                    showBottomSheet = showBottomSheet,
                )
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
