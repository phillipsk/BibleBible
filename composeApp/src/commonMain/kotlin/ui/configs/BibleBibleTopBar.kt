package ui.configs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import data.bible.BibleState
import ui.configs.home.HomeTopBar

@Composable
internal fun BibleBibleTopBar(
    onClick: () -> Unit,
    showBottomSheet: () -> Unit,
    showHomePage: Boolean = BibleState.showHomePage,
) {
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
            if (showHomePage) {
                BibleBookTopBar()
            } else {
                ChapterTopBar(
                    showHomePage = showHomePage,
                    onBackClick = onClick,
                    onHomeEmblemClick = BibleState.onHomeClick,
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

internal val HomeTopBarHeight = 56.dp

@Composable
internal fun FloatingHomeTopBar(progress: () -> Float) {
    val solidTint = MaterialTheme.colors.primarySurface
    Box(
        Modifier.fillMaxWidth().drawBehind {
            val statusHeight = (size.height - HomeTopBarHeight.toPx()).coerceAtLeast(0f)
            drawRect(
                color = solidTint.copy(alpha = 1f - progress() * 0.08f),
                size = Size(size.width, statusHeight),
            )
        }.statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HomeTopBarHeight)
                .testTag("floating_home_top_bar")
                .drawBehind {
                    val fraction = progress()
                    val inset = 10.dp.toPx() * fraction
                    val radius = CornerRadius(16.dp.toPx() * fraction)
                    val surfaceSize = Size((size.width - inset * 2).coerceAtLeast(0f), size.height)
                    val surfaceStart = Offset(inset, 0f)
                    drawRoundRect(
                        color = solidTint.copy(alpha = 1f - fraction * 0.23f),
                        topLeft = surfaceStart,
                        size = surfaceSize,
                        cornerRadius = radius,
                    )
                    if (fraction > 0f) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = fraction * 0.22f),
                            topLeft = surfaceStart,
                            size = surfaceSize,
                            cornerRadius = radius,
                            style = Stroke(1.dp.toPx()),
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.padding(horizontal = 10.dp)) { BibleBookTopBar() }
        }
    }
}
