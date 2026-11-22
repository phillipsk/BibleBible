package ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoadingScreen(
    isAppStartup: Boolean = false,
    message: String? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(color = if (isAppStartup) MaterialTheme.colors.surface else Color.Transparent)
            .testTag("loading_screen")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircularProgressIndicator(
                color = Color(0xFFFBD38D),
                strokeWidth = 4.dp,
                modifier = Modifier.size(44.dp),
            )
            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    color = Color(0xFFE2E8F0),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun AiSummaryLoadingView(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ai_summary_shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -350f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ai_shimmer_x",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(Color(0x14FFFFFF), Color(0x38FBD38D), Color(0x14FFFFFF)),
        start = Offset(shimmerX, 0f),
        end = Offset(shimmerX + 320f, 160f),
    )

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .testTag("ai_summary_loading_view"),
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0x44FBD38D)),
            backgroundColor = Color(0xFF19304F),
            elevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 26.dp),
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFBD38D),
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(44.dp),
                )
                Text(
                    text = "Generating AI summary…",
                    color = Color(0xFFFBD38D),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                LinearProgressIndicator(
                    color = Color(0xFFFBD38D),
                    backgroundColor = Color(0x33FFFFFF),
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                )
                Spacer(modifier = Modifier.height(6.dp))
                listOf(0.95f, 0.88f, 0.92f, 0.70f, 0.85f).forEach { fraction ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.Start)
                            .fillMaxWidth(fraction)
                            .height(13.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}

@Composable
fun ScriptureShimmerLoading(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scripture_shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_x",
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0x14FFFFFF),
            Color(0x36FFFFFF),
            Color(0x14FFFFFF),
        ),
        start = Offset(shimmerX, 0f),
        end = Offset(shimmerX + 360f, 180f),
    )

    val lineFractions = listOf(
        0.96f, 0.91f, 0.98f, 0.72f,
        0f,
        0.94f, 0.89f, 0.97f, 0.85f, 0.62f,
        0f,
        0.95f, 0.92f, 0.88f, 0.78f,
        0f,
        0.93f, 0.90f, 0.68f,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 22.dp)
            .testTag("scripture_shimmer_loading"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        lineFractions.forEach { fraction ->
            if (fraction <= 0f) {
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush)
                )
            }
        }
    }
}
