package ui.configs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.bibleIQ.BibleIQDataModel

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
        TopBarTitle("Bible Study")
        Spacer(modifier = Modifier.weight(1f))
        SortBibleBooksToggle()
    }
}

@Composable
internal fun SortBibleBooksToggle() {
    var checked by remember { mutableStateOf(!BibleIQDataModel.sortAZ) }
    Switch(
        checked = checked,
        onCheckedChange = {
            BibleIQDataModel.sortAZ = !BibleIQDataModel.sortAZ
            checked = it
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color.White.copy(alpha = 0.5f),
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color.White.copy(alpha = 0.5f)
        )
    )
    Text(
        text = AnnotatedString(BibleIQDataModel.selectedSortType),
        style = MaterialTheme.typography.subtitle1.copy(fontSize = 14.sp, color = Color.White),
    )
}
