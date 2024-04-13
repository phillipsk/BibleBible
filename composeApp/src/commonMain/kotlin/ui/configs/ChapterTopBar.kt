package ui.configs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import data.GeminiModel
import data.bibleIQ.BibleIQDataModel
import kotlinx.coroutines.launch

@Composable
internal fun ChapterTopBar(showHomePage: Boolean, onClick: () -> Unit, showBottomSheet: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarTitle(BibleIQDataModel.selectedBook.cleanedName.toString(), showHomePage, onClick)
        Spacer(modifier = Modifier.weight(1f))
        AISummaryTopBar()
        Spacer(modifier = Modifier.weight(0.05f))
        BottomSheetToggle(showBottomSheet)
    }
}

@Composable
fun BottomSheetToggle(showBottomSheet: () -> Unit) {
    Icon(
        imageVector = Icons.Default.MoreVert,
        contentDescription = "Open Bottom Drawer",
        tint = Color.White,
        modifier = Modifier.padding(start = 4.dp)
            .clickable(onClick = showBottomSheet)

    )
}

@Composable
private fun AISummaryTopBar() {
    val scope = rememberCoroutineScope()
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
        isAISummaryLoading = GeminiModel.isLoading,
        isAISummarySuccessful = GeminiModel.isSuccessful
    )
}