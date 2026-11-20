package ui.configs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biblebible.composeapp.generated.resources.BibleBible_ico_iv
import biblebible.composeapp.generated.resources.Res
import data.GeminiModel
import data.analytics.AnalyticsTracker
import data.bibleIQ.BibleIQDataModel
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun ChapterTopBar(
    showHomePage: Boolean,
    onBackClick: () -> Unit,
    onHomeEmblemClick: () -> Unit = BibleIQDataModel.onHomeClick,
    showBottomSheet: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        // Left: Back arrow + Book name (natural width, capped cleanly so center marquee always has room)
        TopBarTitle(
            title = BibleIQDataModel.selectedBook.cleanedName.toString(),
            showHomePage = showHomePage,
            onClick = onBackClick,
            modifier = Modifier.widthIn(max = 160.dp),
        )

        // Center corridor: exact space between end of book title and start of AI button
        ChapterBrandMarquee(
            bookKey = BibleIQDataModel.selectedBook.bookId,
            onHomeClick = onHomeEmblemClick,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        )

        // Right: AI button + Settings gear
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
private fun ChapterBrandMarquee(
    bookKey: String,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clipToBounds()
            .clickable(onClick = onHomeClick)
            .testTag("chapter_top_bar_emblem"),
    ) {
        val corridorWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(48f)
        val travelPx = (corridorWidthPx * 0.55f) + with(density) { 86.dp.toPx() }
        val offsetX = remember(bookKey) { Animatable(0f) }
        val labelAlpha = remember(bookKey) { Animatable(0f) }

        LaunchedEffect(bookKey, corridorWidthPx) {
            offsetX.snapTo(0f)
            labelAlpha.snapTo(0f)
            delay(450L)

            // Fade in "BibleBible" next to the emblem, scroll left across the center corridor 3 times,
            // then return cleanly to dead-center with only the emblem visible.
            labelAlpha.animateTo(1f, animationSpec = tween(260))
            repeat(3) {
                offsetX.animateTo(
                    targetValue = -travelPx,
                    animationSpec = tween(durationMillis = 1400, easing = LinearEasing),
                )
                offsetX.snapTo(travelPx)
                offsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                )
                delay(220L)
            }
            labelAlpha.animateTo(0f, animationSpec = tween(400))
            offsetX.snapTo(0f)
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0x66FBD38D)),
                    elevation = 0.dp,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.BibleBible_ico_iv),
                        contentDescription = "BibleBible Home",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                    )
                }
                if (labelAlpha.value > 0.02f) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BibleBible",
                        maxLines = 1,
                        style = TextStyle(
                            fontFamily = MaterialTheme.typography.h1.fontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            color = Color(0xFFFBD38D),
                        ),
                        modifier = Modifier.alpha(labelAlpha.value),
                    )
                }
            }
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
