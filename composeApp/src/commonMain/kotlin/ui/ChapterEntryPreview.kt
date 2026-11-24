package ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ChapterEntryPreview(bookName: String, chapter: Int, showGenesisVerse: Boolean, progress: () -> Float) {
    val background = MaterialTheme.colors.background
    val tint = MaterialTheme.colors.primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clearAndSetSemantics { hideFromAccessibility() }
            .graphicsLayer {
                val fraction = progress()
                translationY = size.height * (1f - fraction)
                alpha = (fraction * 3f).coerceAtMost(1f)
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                clip = fraction < 1f
            }
            .drawWithContent {
                val fraction = progress()
                val glass = (1f - fraction).coerceIn(0f, 1f)
                val surfaceAlpha = 1f - glass * 0.28f
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            tint.copy(alpha = glass * 0.12f).compositeOver(background).copy(alpha = surfaceAlpha),
                            background.copy(alpha = surfaceAlpha),
                        ),
                    ),
                )
                drawContent()
                if (glass > 0f) {
                    drawLine(
                        color = Color.White.copy(alpha = glass * 0.38f),
                        start = Offset.Zero,
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 25.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            bookName,
            style = MaterialTheme.typography.h1.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colors.onBackground,
        )
        Text(
            "Chapter $chapter",
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.secondary,
        )
        if (showGenesisVerse) {
            Text(
                "In the beginning God created the heaven and the earth.",
                modifier = Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.body1.copy(fontSize = 20.sp, lineHeight = 32.sp),
                color = MaterialTheme.colors.onBackground,
            )
        }
    }
}
