package ui.shorts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.analytics.AnalyticsTracker
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BiblicalShortsEntryCard(
    onOpenShorts: () -> Unit,
    modifier: Modifier = Modifier,
    animateArrow: Boolean = true,
) {
    var arrowProgress by remember { mutableStateOf(0f) }
    var dragAccumulated by remember { mutableStateOf(0f) }

    LaunchedEffect(animateArrow) {
        if (!animateArrow) {
            arrowProgress = 0.5f
            return@LaunchedEffect
        }
        var phase = 0.0
        while (isActive) {
            withContext(Dispatchers.Default) {
                delay(40L)
            }
            phase = (phase + 0.14) % (2.0 * PI)
            // Smooth 0..1 left-to-right swipe pulse
            arrowProgress = ((sin(phase) + 1.0) * 0.5).toFloat()
        }
    }

    val cardBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF1E3A5F),
            Color(0xFF2B4C7E),
            Color(0xFF3A5F8F),
        )
    )

    Card(
        onClick = {
            AnalyticsTracker.track(
                "shorts_entry_clicked",
                "biblical_shorts",
                mapOf("trigger" to "card_tap"),
            )
            onOpenShorts()
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("biblical_shorts_entry_card")
            .pointerInput(onOpenShorts) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulated = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulated += dragAmount
                        if (dragAccumulated > 64f || dragAccumulated < -64f) {
                            dragAccumulated = 0f
                            AnalyticsTracker.track(
                                "shorts_entry_clicked",
                                "biblical_shorts",
                                mapOf("trigger" to "horizontal_swipe"),
                            )
                            onOpenShorts()
                        }
                    },
                )
            },
        shape = RoundedCornerShape(20.dp),
        elevation = 4.dp,
        border = BorderStroke(1.dp, Color(0x55FBD38D)),
        backgroundColor = Color(0xFF1E3A5F),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FBD38D)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Watch Biblical Shorts",
                        tint = Color(0xFFFBD38D),
                        modifier = Modifier.size(26.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Biblical Words",
                            style = MaterialTheme.typography.subtitle1.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp,
                            ),
                        )
                    }
                    Text(
                        text = "Short scripture moments to inspire your day",
                        style = MaterialTheme.typography.body2.copy(
                            color = Color(0xFFDCE6F2),
                            fontSize = 12.sp,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0x33FBD38D),
                    elevation = 0.dp,
                ) {
                    Text(
                        text = "Watch",
                        color = Color(0xFFFBD38D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }

                // Animated left-to-right swiping arrow track
                Surface(
                    onClick = {
                        AnalyticsTracker.track(
                            "shorts_entry_clicked",
                            "biblical_shorts",
                            mapOf("trigger" to "arrow_tap"),
                        )
                        onOpenShorts()
                    },
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF0F233D),
                    border = BorderStroke(1.dp, Color(0x66FBD38D)),
                    modifier = Modifier.testTag("biblical_shorts_entry_arrow"),
                ) {
                    Row(
                        modifier = Modifier
                            .width(64.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Open Biblical Shorts",
                            tint = Color(0xFFFBD38D),
                            modifier = Modifier
                                .size(20.dp)
                                .offset {
                                    IntOffset(x = (arrowProgress * 26.dp.toPx()).roundToInt(), y = 0)
                                },
                        )
                    }
                }
            }
        }
    }
}
