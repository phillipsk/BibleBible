package ui.shorts

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.viewinterop.AndroidView
import data.shorts.BiblicalShortDto

/**
 * Applies a center-crop aspect-ratio transform so 9:16 video streams fill any phone, foldable,
 * or theater viewport cleanly without stretching or squishing.
 */
private fun updateTextureCenterCropTransform(
    textureView: TextureView,
    videoWidth: Int,
    videoHeight: Int,
    viewWidth: Int,
    viewHeight: Int,
) {
    if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return
    val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
    val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()
    val scaleX: Float
    val scaleY: Float
    if (videoAspect > viewAspect) {
        scaleX = videoAspect / viewAspect
        scaleY = 1f
    } else {
        scaleX = 1f
        scaleY = viewAspect / videoAspect
    }
    val matrix = Matrix().apply {
        setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    }
    textureView.setTransform(matrix)
}

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
    var videoReady by remember(shortItem.id) { mutableStateOf(false) }
    var mediaPlayerRef by remember(shortItem.id) { mutableStateOf<MediaPlayer?>(null) }
    var videoSize by remember(shortItem.id) { mutableStateOf(0 to 0) }

    LaunchedEffect(seekRequestId, isActive) {
        if (seekRequestId > 0 && isActive && videoReady) {
            runCatching {
                mediaPlayerRef?.seekTo((seekTargetSeconds.coerceAtLeast(0)) * 1000)
            }
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

    Box(modifier = modifier.fillMaxSize()) {
        ShortsStoryboardBackdrop(
            shortItem = shortItem,
            elapsedSeconds = elapsedSeconds,
            isPaused = isPaused,
            modifier = Modifier.fillMaxSize(),
        )

        val streamUrl = shortItem.videoUrl.trim()
        if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (videoReady) 1f else 0f),
                factory = { context ->
                    TextureView(context).apply {
                        val textureView = this
                        isOpaque = false
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                                val surface = Surface(surfaceTexture)
                                runCatching {
                                    MediaPlayer().apply {
                                        setAudioAttributes(
                                            AudioAttributes.Builder()
                                                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                                .build()
                                        )
                                        setSurface(surface)
                                        setDataSource(streamUrl)
                                        isLooping = true
                                        setOnVideoSizeChangedListener { _, vWidth, vHeight ->
                                            videoSize = vWidth to vHeight
                                            updateTextureCenterCropTransform(
                                                textureView,
                                                vWidth,
                                                vHeight,
                                                width,
                                                height,
                                            )
                                        }
                                        setOnPreparedListener { player ->
                                            val volume = if (isMuted) 0f else 1f
                                            player.setVolume(volume, volume)
                                            videoSize = player.videoWidth to player.videoHeight
                                            updateTextureCenterCropTransform(
                                                textureView,
                                                player.videoWidth,
                                                player.videoHeight,
                                                width,
                                                height,
                                            )
                                            videoReady = true
                                            if (isActive && !isPaused) {
                                                player.start()
                                            }
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

                            override fun onSurfaceTextureSizeChanged(
                                surfaceTexture: SurfaceTexture,
                                viewWidth: Int,
                                viewHeight: Int,
                            ) {
                                updateTextureCenterCropTransform(
                                    textureView,
                                    videoSize.first,
                                    videoSize.second,
                                    viewWidth,
                                    viewHeight,
                                )
                            }

                            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                                runCatching {
                                    mediaPlayerRef?.release()
                                }
                                mediaPlayerRef = null
                                videoReady = false
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
                        }
                    }
                },
                update = { textureView ->
                    updateTextureCenterCropTransform(
                        textureView,
                        videoSize.first,
                        videoSize.second,
                        textureView.width,
                        textureView.height,
                    )
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

            LaunchedEffect(isActive, isPaused, isMuted, videoReady) {
                val player = mediaPlayerRef ?: return@LaunchedEffect
                runCatching {
                    val volume = if (isMuted) 0f else 1f
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

            DisposableEffect(shortItem.id) {
                onDispose {
                    runCatching { mediaPlayerRef?.release() }
                    mediaPlayerRef = null
                    videoReady = false
                }
            }
        }
    }
}
