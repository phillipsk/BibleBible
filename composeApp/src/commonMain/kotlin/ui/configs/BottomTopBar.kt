package ui.configs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiModel
import data.bibleIQ.BibleIQDataModel
import kotlinx.coroutines.launch

@Composable
internal fun FrontLayerTopBar(onClick: () -> Unit) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onClick)
                    .weight(1f)
            ) {
                Text(
                    text = if (BibleIQDataModel.showHomePage) {
                        "Bible Study"
                    } else {
                        BibleIQDataModel.selectedBook.cleanedName.toString()
                    },
                    style = TextStyle(
                        fontFamily = FontFamily.Cursive,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.1.sp,
                        color = Color.White,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp, end = 4.dp)
                )

            }
            FrontTopBar(BibleIQDataModel.showHomePage)
        }
    }
}

@Composable
fun FrontTopBar(showHomePage: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = if (showHomePage) 2.dp else 12.dp).wrapContentWidth()
    ) {
        val scope = rememberCoroutineScope()
        if (showHomePage) {
            SortBibleBooksToggle()
        } else {
            AISummaryButton(
                generateAISummary = {
                    GeminiModel.isLoading = true
                    if (!GeminiModel.showSummary) {
                        scope.launch {
                            GeminiModel.showSummary = true
                            GeminiModel.generateAISummary()
                            GeminiModel.isLoading = false
                        }
                    } else {
                        GeminiModel.showSummary = false
                    }
                },
                isAISummaryLoading = GeminiModel.isSuccessful
            )

        }
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
