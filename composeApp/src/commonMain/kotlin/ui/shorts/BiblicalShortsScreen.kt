package ui.shorts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.analytics.AnalyticsTracker
import data.bibleIQ.BibleIQDataModel
import data.shorts.BiblicalShortDto
import data.shorts.ShortsPlaybackLogic
import data.shorts.ShortsProgressManager
import data.shorts.biblicalShortsCategories
import data.shorts.biblicalShortsRepository
import data.shorts.defaultBiblicalShortsCatalog
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
    selectedCategory: String = BibleIQDataModel.selectedShortsCategory,
    onCategorySelected: (String) -> Unit = { BibleIQDataModel.selectedShortsCategory = it },
    enablePlaybackTimer: Boolean = true,
    resumeShortId: String? = null,
    progressManager: ShortsProgressManager = remember { ShortsProgressManager() },
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
    var seekTargetSeconds by remember { mutableStateOf(0) }
    var seekRequestId by remember { mutableStateOf(0) }
    var showSwipeHint by remember { mutableStateOf(false) }
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
        pagerState.scrollToPage(0)
        elapsedSeconds = 0
    }

    // Immediately cache each visible Biblical Word ID in SQLDelight so it won't repeat until all others are seen
    LaunchedEffect(pagerState.currentPage, activeShort?.id) {
        elapsedSeconds = 0
        isPaused = false
        activeShort?.let { short ->
            AnalyticsTracker.trackShortViewed(short)
            if (!useDeterministicOrder) {
                progressManager.markShortSeen(short.id)
            }
        }
        val shouldHint = progressManager.shouldShowSwipeHint(pagerState.currentPage)
        showSwipeHint = shouldHint
        if (shouldHint) {
            progressManager.markSwipeHintShown()
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

    LaunchedEffect(enablePlaybackTimer, activeShort?.id, isPaused, isAppInForeground) {
        if (!enablePlaybackTimer || activeShort == null || isPaused || !isAppInForeground) return@LaunchedEffect
        val maxDuration = activeShort.durationSeconds.coerceAtLeast(1)
        while (isActive) {
            withContext(Dispatchers.Default) {
                delay(1000L)
            }
            elapsedSeconds = (elapsedSeconds + 1) % maxDuration
        }
    }

    LaunchedEffect(BibleIQDataModel.autoDemoTourEnabled, filteredShorts.size) {
        if (BibleIQDataModel.autoDemoTourEnabled && filteredShorts.size >= 2) {
            delay(5500L)
            pagerState.animateScrollToPage(1)
            delay(5500L)
            BibleIQDataModel.autoDemoTourEnabled = false
            onReadChapter(filteredShorts[1])
        }
    }

    Box(
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
                            AnalyticsTracker.track("short_swipe_back_home", "biblical_shorts")
                            onBack()
                        } else if (totalHorizontalDrag < -threshold && filteredShorts.isNotEmpty()) {
                            // Swipe right-to-left -> load next clip
                            handled = true
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
        if (filteredShorts.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("biblical_shorts_pager"),
            ) { page ->
                val shortItem = filteredShorts[page % filteredShorts.size]
                val isCurrentPage = page == pagerState.currentPage
                val maxDuration = shortItem.durationSeconds.coerceAtLeast(1)
                val pageElapsed = if (isCurrentPage) elapsedSeconds.coerceIn(0, maxDuration) else 0

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isCurrentPage) {
                            detectTapGestures(
                                onTap = {
                                    if (isCurrentPage) {
                                        isPaused = !isPaused
                                    }
                                },
                                onDoubleTap = {
                                    if (isCurrentPage) {
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
                        },
                ) {
                    ShortsVideoPlayer(
                        shortItem = shortItem,
                        isActive = isCurrentPage && isAppInForeground,
                        isMuted = isMuted,
                        isPaused = isPaused,
                        elapsedSeconds = pageElapsed,
                        seekTargetSeconds = seekTargetSeconds,
                        seekRequestId = seekRequestId,
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Center double-tap sound feedback pill
                    AnimatedVisibility(
                        visible = isCurrentPage && soundToastLabel != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xCC09111E),
                            border = BorderStroke(1.dp, Color(0x88FBD38D)),
                            elevation = 0.dp,
                        ) {
                            Text(
                                text = soundToastLabel.orEmpty(),
                                color = Color(0xFFFBD38D),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            )
                        }
                    }

                    // Bottom overlay with solid lower scrim (masks any baked-in video footer cleanly)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Transparent,
                                        0.28f to Color(0xEE09111E),
                                        0.52f to Color(0xFF09111E),
                                        1.0f to Color(0xFF09111E),
                                    )
                                )
                            )
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 14.dp),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            AnimatedVisibility(
                                visible = isCurrentPage && showSwipeHint,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xB30F172A),
                                    border = BorderStroke(1.dp, Color(0x66FBD38D)),
                                    elevation = 0.dp,
                                    modifier = Modifier
                                        .offset { IntOffset(0, hintBounceOffsetPx) }
                                        .testTag("shorts_swipe_hint"),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    ) {
                                        Text(
                                            text = "Swipe for next clip",
                                            color = Color(0xFFFBD38D),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color(0xFFFBD38D),
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                }
                            }

                            // Interactive scrubber progress bar so users can rewind/seek after enabling sound
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = formatShortTime(pageElapsed),
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Slider(
                                    value = pageElapsed.toFloat(),
                                    onValueChange = { newSec ->
                                        if (isCurrentPage) {
                                            val clamped = newSec.roundToInt().coerceIn(0, maxDuration)
                                            elapsedSeconds = clamped
                                            seekTargetSeconds = clamped
                                            seekRequestId++
                                        }
                                    },
                                    valueRange = 0f..maxDuration.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFBD38D),
                                        activeTrackColor = Color(0xFFFBD38D),
                                        inactiveTrackColor = Color(0x44FFFFFF),
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(26.dp)
                                        .testTag("shorts_progress_scrubber"),
                                )
                                Text(
                                    text = formatShortTime(maxDuration),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                )
                            }

                            // Category filter chips at bottom with smooth left/right edge fade
                            Box(modifier = Modifier.fillMaxWidth()) {
                                LazyRow(
                                    contentPadding = PaddingValues(start = 6.dp, end = 34.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("shorts_category_row"),
                                ) {
                                    items(biblicalShortsCategories) { category ->
                                        val isSelected = selectedCategory.equals(category, ignoreCase = true)
                                        Surface(
                                            onClick = {
                                                AnalyticsTracker.track(
                                                    "short_category_filtered",
                                                    "biblical_shorts",
                                                    mapOf("category" to category),
                                                )
                                                onCategorySelected(category)
                                            },
                                            shape = RoundedCornerShape(50),
                                            color = if (isSelected) Color(0xFFFBD38D) else Color(0xFF142033),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) Color(0xFFFBD38D) else Color(0x44FFFFFF)
                                            ),
                                            elevation = 0.dp,
                                        ) {
                                            Text(
                                                text = category,
                                                color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                            )
                                        }
                                    }
                                }
                                // Soft right-edge fade mask so overflow pills dissolve gently instead of clipping sharply
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .width(34.dp)
                                        .height(30.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color.Transparent, Color(0xFF09111E))
                                            )
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(10.dp)
                                        .height(30.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color(0xFF09111E), Color.Transparent)
                                            )
                                        )
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = {
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
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFFFBD38D),
                                        contentColor = Color(0xFF0F172A),
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("read_chapter_btn_${shortItem.id}"),
                                ) {
                                    Text(
                                        text = "Read Chapter",
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
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
                                    },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, Color(0x66FFFFFF)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        backgroundColor = Color(0xFF142033),
                                        contentColor = Color.White,
                                    ),
                                    modifier = Modifier.testTag("next_short_btn_${shortItem.id}"),
                                ) {
                                    Text(
                                        text = "Next",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transparent top header so video titles ("Job: Anchored Beyond Circumstances") stay bright & unobstructed
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Transparent)
                .statusBarsPadding()
                .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = Color.White,
                    )
                }
                Column {
                    Text(
                        text = "Biblical Words",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                    Text(
                        text = activeShort?.category ?: "Daily Scripture Moments",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    onClick = {
                        isMuted = !isMuted
                        AnalyticsTracker.track(
                            "short_mute_toggled",
                            "biblical_shorts",
                            mapOf("muted" to isMuted.toString()),
                        )
                    },
                    shape = CircleShape,
                    color = Color(0x66000000),
                    border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                    elevation = 0.dp,
                    modifier = Modifier.testTag("shorts_mute_toggle"),
                ) {
                    Text(
                        text = if (isMuted) "Sound Off" else "Sound On",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }

                Surface(
                    onClick = {
                        isPaused = !isPaused
                        AnalyticsTracker.track(
                            "short_pause_toggled",
                            "biblical_shorts",
                            mapOf("paused" to isPaused.toString()),
                        )
                    },
                    shape = CircleShape,
                    color = Color(0x66000000),
                    border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                    elevation = 0.dp,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = if (isPaused) "Play" else "Pause",
                            tint = Color(0xFFFBD38D),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = if (isPaused) "Play" else "Pause",
                            color = Color.White,
                            fontSize = 11.sp,
                        )
                    }
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
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                    ) {
                        Text(
                            text = "Welcome to Biblical Words",
                            color = Color(0xFFFBD38D),
                            fontSize = 18.sp,
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
                        Spacer(modifier = Modifier.height(2.dp))
                        Button(
                            onClick = {
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
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
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
                .size(38.dp)
                .background(Color(0x2AFBD38D), CircleShape),
        ) {
            Text(
                text = badge,
                color = Color(0xFFFBD38D),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
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
