package ui.shorts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    progressManager: ShortsProgressManager = remember { ShortsProgressManager() },
) {
    val scope = rememberCoroutineScope()
    val useDeterministicOrder = initialShorts.isNotEmpty()
    var allShorts by remember(initialShorts) {
        mutableStateOf(
            if (useDeterministicOrder) {
                initialShorts
            } else {
                progressManager.getOrCreateOrderedShorts(defaultBiblicalShortsCatalog())
            }
        )
    }
    // Default sound OFF so shorts start quietly until the user opts in
    var isMuted by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var hasRestoredPosition by remember { mutableStateOf(false) }
    var showSwipeHint by remember { mutableStateOf(false) }
    var hintBounceOffsetPx by remember { mutableStateOf(0) }

    LaunchedEffect(initialShorts) {
        if (initialShorts.isEmpty()) {
            val fetched = biblicalShortsRepository.getShorts()
            allShorts = progressManager.getOrCreateOrderedShorts(fetched)
        }
    }

    val filteredShorts = remember(allShorts, selectedCategory) {
        biblicalShortsRepository.filterByCategory(allShorts, selectedCategory)
            .ifEmpty { allShorts }
    }

    val initialResumePage = remember(filteredShorts, useDeterministicOrder) {
        if (useDeterministicOrder) {
            0
        } else {
            ShortsPlaybackLogic.resolveResumeIndex(filteredShorts, progressManager.getLastShortId())
        }
    }

    val pagerState = rememberPagerState(
        initialPage = initialResumePage.coerceIn(0, (filteredShorts.size - 1).coerceAtLeast(0)),
        pageCount = { filteredShorts.size }
    )
    val activeShort = filteredShorts.getOrNull(pagerState.currentPage)

    // Restore saved position once when switching back into "All" on first load
    LaunchedEffect(filteredShorts.size) {
        if (!hasRestoredPosition && !useDeterministicOrder && filteredShorts.isNotEmpty()) {
            val target = ShortsPlaybackLogic.resolveResumeIndex(filteredShorts, progressManager.getLastShortId())
            if (target in filteredShorts.indices && target != pagerState.currentPage) {
                pagerState.scrollToPage(target)
            }
            hasRestoredPosition = true
        }
    }

    LaunchedEffect(selectedCategory) {
        if (hasRestoredPosition || useDeterministicOrder) {
            pagerState.scrollToPage(0)
            elapsedSeconds = 0
        }
    }

    LaunchedEffect(pagerState.currentPage, activeShort?.id) {
        elapsedSeconds = 0
        isPaused = false
        activeShort?.let { short ->
            AnalyticsTracker.trackShortViewed(short)
            if (!useDeterministicOrder) {
                progressManager.saveLastShortId(short.id)
            }
        }
        val shouldHint = progressManager.shouldShowSwipeHint(pagerState.currentPage)
        showSwipeHint = shouldHint
        if (shouldHint) {
            progressManager.markSwipeHintShown()
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

    LaunchedEffect(enablePlaybackTimer, activeShort?.id, isPaused) {
        if (!enablePlaybackTimer || activeShort == null || isPaused) return@LaunchedEffect
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
            .testTag("biblical_shorts_screen"),
    ) {
        if (filteredShorts.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("biblical_shorts_pager"),
            ) { page ->
                val shortItem = filteredShorts[page]
                val isCurrentPage = page == pagerState.currentPage
                val pageElapsed = if (isCurrentPage) elapsedSeconds else 0
                val progressFraction = ((pageElapsed + 1).toFloat() / shortItem.durationSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (isCurrentPage) {
                                isPaused = !isPaused
                            }
                        },
                ) {
                    ShortsVideoPlayer(
                        shortItem = shortItem,
                        isActive = isCurrentPage,
                        isMuted = isMuted,
                        isPaused = isPaused,
                        elapsedSeconds = pageElapsed,
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Compact bottom overlay: optional weekly swipe hint + progress bar + categories + actions
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xB809111E),
                                        Color(0xF209111E),
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                    color = Color(0x990F172A),
                                    border = BorderStroke(1.dp, Color(0x66FBD38D)),
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
                                            text = "Swipe for next short",
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

                            LinearProgressIndicator(
                                progress = progressFraction,
                                color = Color(0xFFFBD38D),
                                backgroundColor = Color(0x33FFFFFF),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(50)),
                            )

                            // Categories moved to bottom bar so top video title & scripture stay unobstructed
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 2.dp),
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
                                        color = if (isSelected) Color(0xFFFBD38D) else Color(0x770F172A),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFFFBD38D) else Color(0x44FFFFFF)
                                        ),
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                        val nextIndex = (page + 1) % filteredShorts.size
                                        scope.launch { pagerState.animateScrollToPage(nextIndex) }
                                    },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, Color(0x66FFFFFF)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        backgroundColor = Color(0x33000000),
                                        contentColor = Color.White,
                                    ),
                                    modifier = Modifier.testTag("next_short_btn_${shortItem.id}"),
                                ) {
                                    Text(
                                        text = "Next Short",
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

        // Clean top overlay header only (category chips moved to bottom)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xCC09111E), Color.Transparent)
                    )
                )
                .padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = Color.White,
                    )
                }
                Column {
                    Text(
                        text = "Biblical Shorts",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                    Text(
                        text = "${(pagerState.currentPage + 1).coerceAtMost(filteredShorts.size)} / ${filteredShorts.size} · Under 30s",
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
    }
}
