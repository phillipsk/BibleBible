package ui.shorts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.analytics.AnalyticsTracker
import data.bible.BibleState
import data.bible.currentTimeMillis
import data.shorts.BiblicalShortDto
import data.shorts.ShortsPlaybackLogic
import data.shorts.ShortsProgressManager
import data.shorts.biblicalShortsCategories
import data.shorts.biblicalShortsRepository
import data.shorts.defaultBiblicalShortsCatalog
import ui.computeAdaptiveWindowMetrics
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun BiblicalShortsScreen(
    onBack: () -> Unit,
    onReadChapter: (BiblicalShortDto) -> Unit,
    modifier: Modifier = Modifier,
    initialShorts: List<BiblicalShortDto> = emptyList(),
    selectedCategory: String = BibleState.selectedShortsCategory,
    onCategorySelected: (String) -> Unit = { BibleState.selectedShortsCategory = it },
    enablePlaybackTimer: Boolean = true,
    resumeShortId: String? = null,
    progressManager: ShortsProgressManager = remember { ShortsProgressManager() },
    videoPreviewOverride: (@Composable (BiblicalShortDto, Modifier) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val useDeterministicOrder = initialShorts.isNotEmpty()
    var allShorts by remember(initialShorts, resumeShortId) {
        mutableStateOf(
            if (useDeterministicOrder) {
                initialShorts
            } else {
                progressManager.buildFreshSessionDeck(
                    shorts = defaultBiblicalShortsCatalog(),
                    resumeShortId = resumeShortId,
                )
            }
        )
    }
    // Default sound OFF so playback starts quietly until the user double-taps or toggles sound
    var isMuted by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }
    var isAppInForeground by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var completedPlaysOnCurrentShort by remember { mutableStateOf(0) }
    var lastLoopEventMs by remember { mutableStateOf(0L) }
    var lastUserInteractionMs by remember { mutableStateOf(currentTimeMillis()) }
    var consecutiveAutoAdvances by remember { mutableStateOf(0) }
    var pendingAutoAdvance by remember { mutableStateOf(false) }
    val markUserInteraction: () -> Unit = {
        lastUserInteractionMs = currentTimeMillis()
        consecutiveAutoAdvances = 0
    }
    var seekTargetSeconds by remember { mutableStateOf(0) }
    var seekRequestId by remember { mutableStateOf(0) }
    var showSwipeHint by remember { mutableStateOf(false) }
    var showGestureLegend by remember { mutableStateOf(false) }
    var lastLegendEvaluatedPage by remember { mutableStateOf(-1) }
    var showTutorialOverlay by remember { mutableStateOf(progressManager.shouldShowTutorial()) }
    var hintBounceOffsetPx by remember { mutableStateOf(0) }
    var soundToastLabel by remember { mutableStateOf<String?>(null) }
    var hasLoadedRemoteCatalog by remember { mutableStateOf(false) }

    // Pause audio & video immediately when backgrounded; resume automatically on foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    isAppInForeground = false
                }
                Lifecycle.Event.ON_RESUME -> {
                    isAppInForeground = true
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(initialShorts) {
        if (initialShorts.isEmpty() && !hasLoadedRemoteCatalog) {
            val fetched = biblicalShortsRepository.getShorts()
            // Preserve the currently playing item at index 0 while updating video URLs from the backend
            val currentFirstId = allShorts.firstOrNull()?.id ?: resumeShortId
            allShorts = progressManager.buildFreshSessionDeck(
                shorts = fetched,
                resumeShortId = currentFirstId,
            )
            hasLoadedRemoteCatalog = true
        }
    }

    val filteredShorts = remember(allShorts, selectedCategory) {
        biblicalShortsRepository.filterByCategory(allShorts, selectedCategory)
            .ifEmpty { allShorts }
    }

    val infiniteFeedEnabled = !useDeterministicOrder && filteredShorts.size > 1
    val virtualPageCount = if (infiniteFeedEnabled) Int.MAX_VALUE else filteredShorts.size

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { virtualPageCount }
    )
    val activeShort = if (filteredShorts.isNotEmpty()) {
        filteredShorts[pagerState.currentPage % filteredShorts.size]
    } else {
        null
    }

    LaunchedEffect(selectedCategory) {
        markUserInteraction()
        pendingAutoAdvance = false
        pagerState.scrollToPage(0)
        elapsedSeconds = 0
        completedPlaysOnCurrentShort = 0
        lastLoopEventMs = 0L
    }

    // Immediately cache each visible Biblical Word ID in SQLDelight so it won't repeat until all others are seen
    LaunchedEffect(pagerState.currentPage, activeShort?.id) {
        val wasAutoAdvance = pendingAutoAdvance
        pendingAutoAdvance = false
        if (!wasAutoAdvance) {
            markUserInteraction()
        }
        elapsedSeconds = 0
        completedPlaysOnCurrentShort = 0
        lastLoopEventMs = 0L
        isPaused = false
        activeShort?.let { short ->
            AnalyticsTracker.trackShortViewed(
                shortItem = short,
                advanceSource = if (wasAutoAdvance) "auto" else "manual",
            )
            if (!useDeterministicOrder) {
                progressManager.markShortSeen(short.id)
            }
        }
        val shouldHint = progressManager.shouldShowSwipeHint(pagerState.currentPage)
        showSwipeHint = shouldHint
        if (shouldHint) {
            progressManager.markSwipeHintShown()
        }
        if (pagerState.currentPage != lastLegendEvaluatedPage) {
            lastLegendEvaluatedPage = pagerState.currentPage
            val shouldLegend = progressManager.shouldShowGestureLegend()
            showGestureLegend = shouldLegend
            if (shouldLegend) {
                progressManager.markGestureLegendShown()
            }
        }
    }

    LaunchedEffect(soundToastLabel) {
        if (soundToastLabel != null) {
            delay(1100L)
            soundToastLabel = null
        }
    }

    LaunchedEffect(showSwipeHint, enablePlaybackTimer) {
        if (!showSwipeHint || !enablePlaybackTimer) {
            hintBounceOffsetPx = 0
            return@LaunchedEffect
        }
        var phase = 0.0
        while (isActive) {
            withContext(Dispatchers.Default) {
                delay(45L)
            }
            phase = (phase + 0.18) % (2.0 * PI)
            hintBounceOffsetPx = (sin(phase) * 5.0).roundToInt()
        }
    }

    val handlePlaybackLoopCompleted: () -> Unit = {
        val currentItem = activeShort
        val nowMs = currentTimeMillis()
        if (currentItem != null &&
            !isPaused &&
            isAppInForeground &&
            !showTutorialOverlay &&
            (nowMs - lastLoopEventMs) >= 2000L
        ) {
            lastLoopEventMs = nowMs
            completedPlaysOnCurrentShort += 1
            elapsedSeconds = 0
            if (ShortsPlaybackLogic.shouldAutoAdvanceAfterPlay(completedPlaysOnCurrentShort) &&
                filteredShorts.size > 1
            ) {
                consecutiveAutoAdvances += 1
                pendingAutoAdvance = true
                val idleSeconds = ((nowMs - lastUserInteractionMs) / 1000L).coerceAtLeast(0L)
                AnalyticsTracker.track(
                    "short_auto_advanced",
                    "biblical_shorts",
                    mapOf(
                        "fromShortId" to currentItem.id,
                        "plays" to completedPlaysOnCurrentShort.toString(),
                        "consecutiveAutoAdvances" to consecutiveAutoAdvances.toString(),
                        "idleSeconds" to idleSeconds.toString(),
                        "likelyIdle" to (consecutiveAutoAdvances >= 3).toString(),
                    ),
                )
                val nextIndex = if (infiniteFeedEnabled) {
                    pagerState.currentPage + 1
                } else {
                    (pagerState.currentPage + 1) % filteredShorts.size
                }
                scope.launch { pagerState.animateScrollToPage(nextIndex) }
            }
        }
    }

    LaunchedEffect(enablePlaybackTimer, activeShort?.id, isPaused, isAppInForeground, showTutorialOverlay) {
        if (!enablePlaybackTimer || activeShort == null || isPaused || !isAppInForeground || showTutorialOverlay) {
            return@LaunchedEffect
        }
        val maxDuration = activeShort.effectiveInAppDurationSeconds()
        while (isActive) {
            withContext(Dispatchers.Default) {
                delay(1000L)
            }
            if (elapsedSeconds + 1 >= maxDuration) {
                handlePlaybackLoopCompleted()
            } else {
                elapsedSeconds += 1
            }
        }
    }

    LaunchedEffect(BibleState.autoDemoTourEnabled, filteredShorts.size) {
        if (BibleState.autoDemoTourEnabled && filteredShorts.size >= 2) {
            delay(5500L)
            pagerState.animateScrollToPage(1)
            delay(5500L)
            BibleState.autoDemoTourEnabled = false
            onReadChapter(filteredShorts[1])
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09111E))
            .pointerInput(filteredShorts.size, infiniteFeedEnabled) {
                var totalHorizontalDrag = 0f
                var handled = false
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalHorizontalDrag = 0f
                        handled = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (handled) return@detectHorizontalDragGestures
                        totalHorizontalDrag += dragAmount
                        val threshold = 72.dp.toPx()
                        if (totalHorizontalDrag > threshold) {
                            // Swipe left-to-right -> return to Home
                            handled = true
                            markUserInteraction()
                            AnalyticsTracker.track("short_swipe_back_home", "biblical_shorts")
                            onBack()
                        } else if (totalHorizontalDrag < -threshold && filteredShorts.isNotEmpty()) {
                            // Swipe right-to-left -> load next clip
                            handled = true
                            markUserInteraction()
                            pendingAutoAdvance = false
                            AnalyticsTracker.track("short_swipe_left_next", "biblical_shorts")
                            val nextIndex = if (infiniteFeedEnabled) {
                                pagerState.currentPage + 1
                            } else {
                                (pagerState.currentPage + 1) % filteredShorts.size
                            }
                            scope.launch { pagerState.animateScrollToPage(nextIndex) }
                        }
                    },
                )
            }
            .testTag("biblical_shorts_screen"),
    ) {
        val windowMetrics = computeAdaptiveWindowMetrics(maxWidth, maxHeight)

        if (filteredShorts.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("biblical_shorts_pager"),
            ) { page ->
                val shortItem = filteredShorts[page % filteredShorts.size]
                val isCurrentPage = page == pagerState.currentPage
                val maxDuration = shortItem.effectiveInAppDurationSeconds()
                val pageElapsed = if (isCurrentPage) elapsedSeconds.coerceIn(0, maxDuration) else 0

                val tapGestureModifier = Modifier.pointerInput(isCurrentPage) {
                    detectTapGestures(
                        onTap = {
                            if (isCurrentPage) {
                                markUserInteraction()
                                isPaused = !isPaused
                            }
                        },
                        onDoubleTap = {
                            if (isCurrentPage) {
                                markUserInteraction()
                                isMuted = !isMuted
                                soundToastLabel = if (isMuted) "Sound Off" else "Sound On"
                                AnalyticsTracker.track(
                                    "short_double_tap_sound_toggled",
                                    "biblical_shorts",
                                    mapOf("muted" to isMuted.toString()),
                                )
                            }
                        },
                    )
                }

                val seekHandler: (Int) -> Unit = { targetSec ->
                    if (isCurrentPage) {
                        markUserInteraction()
                        val clamped = targetSec.coerceIn(0, maxDuration)
                        elapsedSeconds = clamped
                        seekTargetSeconds = clamped
                        seekRequestId++
                    }
                }

                val readChapterHandler: () -> Unit = {
                    markUserInteraction()
                    AnalyticsTracker.track(
                        "short_read_chapter_clicked",
                        "biblical_shorts",
                        mapOf(
                            "shortId" to shortItem.id,
                            "title" to shortItem.title,
                            "bookUsfm" to shortItem.bookUsfm,
                            "chapter" to shortItem.chapter.toString(),
                        ),
                    )
                    onReadChapter(shortItem)
                }

                val nextShortHandler: () -> Unit = {
                    markUserInteraction()
                    pendingAutoAdvance = false
                    AnalyticsTracker.track(
                        "short_next_clicked",
                        "biblical_shorts",
                        mapOf("fromShortId" to shortItem.id),
                    )
                    val nextIndex = if (infiniteFeedEnabled) {
                        page + 1
                    } else {
                        (page + 1) % filteredShorts.size
                    }
                    scope.launch { pagerState.animateScrollToPage(nextIndex) }
                }

                val isCompactLandscape = maxHeight < 520.dp
                val isPortraitTablet = maxHeight > maxWidth && !isCompactLandscape
                val isLargeTablet = maxWidth >= 780.dp && maxHeight >= 580.dp

                if (windowMetrics.isDualPaneShorts && isPortraitTablet) {
                    val topBarReserve = if (isLargeTablet) 68.dp else 56.dp
                    val horizontalInset = if (isLargeTablet) 20.dp else 14.dp
                    val bottomInset = if (isLargeTablet) 16.dp else 12.dp

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(
                                start = horizontalInset,
                                end = horizontalInset,
                                top = topBarReserve,
                                bottom = bottomInset,
                            )
                            .testTag("shorts_adaptive_dual_pane"),
                    ) {
                        // Top 75%: tall generous video clip with overlaid controls & seek bar
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(0.75f)
                                .fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                                    .clip(RoundedCornerShape(22.dp))
                                    .border(
                                        1.dp,
                                        Color(0x44FBD38D),
                                        RoundedCornerShape(22.dp),
                                    )
                                    .then(tapGestureModifier)
                                    .testTag("shorts_theater_video_frame"),
                            ) {
                                if (videoPreviewOverride != null) {
                                    videoPreviewOverride(shortItem, Modifier.fillMaxSize())
                                } else {
                                    ShortsVideoPlayer(
                                        shortItem = shortItem,
                                        isActive = isCurrentPage && isAppInForeground,
                                        isMuted = isMuted,
                                        isPaused = isPaused,
                                        elapsedSeconds = pageElapsed,
                                        seekTargetSeconds = seekTargetSeconds,
                                        seekRequestId = seekRequestId,
                                        onLoopCompleted = { if (isCurrentPage) handlePlaybackLoopCompleted() },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                ShortsPauseOverlayBadge(
                                    visible = isCurrentPage && isPaused,
                                    isLargeTablet = isLargeTablet,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                                ShortsSoundToastPill(
                                    visible = isCurrentPage && soundToastLabel != null,
                                    label = soundToastLabel.orEmpty(),
                                    modifier = Modifier.align(Alignment.Center),
                                )
                                ShortsClipBottomOverlay(
                                    isCurrentPage = isCurrentPage,
                                    showReadNowButton = pageElapsed >= shortItem.effectiveScene2StartSeconds(),
                                    onReadNowClick = readChapterHandler,
                                    showSwipeHint = false,
                                    showGestureLegend = showGestureLegend,
                                    hintOffsetPx = 0,
                                    onSwipeHintClick = null,
                                    pageElapsed = pageElapsed,
                                    maxDuration = maxDuration,
                                    onSeekSeconds = seekHandler,
                                    isLargeTablet = isLargeTablet,
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                )
                            }
                        }

                        // Bottom 25%: crisp Scripture verse card + pinned Read CTA + persistent next-clip pill
                        ShortsPortraitTabletBottomCard(
                            shortItem = shortItem,
                            onReadChapter = readChapterHandler,
                            isLargeTablet = isLargeTablet,
                            hintOffsetPx = hintBounceOffsetPx,
                            onNextShort = nextShortHandler,
                            showSwipeHint = isCurrentPage,
                            modifier = Modifier
                                .weight(0.25f)
                                .fillMaxWidth(),
                        )
                    }
                } else if (windowMetrics.isDualPaneShorts) {
                    val topBarReserve = when {
                        isCompactLandscape -> 48.dp
                        isLargeTablet -> 72.dp
                        else -> 60.dp
                    }
                    val horizontalInset = if (isCompactLandscape) 14.dp else 24.dp
                    val bottomInset = if (isCompactLandscape) 10.dp else 18.dp
                    val availableTheaterHeight = (maxHeight - topBarReserve - bottomInset - 8.dp).coerceAtLeast(250.dp)
                    val constrainedPaneWidth = ((minOf(maxWidth, 1240.dp) - (horizontalInset * 2) - windowMetrics.hingeSafeGap) * 0.44f)
                        .coerceAtLeast(160.dp)
                    val normalizedTheaterHeight = minOf(availableTheaterHeight, constrainedPaneWidth * (16f / 9f))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(
                                start = horizontalInset,
                                end = horizontalInset,
                                top = topBarReserve,
                                bottom = bottomInset,
                            ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1240.dp)
                                .testTag("shorts_adaptive_dual_pane"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Left theater pane holds the 9:16 video clip with overlaid gesture legend and seek bar
                            Box(
                                modifier = Modifier
                                    .weight(0.92f)
                                    .height(normalizedTheaterHeight),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(normalizedTheaterHeight)
                                        .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                                        .clip(RoundedCornerShape(if (isCompactLandscape) 16.dp else 22.dp))
                                        .border(
                                            1.dp,
                                            Color(0x44FBD38D),
                                            RoundedCornerShape(if (isCompactLandscape) 16.dp else 22.dp),
                                        )
                                        .then(tapGestureModifier)
                                        .testTag("shorts_theater_video_frame"),
                                ) {
                                    if (videoPreviewOverride != null) {
                                        videoPreviewOverride(shortItem, Modifier.fillMaxSize())
                                    } else {
                                        ShortsVideoPlayer(
                                            shortItem = shortItem,
                                            isActive = isCurrentPage && isAppInForeground,
                                            isMuted = isMuted,
                                            isPaused = isPaused,
                                            elapsedSeconds = pageElapsed,
                                            seekTargetSeconds = seekTargetSeconds,
                                            seekRequestId = seekRequestId,
                                            onLoopCompleted = { if (isCurrentPage) handlePlaybackLoopCompleted() },
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                    ShortsPauseOverlayBadge(
                                        visible = isCurrentPage && isPaused,
                                        isLargeTablet = isLargeTablet,
                                        modifier = Modifier.align(Alignment.Center),
                                    )
                                    ShortsSoundToastPill(
                                        visible = isCurrentPage && soundToastLabel != null,
                                        label = soundToastLabel.orEmpty(),
                                        modifier = Modifier.align(Alignment.Center),
                                    )
                                    ShortsClipBottomOverlay(
                                        isCurrentPage = isCurrentPage,
                                        showReadNowButton = pageElapsed >= shortItem.effectiveScene2StartSeconds(),
                                        onReadNowClick = readChapterHandler,
                                        showSwipeHint = false,
                                        showGestureLegend = showGestureLegend,
                                        hintOffsetPx = 0,
                                        onSwipeHintClick = null,
                                        pageElapsed = pageElapsed,
                                        maxDuration = maxDuration,
                                        onSeekSeconds = seekHandler,
                                        isLargeTablet = isLargeTablet,
                                        isCompactLandscape = isCompactLandscape,
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(windowMetrics.hingeSafeGap))

                            // Right scripture study card fills the entire pane height cleanly
                            ShortsDualPaneMetadataCard(
                                shortItem = shortItem,
                                onReadChapter = readChapterHandler,
                                isLargeTablet = isLargeTablet,
                                isCompactLandscape = isCompactLandscape,
                                showSwipeHint = isCurrentPage && !isCompactLandscape,
                                hintOffsetPx = hintBounceOffsetPx,
                                onNextShort = nextShortHandler,
                                modifier = Modifier
                                    .weight(1.08f)
                                    .height(normalizedTheaterHeight),
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                    ) {
                        // Reserve dedicated space for the top header bar so back + Speaker-X never overlap video captions
                        Spacer(modifier = Modifier.height(52.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .then(tapGestureModifier)
                                .testTag("shorts_phone_video_frame"),
                        ) {
                            if (videoPreviewOverride != null) {
                                videoPreviewOverride(shortItem, Modifier.fillMaxSize())
                            } else {
                                ShortsVideoPlayer(
                                    shortItem = shortItem,
                                    isActive = isCurrentPage && isAppInForeground,
                                    isMuted = isMuted,
                                    isPaused = isPaused,
                                    elapsedSeconds = pageElapsed,
                                    seekTargetSeconds = seekTargetSeconds,
                                    seekRequestId = seekRequestId,
                                    onLoopCompleted = { if (isCurrentPage) handlePlaybackLoopCompleted() },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            ShortsPauseOverlayBadge(
                                visible = isCurrentPage && isPaused,
                                isLargeTablet = false,
                                modifier = Modifier.align(Alignment.Center),
                            )
                            ShortsSoundToastPill(
                                visible = isCurrentPage && soundToastLabel != null,
                                label = soundToastLabel.orEmpty(),
                                modifier = Modifier.align(Alignment.Center),
                            )
                            ShortsClipBottomOverlay(
                                isCurrentPage = isCurrentPage,
                                showReadNowButton = pageElapsed >= shortItem.effectiveScene2StartSeconds(),
                                onReadNowClick = readChapterHandler,
                                showSwipeHint = showSwipeHint,
                                showGestureLegend = showGestureLegend,
                                hintOffsetPx = hintBounceOffsetPx,
                                onSwipeHintClick = nextShortHandler,
                                pageElapsed = pageElapsed,
                                maxDuration = maxDuration,
                                onSeekSeconds = seekHandler,
                                isLargeTablet = false,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                }
            }
        }

        val headerIsLargeTablet = maxWidth >= 780.dp && maxHeight >= 580.dp
        val headerTitleSize = if (headerIsLargeTablet) 22.sp else 17.sp
        val headerSubSize = if (headerIsLargeTablet) 13.5.sp else 11.sp
        val soundBtnSize = if (headerIsLargeTablet) 44.dp else 38.dp
        val soundDesc = if (isMuted) "Sound Off" else "Sound On"

        // Top header uses clean single Speaker-X / Speaker-On icon circle in top-right (Option 1)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Transparent)
                .statusBarsPadding()
                .padding(
                    start = if (headerIsLargeTablet) 14.dp else 8.dp,
                    end = if (headerIsLargeTablet) 18.dp else 12.dp,
                    top = 6.dp,
                    bottom = 6.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = Color.White.copy(alpha = 0.68f),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Biblical Words",
                        color = Color.White.copy(alpha = 0.68f),
                        fontWeight = FontWeight.Bold,
                        fontSize = headerTitleSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Daily Scripture Moments",
                        color = Color(0xFFCBD5E1).copy(alpha = 0.50f),
                        fontSize = headerSubSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                onClick = {
                    markUserInteraction()
                    isMuted = !isMuted
                    soundToastLabel = if (isMuted) "Sound Off" else "Sound On"
                    AnalyticsTracker.track(
                        "short_mute_toggled",
                        "biblical_shorts",
                        mapOf("muted" to isMuted.toString()),
                    )
                },
                shape = CircleShape,
                color = if (isMuted) Color(0x4709111E) else Color(0x2EFBD38D),
                border = BorderStroke(1.dp, if (isMuted) Color(0x33FFFFFF) else Color(0xB3FBD38D)),
                elevation = 0.dp,
                modifier = Modifier
                    .size(soundBtnSize)
                    .semantics { contentDescription = soundDesc }
                    .testTag("shorts_mute_toggle"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ShortsSpeakerIcon(
                        isMuted = isMuted,
                        tint = if (isMuted) Color.White.copy(alpha = 0.72f) else Color(0xFFFBD38D).copy(alpha = 0.88f),
                        modifier = Modifier.size(if (headerIsLargeTablet) 21.dp else 18.dp),
                    )
                }
            }
        }

        // Brief first-time gesture tutorial overlay (Double-tap sound, Tap pause, Swipe up next)
        AnimatedVisibility(
            visible = showTutorialOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB8060C16))
                    .clickable {
                        markUserInteraction()
                        showTutorialOverlay = false
                        progressManager.markTutorialDismissed()
                    }
                    .padding(horizontal = 28.dp)
                    .testTag("shorts_tutorial_overlay"),
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF15243A),
                    border = BorderStroke(1.dp, Color(0x66FBD38D)),
                    elevation = 10.dp,
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .fillMaxWidth(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    ) {
                        Text(
                            text = "Welcome to Biblical Words",
                            color = Color(0xFFFBD38D),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        TutorialTipRow(
                            badge = "2×",
                            title = "Double-tap center",
                            subtitle = "Turn sound on or off",
                        )
                        TutorialTipRow(
                            badge = "1×",
                            title = "Tap once",
                            subtitle = "Pause or resume playback",
                        )
                        TutorialTipRow(
                            badge = "↑",
                            title = "Swipe up",
                            subtitle = "View the next Biblical Word",
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                markUserInteraction()
                                showTutorialOverlay = false
                                progressManager.markTutorialDismissed()
                                AnalyticsTracker.track("shorts_tutorial_dismissed", "biblical_shorts")
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFFBD38D),
                                contentColor = Color(0xFF0F172A),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shorts_tutorial_dismiss_btn"),
                        ) {
                            Text(
                                text = "Got it",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortsSpeakerIcon(
    isMuted: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val speakerPath = Path().apply {
            moveTo(w * 0.14f, h * 0.37f)
            lineTo(w * 0.31f, h * 0.37f)
            lineTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.50f, h * 0.80f)
            lineTo(w * 0.31f, h * 0.63f)
            lineTo(w * 0.14f, h * 0.63f)
            close()
        }
        drawPath(
            path = speakerPath,
            color = tint,
            style = Stroke(width = w * 0.09f, join = StrokeJoin.Round, cap = StrokeCap.Round),
        )
        if (isMuted) {
            val strokeWidth = w * 0.10f
            drawLine(
                color = tint,
                start = Offset(w * 0.66f, h * 0.34f),
                end = Offset(w * 0.90f, h * 0.66f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.90f, h * 0.34f),
                end = Offset(w * 0.66f, h * 0.66f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        } else {
            val waveStroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round)
            drawArc(
                color = tint,
                startAngle = -52f,
                sweepAngle = 104f,
                useCenter = false,
                topLeft = Offset(w * 0.54f, h * 0.32f),
                size = androidx.compose.ui.geometry.Size(w * 0.20f, h * 0.36f),
                style = waveStroke,
            )
            drawArc(
                color = tint,
                startAngle = -56f,
                sweepAngle = 112f,
                useCenter = false,
                topLeft = Offset(w * 0.60f, h * 0.18f),
                size = androidx.compose.ui.geometry.Size(w * 0.30f, h * 0.64f),
                style = waveStroke,
            )
        }
    }
}

@Composable
private fun ShortsPauseOverlayBadge(
    visible: Boolean,
    isLargeTablet: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(140)),
        exit = fadeOut(tween(140)),
        modifier = modifier,
    ) {
        val badgeSize = if (isLargeTablet) 76.dp else 64.dp
        Surface(
            shape = CircleShape,
            color = Color(0xCC09111E),
            border = BorderStroke(1.dp, Color(0x88FBD38D)),
            elevation = 6.dp,
            modifier = Modifier
                .size(badgeSize)
                .testTag("shorts_pause_overlay_badge"),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Paused, tap to resume",
                    tint = Color(0xFFFBD38D),
                    modifier = Modifier.size(if (isLargeTablet) 32.dp else 28.dp),
                )
                Text(
                    text = "PAUSED",
                    color = Color(0xFFFBD38D),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp,
                )
            }
        }
    }
}

@Composable
private fun ShortsGestureLegendRow(
    isLargeTablet: Boolean = false,
    isCompactLandscape: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val dotSize = if (isLargeTablet) 6.dp else 5.dp
    val fontSize = when {
        isLargeTablet -> 12.sp
        isCompactLandscape -> 10.sp
        else -> 11.sp
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.testTag("shorts_gesture_legend"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(Color(0xFFFBD38D), CircleShape),
            )
            Text(
                text = "1x Pause",
                color = Color(0xFFE2E8F0),
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            text = "·",
            color = Color(0xFF94A3B8),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .background(Color(0xFFFBD38D), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .background(Color(0xFFFBD38D), CircleShape),
                )
            }
            Text(
                text = "2x Sound",
                color = Color(0xFFE2E8F0),
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ShortsClipBottomOverlay(
    isCurrentPage: Boolean,
    showReadNowButton: Boolean,
    onReadNowClick: () -> Unit,
    showSwipeHint: Boolean,
    showGestureLegend: Boolean,
    hintOffsetPx: Int,
    onSwipeHintClick: (() -> Unit)?,
    pageElapsed: Int,
    maxDuration: Int,
    onSeekSeconds: (Int) -> Unit,
    isLargeTablet: Boolean,
    isCompactLandscape: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bottomBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0x9909111E),
            Color(0xDE09111E),
        )
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isCompactLandscape) 3.dp else 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(bottomBrush)
            .padding(
                start = if (isCompactLandscape) 10.dp else 14.dp,
                end = if (isCompactLandscape) 10.dp else 14.dp,
                top = if (isCompactLandscape) 14.dp else 24.dp,
                bottom = if (isCompactLandscape) 4.dp else 8.dp,
            ),
    ) {
        ShortsCard2ReadNowButton(
            visible = isCurrentPage && showReadNowButton,
            onClick = onReadNowClick,
        )
        ShortsSwipeHintBadge(
            visible = isCurrentPage && showSwipeHint,
            hintOffsetPx = hintOffsetPx,
            isLargeTablet = isLargeTablet,
            isCompactLandscape = isCompactLandscape,
            onClick = onSwipeHintClick,
        )
        AnimatedVisibility(
            visible = isCurrentPage && showGestureLegend,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ShortsGestureLegendRow(
                isLargeTablet = isLargeTablet,
                isCompactLandscape = isCompactLandscape,
            )
        }
        ShortsScrubberRow(
            pageElapsed = pageElapsed,
            maxDuration = maxDuration,
            onSeekSeconds = onSeekSeconds,
            isLargeTablet = isLargeTablet,
        )
    }
}

@Composable
private fun ShortsSoundToastPill(
    visible: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xCC09111E),
            border = BorderStroke(1.dp, Color(0x88FBD38D)),
            elevation = 0.dp,
        ) {
            Text(
                text = label,
                color = Color(0xFFFBD38D),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ShortsSwipeHintBadge(
    visible: Boolean,
    hintOffsetPx: Int,
    modifier: Modifier = Modifier,
    isLargeTablet: Boolean = false,
    isCompactLandscape: Boolean = false,
    label: String = "Swipe for next clip",
    onClick: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val textSize = when {
            isLargeTablet -> 14.sp
            isCompactLandscape -> 11.5.sp
            else -> 11.sp
        }
        val hPad = if (isLargeTablet) 18.dp else 12.dp
        val vPad = if (isLargeTablet) 9.dp else 5.dp
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xB30F172A),
            border = BorderStroke(1.dp, Color(0x66FBD38D)),
            elevation = 0.dp,
            modifier = Modifier
                .offset { IntOffset(0, hintOffsetPx) }
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .testTag("shorts_swipe_hint"),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = hPad, vertical = vPad),
            ) {
                Text(
                    text = label,
                    color = Color(0xFFFBD38D),
                    fontSize = textSize,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFFFBD38D),
                    modifier = Modifier.size(if (isLargeTablet) 18.dp else 15.dp),
                )
            }
        }
    }
}

