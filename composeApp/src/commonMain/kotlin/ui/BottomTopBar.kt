package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiModel
import data.bibleIQ.BibleIQDataModel

@Composable
internal fun BottomTopBar(onClick: () -> Unit, generateAISummary: () -> Unit) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 2.dp).wrapContentWidth()
            ) {
                if (!BibleIQDataModel.showHomePage) {
                    GenerateAISummaryButton(
                        generateAISummary,
                        GeminiModel.isSuccessful
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    FontSizeMenu()
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    BibleMenu(
                        bibleVersionsList = BibleIQDataModel.bibleVersions
                    )
                } else {
                    SortBibleBooksToggle()
                }
            }
        }
    }
}