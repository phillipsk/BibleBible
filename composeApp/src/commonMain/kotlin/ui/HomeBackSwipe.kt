package ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

internal class SwipeTransition(private val scope: CoroutineScope) {
    var offset by mutableFloatStateOf(0f)
        private set

    var extent = 0
    val progress: Float get() = if (extent > 0) offset / extent else 0f
    private var animation: Job? = null
    var isCommitting = false
        private set

    fun drag(delta: Float) = dragTo(offset + delta)

    fun dragTo(position: Float) {
        if (isCommitting) return
        animation?.cancel()
        offset = position.coerceIn(0f, extent.toFloat())
    }

    fun finish(cancelled: Boolean, onBack: () -> Unit) {
        if (isCommitting || offset == 0f) return
        if (!cancelled && progress >= 0.24f) {
            goBack(onBack)
        } else {
            settle(0f) {}
        }
    }

    fun goBack(onBack: () -> Unit) {
        if (isCommitting) return
        isCommitting = true
        settle(extent.toFloat()) {
            onBack()
            offset = 0f
            isCommitting = false
        }
    }

    private fun settle(target: Float, onFinished: () -> Unit) {
        animation?.cancel()
        animation = scope.launch {
            animate(offset, target, animationSpec = tween(220)) { value, _ ->
                offset = value
            }
            onFinished()
        }
    }
}

// Claim a rightward drag before the chapter pager sees it. Leftward and multi-touch
// gestures stay with the pager and scripture content.
internal fun Modifier.readerRightBackSwipe(
    canStart: () -> Boolean,
    onDrag: (Float) -> Unit,
    onFinish: (cancelled: Boolean) -> Unit,
): Modifier = composed {
    val latestCanStart by rememberUpdatedState(canStart)
    val latestOnDrag by rememberUpdatedState(onDrag)
    val latestOnFinish by rememberUpdatedState(onFinish)
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (!latestCanStart()) return@awaitEachGesture

            var dragging = false
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (event.changes.count { it.pressed } > 1 || change.isConsumed) break
                    if (!change.pressed) {
                        if (dragging) {
                            change.consume()
                            dragging = false
                            latestOnFinish(false)
                        }
                        break
                    }

                    if (dragging) {
                        latestOnDrag(change.position.x - change.previousPosition.x)
                        change.consume()
                    } else {
                        val travel = change.position - down.position
                        val slop = viewConfiguration.touchSlop
                        if (abs(travel.y) > slop || travel.x < -slop) break
                        if (travel.x > slop) {
                            dragging = true
                            latestOnDrag(travel.x - slop)
                            change.consume()
                        }
                    }
                }
            } finally {
                if (dragging) latestOnFinish(true)
            }
        }
    }
}