@Composable
private fun ShortsScrubberRow(
    pageElapsed: Int,
    maxDuration: Int,
    onSeekSeconds: (Int) -> Unit,
    isLargeTablet: Boolean = false,
) {
    val timeSize = if (isLargeTablet) 13.5.sp else 11.sp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = formatShortTime(pageElapsed),
            color = Color(0xFFCBD5E1),
            fontSize = timeSize,
            fontWeight = FontWeight.Medium,
        )
        Slider(
            value = pageElapsed.toFloat(),
            onValueChange = { newSec -> onSeekSeconds(newSec.roundToInt()) },
            valueRange = 0f..maxDuration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFBD38D),
                activeTrackColor = Color(0xFFFBD38D),
                inactiveTrackColor = Color(0x44FFFFFF),
            ),
            modifier = Modifier
                .weight(1f)
                .height(if (isLargeTablet) 32.dp else 26.dp)
                .testTag("shorts_progress_scrubber"),
        )
        Text(
            text = formatShortTime(maxDuration),
            color = Color(0xFF94A3B8),
            fontSize = timeSize,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ShortsCard2ReadNowButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val transition = rememberInfiniteTransition()
        val firstShift by transition.animateFloat(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 720, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        )
        val firstAlpha by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 720, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        )
        val secondAlpha by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 720, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        )

        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color(0xFFFBD38D),
            border = BorderStroke(1.dp, Color(0xFFFFE8B3)),
            elevation = 6.dp,
            modifier = Modifier.testTag("short_card2_read_now_button"),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Read Now",
                    color = Color(0xFF09111E),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF09111E),
                    modifier = Modifier
                        .size(20.dp)
                        .offset(x = firstShift.dp)
                        .alpha(firstAlpha),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF09111E),
                    modifier = Modifier
                        .size(20.dp)
                        .offset(x = (firstShift * 0.8f - 4f).dp)
                        .alpha(secondAlpha),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ShortsPortraitTabletBottomCard(
    shortItem: BiblicalShortDto,
    onReadChapter: () -> Unit,
    isLargeTablet: Boolean,
    hintOffsetPx: Int,
    onNextShort: () -> Unit,
    showSwipeHint: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF131E30),
        border = BorderStroke(1.dp, Color(0x33FBD38D)),
        elevation = 0.dp,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isLargeTablet) 20.dp else 14.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x2AFBD38D),
                        border = BorderStroke(1.dp, Color(0x55FBD38D)),
                        elevation = 0.dp,
                    ) {
                        Text(
                            text = shortItem.scriptureReference,
                            color = Color(0xFFFBD38D),
                            fontSize = if (isLargeTablet) 13.5.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                    Text(
                        text = shortItem.title,
                        fontFamily = MaterialTheme.typography.h1.fontFamily,
                        color = Color.White,
                        fontSize = if (isLargeTablet) 22.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(42.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFFBD38D)),
                    )
                    Text(
                        text = "“${shortItem.keyVerseText}”",
                        color = Color(0xFFF1F5F9),
                        fontSize = if (isLargeTablet) 15.sp else 13.5.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = if (isLargeTablet) 21.sp else 19.sp,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (shortItem.lesson.isNotBlank()) {
                    Text(
                        text = "Practice: ${shortItem.lesson}",
                        color = Color(0xFF9AE6B4),
                        fontSize = if (isLargeTablet) 13.sp else 12.sp,
                        lineHeight = if (isLargeTablet) 18.sp else 16.sp,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                modifier = Modifier.widthIn(min = 160.dp, max = 216.dp),
            ) {
                Surface(
                    onClick = onReadChapter,
                    shape = CircleShape,
                    color = Color(0xFFFBD38D),
                    border = BorderStroke(1.dp, Color(0xFFFFE8B3)),
                    elevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dual_pane_read_chapter_btn"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    ) {
                        Text(
                            text = "Read ${shortItem.scriptureReference}",
                            color = Color(0xFF09111E),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isLargeTablet) 14.5.sp else 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF09111E),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                ShortsSwipeHintBadge(
                    visible = showSwipeHint,
                    hintOffsetPx = hintOffsetPx,
                    isLargeTablet = isLargeTablet,
                    isCompactLandscape = false,
                    label = "Swipe or tap for next clip",
                    onClick = onNextShort,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ShortsDualPaneMetadataCard(
    shortItem: BiblicalShortDto,
    onReadChapter: () -> Unit,
    isLargeTablet: Boolean,
    isCompactLandscape: Boolean,
    showSwipeHint: Boolean,
    hintOffsetPx: Int,
    onNextShort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardPad = when {
        isCompactLandscape -> 14.dp
        isLargeTablet -> 28.dp
        else -> 20.dp
    }
    val refFontSize = when {
        isCompactLandscape -> 12.sp
        isLargeTablet -> 15.sp
        else -> 13.5.sp
    }
    val titleFontSize = when {
        isCompactLandscape -> 20.sp
        isLargeTablet -> 32.sp
        else -> 24.sp
    }
    val titleLineHeight = when {
        isCompactLandscape -> 25.sp
        isLargeTablet -> 38.sp
        else -> 30.sp
    }
    val verseFontSize = when {
        isCompactLandscape -> 14.5.sp
        isLargeTablet -> 22.sp
        else -> 17.5.sp
    }
    val verseLineHeight = when {
        isCompactLandscape -> 21.sp
        isLargeTablet -> 33.sp
        else -> 26.sp
    }
    val lessonFontSize = when {
        isCompactLandscape -> 13.sp
        isLargeTablet -> 17.5.sp
        else -> 15.sp
    }
    val lessonLineHeight = when {
        isCompactLandscape -> 18.sp
        isLargeTablet -> 25.sp
        else -> 22.sp
    }

    Surface(
        shape = RoundedCornerShape(if (isCompactLandscape) 16.dp else 22.dp),
        color = Color(0xFF131E30),
        border = BorderStroke(1.dp, Color(0x33FBD38D)),
        elevation = 0.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(cardPad),
            verticalArrangement = Arrangement.spacedBy(if (isCompactLandscape) 8.dp else 14.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (isCompactLandscape) 8.dp else 16.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0x2AFBD38D),
                    border = BorderStroke(1.dp, Color(0x55FBD38D)),
                    elevation = 0.dp,
                ) {
                    Text(
                        text = shortItem.scriptureReference,
                        color = Color(0xFFFBD38D),
                        fontSize = refFontSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            horizontal = if (isLargeTablet) 14.dp else 10.dp,
                            vertical = if (isLargeTablet) 6.dp else 4.dp,
                        ),
                    )
                }

                Text(
                    text = shortItem.title,
                    fontFamily = MaterialTheme.typography.h1.fontFamily,
                    color = Color.White,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    lineHeight = titleLineHeight,
                )

                Divider(
                    color = Color(0x28FBD38D),
                    thickness = 1.dp,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(if (isCompactLandscape) 44.dp else 96.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFBD38D)),
                    )
                    Text(
                        text = "“${shortItem.keyVerseText}”",
                        color = Color(0xFFF1F5F9),
                        fontSize = verseFontSize,
                        fontStyle = FontStyle.Italic,
                        lineHeight = verseLineHeight,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (shortItem.lesson.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF172D42),
                        border = BorderStroke(1.dp, Color(0x4438B2AC)),
                        elevation = 0.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(if (isCompactLandscape) 3.dp else 6.dp),
                            modifier = Modifier.padding(if (isCompactLandscape) 10.dp else 16.dp),
                        ) {
                            Text(
                                text = "PRACTICE TODAY",
                                color = Color(0xFF68D391),
                                fontSize = if (isLargeTablet) 12.sp else 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                            )
                            Text(
                                text = shortItem.lesson,
                                color = Color(0xFFE2E8F0),
                                fontSize = lessonFontSize,
                                lineHeight = lessonLineHeight,
                            )
                        }
                    }
                }
            }

            Surface(
                onClick = onReadChapter,
                shape = CircleShape,
                color = Color(0xFFFBD38D),
                border = BorderStroke(1.dp, Color(0xFFFFE8B3)),
                elevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dual_pane_read_chapter_btn"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = if (isCompactLandscape) 8.dp else 14.dp,
                    ),
                ) {
                    Text(
                        text = "Read ${shortItem.scriptureReference}",
                        color = Color(0xFF09111E),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isLargeTablet) 17.sp else 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF09111E),
                        modifier = Modifier.size(if (isLargeTablet) 22.dp else 18.dp),
                    )
                }
            }

            if (showSwipeHint) {
                ShortsSwipeHintBadge(
                    visible = true,
                    hintOffsetPx = hintOffsetPx,
                    isLargeTablet = isLargeTablet,
                    isCompactLandscape = isCompactLandscape,
                    label = "Swipe or tap for next clip",
                    onClick = onNextShort,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun TutorialTipRow(
    badge: String,
    title: String,
    subtitle: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(Color(0x2AFBD38D), CircleShape),
        ) {
            Text(
                text = badge,
                color = Color(0xFFFBD38D),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = Color(0xFFCBD5E1),
                fontSize = 14.5.sp,
            )
        }
    }
}

private fun formatShortTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    val secStr = if (secs < 10) "0$secs" else "$secs"
    return "$mins:$secStr"
}
