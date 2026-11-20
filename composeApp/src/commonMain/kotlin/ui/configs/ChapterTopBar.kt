package ui.configs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biblebible.composeapp.generated.resources.BibleBible_ico_iv
import biblebible.composeapp.generated.resources.Res
import data.GeminiModel
import data.analytics.AnalyticsTracker
import data.bibleIQ.BibleIQDataModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun ChapterTopBar(
    showHomePage: Boolean,
    onBackClick: () -> Unit,
    onHomeEmblemClick: () -> Unit = BibleIQDataModel.onHomeClick,
    showBottomSheet: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        val bookTitle = BibleIQDataModel.selectedBook.cleanedName.toString()
        // Show full dead-center icon + gold "BibleBible" whenever the title leaves breathing room
        val showBrandTitle = maxWidth >= 340.dp && bookTitle.length <= 10

        TopBarTitle(
            title = bookTitle,
            showHomePage = showHomePage,
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .widthIn(max = if (showBrandTitle) 132.dp else 155.dp),
        )

        // Dead-center BibleBible icon + gold title horizontally across the entire top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .clickable(onClick = onHomeEmblemClick)
                .testTag("chapter_top_bar_emblem"),
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, Color(0x66FBD38D)),
                elevation = 0.dp,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape),
            ) {
                Image(
                    painter = painterResource(Res.drawable.BibleBible_ico_iv),
                    contentDescription = "BibleBible Home",
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape),
                )
            }
            if (showBrandTitle) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BibleBible",
                    maxLines = 1,
                    style = TextStyle(
                        fontFamily = MaterialTheme.typography.h1.fontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = Color(0xFFFBD38D),
                    ),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            key(
                BibleIQDataModel.selectedBook.bookId,
                BibleIQDataModel.bibleChapter?.chapterId,
                BibleIQDataModel.selectedVersion,
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
                        "bookName" to (BibleIQDataModel.selectedBook.cleanedName ?: ""),
                        "chapter" to (BibleIQDataModel.bibleChapter?.chapterId?.toString() ?: ""),
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
