package ui.shorts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import data.shorts.BiblicalShortDto

@Composable
internal actual fun ShortsVideoPlayer(
    shortItem: BiblicalShortDto,
    isActive: Boolean,
    isMuted: Boolean,
    isPaused: Boolean,
    elapsedSeconds: Int,
    seekTargetSeconds: Int,
    seekRequestId: Int,
    modifier: Modifier,
) {
    ShortsStoryboardBackdrop(
        shortItem = shortItem,
        elapsedSeconds = elapsedSeconds,
        isPaused = isPaused,
        modifier = modifier,
    )
}
