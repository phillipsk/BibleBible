package ui.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 24.dp, vertical = 96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x880B1324))
                .border(1.dp, Color(0x44FBD38D), RoundedCornerShape(24.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0x33FBD38D),
                ) {
                    Text(
                        text = activeScene?.heading?.uppercase() ?: "BIBLICAL SHORT",
                        color = Color(0xFFFBD38D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0x33FFFFFF),
                ) {
                    Text(
                        text = "${shortItem.durationSeconds}s Short",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = activeScene?.caption ?: shortItem.narrative,
                style = MaterialTheme.typography.h3.copy(
                    color = Color.White,
                    fontSize = 19.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                textAlign = TextAlign.Center,
            )

            Text(
                text = "“${shortItem.keyVerseText}”",
                style = MaterialTheme.typography.body1.copy(
                    color = Color(0xFFE2E8F0),
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
                textAlign = TextAlign.Center,
            )

            if (isPaused) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xAA000000),
                ) {
                    Text(
                        text = "PAUSED",
                        color = Color(0xFFFBD38D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
