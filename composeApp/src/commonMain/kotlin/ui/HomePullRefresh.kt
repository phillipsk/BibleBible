package ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import kotlin.math.max
import kotlin.math.min

private val PullGold = Color(0xFFFBD38D)

@Stable
internal class HomePullRefreshState(
    val thresholdPx: Float,
    val holdPx: Float,
    val maxPx: Float,
) {
    var offset by mutableFloatStateOf(0f)
    var dragging by mutableStateOf(false)
    val progress: Float get() = (offset / thresholdPx).coerceIn(0f, 1f)
    val armed: Boolean get() = offset >= thresholdPx
}

// Pull from anywhere on Home. The pull only starts once the grid can no longer
// scroll up, so normal scrolling is untouched when the user is not at the top.
@Composable
internal fun HomePullRefreshBox(
    enabled: Boolean,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    indicatorTop: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val state = remember(density) {
        with(density) { HomePullRefreshState(thresholdPx = 72.dp.toPx(), holdPx = 88.dp.toPx(), maxPx = 120.dp.toPx()) }
    }
    val latestEnabled by rememberUpdatedState(enabled)
    val latestRefreshing by rememberUpdatedState(refreshing)
    val latestOnRefresh by rememberUpdatedState(onRefresh)
    val haptics = LocalHapticFeedback.current

    val connection = remember(state) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!latestEnabled || source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y < 0f && state.dragging && state.offset > 0f) {
                    val delta = max(available.y, -state.offset)
                    state.offset += delta
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!latestEnabled || latestRefreshing || source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y > 0f) {
                    state.dragging = true
                    state.offset = min(state.maxPx, state.offset + available.y * 0.55f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (state.dragging) {
                    state.dragging = false
                    if (state.armed && latestEnabled && !latestRefreshing) latestOnRefresh()
                }
                return Velocity.Zero
            }
        }
    }

    // Tick once when the pull crosses the release threshold.
    val armed = state.armed
    LaunchedEffect(armed) {
        if (armed && state.dragging) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    // Settle: hold open while loading, otherwise spring back to rest.
    LaunchedEffect(state.dragging, refreshing) {
        if (state.dragging) return@LaunchedEffect
        val target = if (refreshing) state.holdPx else 0f
        animate(
            initialValue = state.offset,
            targetValue = target,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        ) { value, _ -> state.offset = value }
    }
    Box(modifier.nestedScroll(connection)) {
        HomePullIndicator(
            state = state,
            refreshing = refreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = indicatorTop)
                .testTag("home_pull_indicator"),
        )
        Box(Modifier.graphicsLayer { translationY = state.offset }) { content() }
    }
}

@Composable
private fun HomePullIndicator(
    state: HomePullRefreshState,
    refreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress = state.progress
    val visible = if (refreshing) 1f else (0.35f + progress * 1.2f).coerceAtMost(1f).takeIf { state.offset > 0f } ?: 0f
    var spin by remember { mutableLongStateOf(0L) }
    LaunchedEffect(refreshing) {
        if (!refreshing) return@LaunchedEffect
        // Wall-clock driven so it still animates when Android animation scales are 0.
        while (true) {
            androidx.compose.runtime.withFrameNanos { spin = it / 1_000_000L }
        }
    }
    // Nothing is composed (or exposed to accessibility) while the indicator is hidden.
    if (visible <= 0f) return
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = visible },
    ) {
        val glow = if (refreshing || state.armed) 0.85f else 0.45f
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .graphicsLayer {
                    val s = if (refreshing) 1f else 0.5f + 0.5f * min(1f, progress * 1.6f)
                    scaleX = s
                    scaleY = s
                }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(PullGold.copy(alpha = glow * 0.55f), PullGold.copy(alpha = 0f)),
                            radius = size.minDimension,
                        ),
                        radius = size.minDimension,
                    )
                }
                .background(
                    Brush.radialGradient(listOf(Color(0x4DFBD38D), Color(0x1AFBD38D))),
                    CircleShape,
                )
                .border(BorderStroke(1.dp, Color(0x73FBD38D)), CircleShape),
        ) {
            Canvas(Modifier.size(40.dp)) {
                val stroke = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                val inset = stroke.width / 2f
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                val topLeft = Offset(inset, inset)
                drawArc(PullGold.copy(alpha = 0.28f), 0f, 360f, false, topLeft, arcSize, style = stroke)
                if (refreshing) {
                    val angle = (spin % 850L) / 850f * 360f
                    rotate(angle) { drawGoldArc(0f, 260f, topLeft, arcSize, stroke) }
                } else {
                    drawGoldArc(-90f, 360f * progress, topLeft, arcSize, stroke)
                }
            }
        }
        Text(
            text = when {
                refreshing -> "Refreshing"
                state.armed -> "Release to refresh"
                else -> "Pull to refresh"
            },
            style = MaterialTheme.typography.caption.copy(
                color = PullGold,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                letterSpacing = 0.8.sp,
            ),
            modifier = Modifier
                .offset { IntOffset(0, 6.dp.roundToPx()) }
                .graphicsLayer { alpha = if (refreshing) 1f else min(1f, progress * 1.6f) },
        )
    }
}

private fun DrawScope.drawGoldArc(start: Float, sweep: Float, topLeft: Offset, arcSize: Size, stroke: Stroke) {
    drawArc(PullGold, start, sweep, false, topLeft, arcSize, style = stroke)
}
