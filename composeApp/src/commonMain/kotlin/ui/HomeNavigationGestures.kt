package ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal data class EdgeTurn(
    val transition: SwipeTransition,
    val onStart: () -> Unit = {},
    val onCommit: () -> Unit,
)

// Observe until the scrollable content reaches its edge, then take over the
// remaining drag. A fling by itself never opens another screen.
internal fun Modifier.verticalEdgeTurns(
    upward: () -> EdgeTurn?,
    downward: () -> EdgeTurn? = { null },
): Modifier = composed {
    val latestUpward by rememberUpdatedState(upward)
    val latestDownward by rememberUpdatedState(downward)
    pointerInput(Unit) {
        val minimumTravel = minOf(size.height * 0.18f, 88.dp.toPx())
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (size.height == 0) return@awaitEachGesture
            val initialUpward = latestUpward()
            val initialDownward = latestDownward()
            var direction = 0
            var turn: EdgeTurn? = null
            var distance = 0f
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (event.changes.count { it.pressed } > 1 || change.isConsumed) break
                    if (!change.pressed) {
                        turn?.let { active ->
                            change.consume()
                            if (distance >= minimumTravel) {
                                active.transition.goBack(active.onCommit)
                            } else {
                                active.transition.finish(cancelled = true) {}
                            }
                            turn = null
                        }
                        break
                    }
                    val movement = change.position - change.previousPosition
                    val travel = change.position - down.position
                    val slop = viewConfiguration.touchSlop
                    val justStarted = direction == 0
                    if (justStarted) {
                        if (maxOf(abs(travel.x), abs(travel.y)) <= slop) continue
                        if (abs(travel.x) >= abs(travel.y)) break
                        direction = if (travel.y < 0) -1 else 1
                    }
                    if (turn == null) {
                        if (movement.y * direction <= 0f) continue
                        val candidate = if (direction < 0) latestUpward() else latestDownward()
                        if (candidate == null || candidate.transition.isCommitting) continue
                        turn = candidate
                        candidate.onStart()
                        val beganAtEdge = if (direction < 0) initialUpward != null else initialDownward != null
                        distance = if (justStarted && beganAtEdge) abs(travel.y) - slop else abs(movement.y)
                    } else {
                        distance = (distance + movement.y * direction).coerceAtLeast(0f)
                    }
                    turn.transition.dragTo(distance)
                    change.consume()
                }
            } finally {
                turn?.transition?.finish(cancelled = true) {}
            }
        }
    }
}

internal fun Modifier.homeSwipeToWords(openWords: () -> Unit): Modifier = pointerInput(Unit) {
    val minimumTravel = 72.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var swiping = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (event.changes.count { it.pressed } > 1 || change.isConsumed) break
            val travel = change.position - down.position
            if (!change.pressed) {
                if (swiping && travel.x <= -minimumTravel) {
                    change.consume()
                    openWords()
                }
                break
            }
            if (!swiping) {
                val slop = viewConfiguration.touchSlop
                if (maxOf(abs(travel.x), abs(travel.y)) <= slop) continue
                if (travel.x >= -slop || abs(travel.x) <= abs(travel.y)) break
                swiping = true
            }
            change.consume()
        }
    }
}
