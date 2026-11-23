package ui.shorts

import android.graphics.Color as AndroidColor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import data.shorts.BiblicalShortDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive as coroutineIsActive

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
    val streamUrl = shortItem.videoUrl.trim()
    val hasValidStream = streamUrl.startsWith("http://") || streamUrl.startsWith("https://")
    var videoReady by remember(shortItem.id, streamUrl) { mutableStateOf(false) }
    var mediaPlayerRef by remember(shortItem.id, streamUrl) { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(seekRequestId, isActive) {
        if (seekRequestId > 0 && isActive && videoReady) {
            runCatching {
                mediaPlayerRef?.seekTo((seekTargetSeconds.coerceAtLeast(0)) * 1000)
            }
        }
    }

    LaunchedEffect(isActive, isPaused, videoReady, shortItem.id) {
        val cutoffMs = shortItem.inAppCutoffSeconds()?.let { (it * 1000.0).toLong() } ?: return@LaunchedEffect
        if (!isActive || isPaused || !videoReady || cutoffMs <= 0L) return@LaunchedEffect
        while (coroutineIsActive) {
            val player = mediaPlayerRef
            if (player != null) {
                val posMs = runCatching {
                    if (player.isPlaying) player.currentPosition else -1
                }.getOrDefault(-1)
                if (posMs >= cutoffMs) {
                    runCatching { player.seekTo(0) }
                }
            }
            delay(120L)
        }
    }

    LaunchedEffect(isActive, isPaused, isMuted, videoReady) {
        val player = mediaPlayerRef ?: return@LaunchedEffect
        val volume = if (isMuted) 0f else 1f
        runCatching {
            player.setVolume(volume, volume)
            if (videoReady) {
                if (isActive && !isPaused && !player.isPlaying) {
                    player.start()
                } else if ((!isActive || isPaused) && player.isPlaying) {
                    player.pause()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09111E)),
    ) {
        if (hasValidStream && isActive) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    FrameLayout(context).apply {
                        setBackgroundColor(AndroidColor.rgb(9, 17, 30))
                        clipChildren = true
                        clipToOutline = true
                        val surfaceView = SurfaceView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                Gravity.CENTER,
                            )
                        }
                        addView(surfaceView)
                        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                runCatching {
                                    MediaPlayer().apply {
                                        setAudioAttributes(
                                            AudioAttributes.Builder()
                                                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                                .build()
                                        )
                                        setDisplay(holder)
                                        setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                        setDataSource(streamUrl)
                                        isLooping = true
                                        setOnInfoListener { _, what, _ ->
                                            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                                videoReady = true
                                            }
                                            false
                                        }
                                        setOnPreparedListener { player ->
                                            val volume = if (isMuted) 0f else 1f
                                            player.setVolume(volume, volume)
                                            if (isActive && !isPaused) {
                                                player.start()
                                            }
                                            videoReady = true
                                        }
                                        setOnErrorListener { _, _, _ ->
                                            videoReady = false
                                            true
                                        }
                                        prepareAsync()
                                        mediaPlayerRef = this
                                    }
                                }
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int,
                            ) = Unit

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                runCatching { mediaPlayerRef?.release() }
                                mediaPlayerRef = null
                                videoReady = false
                            }
                        })
                    }
                },
                update = {
                    val player = mediaPlayerRef ?: return@AndroidView
                    val volume = if (isMuted) 0f else 1f
                    runCatching {
                        player.setVolume(volume, volume)
                        if (videoReady) {
                            if (isActive && !isPaused && !player.isPlaying) {
                                player.start()
                            } else if ((!isActive || isPaused) && player.isPlaying) {
                                player.pause()
                            }
                        }
                    }
                },
            )

            DisposableEffect(shortItem.id, streamUrl) {
                onDispose {
                    runCatching { mediaPlayerRef?.release() }
                    mediaPlayerRef = null
                    videoReady = false
                }
            }
        }

        AnimatedVisibility(
            visible = !videoReady || !isActive || !hasValidStream,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize(),
        ) {
            ShortsStoryboardBackdrop(
                shortItem = shortItem,
                elapsedSeconds = elapsedSeconds,
                isPaused = isPaused,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
