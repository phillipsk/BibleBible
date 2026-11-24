package ui.configs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import data.GeminiModel
import data.analytics.AnalyticsTracker
import data.bible.BibleState
import kotlinx.coroutines.launch

@Composable
internal fun ChapterTopBar(
    showHomePage: Boolean,
    onBackClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onHomeEmblemClick: () -> Unit = BibleState.onHomeClick,
    showBottomSheet: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        val bookTitle = BibleState.selectedBook.name
            ?.takeIf { it.isNotBlank() }
            ?: BibleState.selectedBook.cleanedName.orEmpty()

        TopBarTitle(
            title = bookTitle,
            showHomePage = showHomePage,
            onClick = onBackClick,
            modifier = Modifier.weight(1f),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            key(
                BibleState.selectedBook.bookId,
                BibleState.bibleChapter?.chapterId,
                BibleState.selectedVersion,
            ) {
                AISummaryTopBar()
            }
            BottomSheetToggle(showBottomSheet)
        }
    }
}

@Composable
fun BottomSheetToggle(showBottomSheet: () -> Unit) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Open Bottom Drawer",
        tint = Color.White,
        modifier = Modifier
            .padding(start = 4.dp, end = 6.dp)
            .clickable(onClick = showBottomSheet)
    )
}

@Composable
private fun AISummaryTopBar() {
    val scope = rememberCoroutineScope()
    AISummaryButton(
        generateAISummary = {
            if (!GeminiModel.showSummary) {
                AnalyticsTracker.trackAiSummary(
                    "ai_summary_toggled",
                    mapOf(
                        "visible" to "true",
                        "bookName" to (BibleState.selectedBook.cleanedName ?: ""),
                        "chapter" to (BibleState.bibleChapter?.chapterId?.toString() ?: ""),
                    ),
                )
                scope.launch {
                    GeminiModel.showSummary = true
                    GeminiModel.isLoading = true
                    try {
                        GeminiModel.generateAISummary()
                    } finally {
                        GeminiModel.isLoading = false
                    }
                }
            } else {
                AnalyticsTracker.trackAiSummary(
                    "ai_summary_toggled",
                    mapOf("visible" to "false"),
                )
                GeminiModel.showSummary = false
            }
        },
        isAISummarySuccessful = GeminiModel.isSuccessful,
        showSummary = GeminiModel.showSummary
    )
}
