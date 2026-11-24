@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ui.shorts

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
import androidx.compose.ui.viewinterop.UIKitView
import data.shorts.BiblicalShortDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive as coroutineIsActive
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.muted
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.UIKit.UIView

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
    val nsUrl = remember(streamUrl) {
        if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
            NSURL.URLWithString(streamUrl)
        } else {
            null
        }
    }
    var videoReady by remember(shortItem.id, nsUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09111E)),
    ) {
        if (nsUrl != null && isActive) {
            val player = remember(shortItem.id, nsUrl) {
                AVPlayer.playerWithURL(nsUrl).apply {
                    muted = isMuted
                }
            }
            val playerLayer = remember(shortItem.id, player) {
                AVPlayerLayer.playerLayerWithPlayer(player).apply {
                    videoGravity = AVLayerVideoGravityResizeAspectFill
                    backgroundColor = UIColor(
                        red = 9.0 / 255.0,
                        green = 17.0 / 255.0,
                        blue = 30.0 / 255.0,
                        alpha = 1.0,
                    ).CGColor
                }
            }

            LaunchedEffect(shortItem.id, playerLayer) {
                while (coroutineIsActive) {
                    if (playerLayer.readyForDisplay) {
                        videoReady = true
                        break
                    }
                    delay(35L)
                }
            }

            UIKitView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    object : UIView(frame = platform.CoreGraphics.CGRectMake(0.0, 0.0, 0.0, 0.0)) {
                        override fun layoutSubviews() {
                            super.layoutSubviews()
                            playerLayer.frame = bounds
                        }
                    }.apply {
                        clipsToBounds = true
                        backgroundColor = UIColor(
                            red = 9.0 / 255.0,
                            green = 17.0 / 255.0,
                            blue = 30.0 / 255.0,
                            alpha = 1.0,
                        )
                        layer.addSublayer(playerLayer)
                    }
                },
                update = { view ->
                    playerLayer.frame = view.bounds
                    player.muted = isMuted
                    if (isActive && !isPaused) {
                        player.play()
                    } else {
                        player.pause()
                    }
                },
            )

            LaunchedEffect(isActive, isPaused, isMuted) {
                player.muted = isMuted
                if (isActive && !isPaused) {
                    player.play()
                } else {
                    player.pause()
                }
            }

            LaunchedEffect(seekRequestId, isActive) {
                if (seekRequestId > 0 && isActive) {
                    player.seekToTime(CMTimeMake(value = seekTargetSeconds.coerceAtLeast(0).toLong(), timescale = 1))
                }
            }

            LaunchedEffect(isActive, isPaused, shortItem.id) {
                val cutoffSec = shortItem.inAppCutoffSeconds() ?: return@LaunchedEffect
                if (!isActive || isPaused || cutoffSec <= 0.0) return@LaunchedEffect
                while (coroutineIsActive) {
                    val posSec = CMTimeGetSeconds(player.currentTime())
                    if (!posSec.isNaN() && posSec >= cutoffSec) {
                        player.seekToTime(CMTimeMake(value = 0L, timescale = 1))
                        if (isActive && !isPaused) {
                            player.play()
                        }
                    }
                    delay(120L)
                }
            }

            DisposableEffect(shortItem.id, player) {
                val observer = NSNotificationCenter.defaultCenter.addObserverForName(
                    name = AVPlayerItemDidPlayToEndTimeNotification,
                    `object` = player.currentItem,
                    queue = NSOperationQueue.mainQueue,
                ) { _ ->
                    player.seekToTime(CMTimeMake(value = 0L, timescale = 1))
                    if (isActive && !isPaused) {
                        player.play()
                    }
                }
                onDispose {
                    NSNotificationCenter.defaultCenter.removeObserver(observer)
                    player.pause()
                    videoReady = false
                }
            }
        }

        AnimatedVisibility(
            visible = !videoReady || !isActive || nsUrl == null,
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
