package ui.shorts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.bibleIQ.BibleIQDataModel
import data.shorts.BiblicalShortDto
import data.shorts.biblicalShortsCategories
import data.shorts.biblicalShortsRepository
import data.shorts.defaultBiblicalShortsCatalog
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
) {
    val scope = rememberCoroutineScope()
    var allShorts by remember(initialShorts) {
        mutableStateOf(
            if (initialShorts.isNotEmpty()) initialShorts else defaultBiblicalShortsCatalog()
        )
    }
    var isMuted by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(initialShorts) {
        if (initialShorts.isEmpty()) {
            allShorts = biblicalShortsRepository.getShorts()
        }
    }

    val filteredShorts = remember(allShorts, selectedCategory) {
        biblicalShortsRepository.filterByCategory(allShorts, selectedCategory)
            .ifEmpty { allShorts }
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { filteredShorts.size })
    val activeShort = filteredShorts.getOrNull(pagerState.currentPage)

    LaunchedEffect(selectedCategory) {
        pagerState.scrollToPage(0)
        elapsedSeconds = 0
    }

    LaunchedEffect(pagerState.currentPage) {
        elapsedSeconds = 0
        isPaused = false
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

                    // Bottom gradient + narrative / lesson overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xCC09111E),
                                        Color(0xF509111E),
                                    )
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            LinearProgressIndicator(
                                progress = progressFraction,
                                color = Color(0xFFFBD38D),
                                backgroundColor = Color(0x33FFFFFF),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(50)),
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0x33FBD38D),
                                ) {
                                    Text(
                                        text = "#${shortItem.order} · ${shortItem.category}",
                                        color = Color(0xFFFBD38D),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0x28FFFFFF),
                                ) {
                                    Text(
                                        text = shortItem.scriptureReference,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }

                            Text(
                                text = shortItem.title,
                                style = MaterialTheme.typography.h3.copy(
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Text(
                                text = shortItem.narrative,
                                style = MaterialTheme.typography.body1.copy(
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x243A5F8F),
                                border = BorderStroke(1.dp, Color(0x4490CDF4)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Key Lesson: ${shortItem.lesson}",
                                    color = Color(0xFFEBF8FF),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = { onReadChapter(shortItem) },
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
                                        val nextIndex = (page + 1) % filteredShorts.size
                                        scope.launch { pagerState.animateScrollToPage(nextIndex) }
                                    },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, Color(0x66FFFFFF)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        backgroundColor = Color(0x22000000),
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

        // Top overlay header + category chips
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xE609111E), Color.Transparent)
                    )
                )
                .padding(top = 10.dp, bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Surface(
                        onClick = { isMuted = !isMuted },
                        shape = CircleShape,
                        color = Color(0x55000000),
                        border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                    ) {
                        Text(
                            text = if (isMuted) "Muted" else "Sound On",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }

                    Surface(
                        onClick = { isPaused = !isPaused },
                        shape = CircleShape,
                        color = Color(0x55000000),
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

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(biblicalShortsCategories) { category ->
                    val isSelected = selectedCategory.equals(category, ignoreCase = true)
                    Surface(
                        onClick = { onCategorySelected(category) },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) Color(0xFFFBD38D) else Color(0x660F172A),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFFBD38D) else Color(0x44FFFFFF)
                        ),
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color(0xFF0F172A) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
