package ui.shorts

import android.graphics.Matrix
import android.os.Build
import android.view.TextureView
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import data.shorts.BiblicalShortDto
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive as coroutineIsActive

private fun isAndroidEmulator(): Boolean {
    return Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
        Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
        Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
        Build.FINGERPRINT.contains("emu", ignoreCase = true) ||
        Build.MODEL.contains("Emulator", ignoreCase = true) ||
        Build.MODEL.contains("sdk_gphone", ignoreCase = true)
}

@kotlin.OptIn(UnstableApi::class)
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
    var exoPlayerRef by remember(shortItem.id, streamUrl) { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(seekRequestId, isActive) {
        if (seekRequestId > 0 && isActive && videoReady) {
            runCatching {
                exoPlayerRef?.seekTo((seekTargetSeconds.coerceAtLeast(0)) * 1000L)
            }
        }
    }

    LaunchedEffect(isActive, isPaused, videoReady, shortItem.id) {
        val cutoffMs = shortItem.inAppCutoffSeconds()?.let { (it * 1000.0).toLong() } ?: return@LaunchedEffect
        if (!isActive || isPaused || !videoReady || cutoffMs <= 0L) return@LaunchedEffect
        while (coroutineIsActive) {
            val player = exoPlayerRef
            if (player != null) {
                val posMs = runCatching {
                    if (player.isPlaying) player.currentPosition else -1L
                }.getOrDefault(-1L)
                if (posMs >= cutoffMs) {
                    runCatching { player.seekTo(0L) }
                }
            }
            delay(120L)
        }
    }

    LaunchedEffect(isActive, isPaused, isMuted, videoReady) {
        val player = exoPlayerRef ?: return@LaunchedEffect
        runCatching {
            player.volume = if (isMuted) 0f else 1f
            player.playWhenReady = isActive && !isPaused
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09111E)),
    ) {
        if (hasValidStream && isActive) {
            key(shortItem.id, streamUrl) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        var knownVideoWidth = 0
                        var knownVideoHeight = 0

                        val textureView = TextureView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                            )
                            isOpaque = false
                        }

                        fun applyCenterCropTransform() {
                            val vw = textureView.width
                            val vh = textureView.height
                            if (vw <= 0 || vh <= 0 || knownVideoWidth <= 0 || knownVideoHeight <= 0) return
                            val viewRatio = vw.toFloat() / vh.toFloat()
                            val videoRatio = knownVideoWidth.toFloat() / knownVideoHeight.toFloat()
                            val scaleX: Float
                            val scaleY: Float
                            if (videoRatio > viewRatio) {
                                scaleX = videoRatio / viewRatio
                                scaleY = 1f
                            } else {
                                scaleX = 1f
                                scaleY = viewRatio / videoRatio
                            }
                            val matrix = Matrix().apply {
                                setScale(scaleX, scaleY, vw / 2f, vh / 2f)
                            }
                            textureView.setTransform(matrix)
                        }

                        textureView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                            if ((right - left) != (oldRight - oldLeft) || (bottom - top) != (oldBottom - oldTop)) {
                                applyCenterCropTransform()
                            }
                        }

                        val renderersFactory = DefaultRenderersFactory(context)
                            .setEnableDecoderFallback(true)
                            .setMediaCodecSelector(
                                MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                                    val defaults = MediaCodecSelector.DEFAULT.getDecoderInfos(
                                        mimeType,
                                        requiresSecureDecoder,
                                        requiresTunnelingDecoder,
                                    )
                                    if (isAndroidEmulator()) {
                                        val nonGoldfish = defaults.filterNot {
                                            it.name.contains("goldfish", ignoreCase = true)
                                        }
                                        val selected = nonGoldfish.ifEmpty { defaults }
                                        if (mimeType.startsWith("video/")) {
                                            Napier.d(
                                                "ShortsVideo emulator codec selection for $mimeType: ${selected.joinToString { it.name }}",
                                                tag = "ShortsVideo",
                                            )
                                        }
                                        selected
                                    } else {
                                        defaults
                                    }
                                },
                            )

                        runCatching {
                            ExoPlayer.Builder(context, renderersFactory)
                                .build()
                                .apply {
                                    setAudioAttributes(
                                        AudioAttributes.Builder()
                                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                                            .setUsage(C.USAGE_MEDIA)
                                            .build(),
                                        false,
                                    )
                                    repeatMode = Player.REPEAT_MODE_ONE
                                    volume = if (isMuted) 0f else 1f
                                    playWhenReady = isActive && !isPaused
                                    setVideoTextureView(textureView)
                                    addListener(object : Player.Listener {
                                        override fun onVideoSizeChanged(videoSize: VideoSize) {
                                            if (videoSize.width > 0 && videoSize.height > 0) {
                                                knownVideoWidth = videoSize.width
                                                knownVideoHeight = videoSize.height
                                                textureView.post { applyCenterCropTransform() }
                                            }
                                        }

                                        override fun onRenderedFirstFrame() {
                                            videoReady = true
                                        }

                                        override fun onPlayerError(error: PlaybackException) {
                                            Napier.w(
                                                "ExoPlayer error: ${error.message} url=$streamUrl",
                                                error,
                                                tag = "ShortsVideo",
                                            )
                                            videoReady = false
                                        }
                                    })
                                    setMediaItem(MediaItem.fromUri(streamUrl))
                                    prepare()
                                    exoPlayerRef = this
                                }
                        }.onFailure { err ->
                            Napier.e("ExoPlayer setup failed: ${err.message}", err, tag = "ShortsVideo")
                        }

                        FrameLayout(context).apply {
                            clipChildren = true
                            clipToOutline = true
                            addView(textureView)
                        }
                    },
                    update = {
                        val player = exoPlayerRef ?: return@AndroidView
                        runCatching {
                            player.volume = if (isMuted) 0f else 1f
                            player.playWhenReady = isActive && !isPaused
                        }
                    },
                    onRelease = { view ->
                        val texture = (view as? FrameLayout)?.getChildAt(0) as? TextureView
                        runCatching {
                            exoPlayerRef?.let { player ->
                                if (texture != null) {
                                    player.clearVideoTextureView(texture)
                                }
                                player.release()
                            }
                        }
                        exoPlayerRef = null
                        videoReady = false
                    },
                )

                DisposableEffect(shortItem.id, streamUrl) {
                    onDispose {
                        runCatching { exoPlayerRef?.release() }
                        exoPlayerRef = null
                        videoReady = false
                    }
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
