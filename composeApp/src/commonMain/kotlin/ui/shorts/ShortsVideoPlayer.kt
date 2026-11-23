package ui.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.shorts.BiblicalShortDto

@Composable
internal expect fun ShortsVideoPlayer(
    shortItem: BiblicalShortDto,
    isActive: Boolean,
    isMuted: Boolean,
    isPaused: Boolean,
    elapsedSeconds: Int,
    seekTargetSeconds: Int = 0,
    seekRequestId: Int = 0,
    modifier: Modifier = Modifier,
)

internal fun parseAccentColor(hex: String, fallback: Color = Color(0xFF3A5F8F)): Color {
    val cleaned = hex.trim().removePrefix("#")
    val parsed = cleaned.toLongOrNull(16) ?: return fallback
    return when (cleaned.length) {
        6 -> Color((0xFF000000L or parsed).toInt())
        8 -> Color(parsed.toInt())
        else -> fallback
    }
}

@Composable
internal fun ShortsStoryboardBackdrop(
    shortItem: BiblicalShortDto,
    elapsedSeconds: Int,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = parseAccentColor(shortItem.accentColorHex)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF09111E),
            accent.copy(alpha = 0.85f),
            Color(0xFF0F172A),
        )
    )

    val activeScene = shortItem.scenes.firstOrNull { scene ->
        elapsedSeconds >= scene.startSeconds && elapsedSeconds < scene.endSeconds
    } ?: shortItem.scenes.lastOrNull()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center,
    ) {
        val isCompact = maxHeight < 380.dp
        val isLargeFrame = maxWidth >= 460.dp && maxHeight >= 520.dp
        val showVerse = maxHeight >= 280.dp

        val outerHPad = if (isCompact) 14.dp else 20.dp
        val outerVPad = if (isCompact) 14.dp else 28.dp
        val cardPad = when {
            isCompact -> 16.dp
            isLargeFrame -> 28.dp
            else -> 24.dp
        }
        val pillFontSize = when {
            isCompact -> 11.5.sp
            isLargeFrame -> 15.5.sp
            else -> 14.sp
        }
        val titleFontSize = when {
            isCompact -> 20.sp
            isLargeFrame -> 32.sp
            else -> 28.sp
        }
        val titleLineHeight = when {
            isCompact -> 24.sp
            isLargeFrame -> 38.sp
            else -> 34.sp
        }
        val hookFontSize = when {
            isCompact -> 15.sp
            isLargeFrame -> 23.sp
            else -> 21.sp
        }
        val hookLineHeight = when {
            isCompact -> 21.sp
            isLargeFrame -> 31.sp
            else -> 29.sp
        }
        val verseFontSize = when {
            isCompact -> 13.5.sp
            isLargeFrame -> 18.5.sp
            else -> 17.sp
        }
        val verseLineHeight = when {
            isCompact -> 19.sp
            isLargeFrame -> 26.sp
            else -> 24.sp
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = outerHPad, vertical = outerVPad)
                .clip(RoundedCornerShape(if (isCompact) 18.dp else 26.dp))
                .background(Color(0xA60B1324))
                .border(
                    1.dp,
                    Color(0x55FBD38D),
                    RoundedCornerShape(if (isCompact) 18.dp else 26.dp),
                )
                .padding(cardPad),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0x38FBD38D),
                    elevation = 0.dp,
                ) {
                    Text(
                        text = shortItem.scriptureReference.ifBlank {
                            activeScene?.heading?.uppercase() ?: "BIBLICAL WORD"
                        },
                        color = Color(0xFFFBD38D),
                        fontSize = pillFontSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            horizontal = if (isCompact) 12.dp else 16.dp,
                            vertical = if (isCompact) 4.dp else 6.dp,
                        ),
                    )
                }
            }

            Text(
                text = shortItem.title,
                style = MaterialTheme.typography.h1.copy(
                    color = Color.White,
                    fontSize = titleFontSize,
                    lineHeight = titleLineHeight,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = activeScene?.caption ?: shortItem.narrative,
                style = MaterialTheme.typography.h3.copy(
                    color = Color(0xFFF8FAFC),
                    fontSize = hookFontSize,
                    lineHeight = hookLineHeight,
                    fontWeight = FontWeight.SemiBold,
                ),
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )

            if (showVerse && shortItem.keyVerseText.isNotBlank()) {
                Text(
                    text = "“${shortItem.keyVerseText}”",
                    style = MaterialTheme.typography.body1.copy(
                        color = Color(0xFFE2E8F0),
                        fontStyle = FontStyle.Italic,
                        fontSize = verseFontSize,
                        lineHeight = verseLineHeight,
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isPaused) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xAA000000),
                ) {
                    Text(
                        text = "PAUSED",
                        color = Color(0xFFFBD38D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}
