package ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import data.GeminiModel
import data.analytics.AnalyticsTracker
import data.appPrefs.ReaderTutorialPrefs
import data.appPrefs.ReadingSession
import data.appPrefs.canonicalBookKey
import data.bible.BibleChapterUIState
import data.bible.BibleState
import data.bible.BookData
import data.bible.currentTimeMillis
import data.bible.fetchBibleChapter
import data.bible.prefetchBibleChapter
import data.config.CloudSessionManager
import data.notifications.bookNumberForUsfm
import data.notifications.usfmForBookNumber
import data.plans.ReadingPlanRepository
import email.kevinphillips.biblebible.isDesktopPlatform
import kotlinx.coroutines.launch
import ui.plans.PlanReaderCompletionBar
import ui.plans.PlanReaderTopStrip

internal fun resolveReaderPageMode(
    showAISummary: Boolean,
    isSummaryReady: Boolean,
    isChapterReady: Boolean,
): String = when {
    showAISummary && (!isChapterReady || !isSummaryReady) -> "summary_loading"
    showAISummary -> "summary"
    !isChapterReady -> "loading"
    else -> "scripture"
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BibleScripturesPager(
    chapters: BibleChapterUIState,
    bibleVersion: String,
    selectedBook: BookData,
    isAISummaryLoading: Boolean,
    showAISummary: Boolean,
    bottomSheetScaffoldState: BottomSheetScaffoldState,
    loadChapter: suspend (BookData, Int, String, Boolean) -> Unit = { book, chapter, version, history ->
        fetchBibleChapter(book, chapter, version, history)
    },
    prefetchChapter: suspend (BookData, Int, String) -> Unit = { book, chapter, version ->
        prefetchBibleChapter(book, chapter, version)
    },
    homeBackSwipe: SwipeTransition? = null,
    animateHomeRightSwipe: Boolean = true,
    onSwipeHome: () -> Unit = {},
    verticalHomeTurn: EdgeTurn? = null,
    chapterTurn: ((BookData, Int, () -> Unit) -> EdgeTurn)? = null,
    initialShowGestureTutorial: Boolean? = null,
    onDismissGestureTutorial: () -> Unit = { ReaderTutorialPrefs.markDismissed() },
) {
    val bookId = BibleState.getBookOrdinal(selectedBook.remoteKey).coerceAtLeast(1)
    val sameBook = chapters.bookId == bookId
    val chapterCount = if (sameBook) chapters.chapterList?.size?.coerceAtLeast(1) ?: 1 else 1
    val tutorialEnabled = CloudSessionManager.featureFlags.readerTutorialEnabled
    val reminderDays = CloudSessionManager.featureFlags.readerTutorialReminderDays
    var tutorialDismissed by remember { mutableStateOf(false) }
    val tutorialEligible = remember(initialShowGestureTutorial, tutorialEnabled, reminderDays) {
        initialShowGestureTutorial ?: ReaderTutorialPrefs.shouldShow(
            enabled = tutorialEnabled,
            reminderDays = reminderDays,
        )
    }
    val showGestureTutorial = tutorialEligible && !tutorialDismissed

    fun dismissGestureTutorial(method: String) {
        if (showGestureTutorial) {
            tutorialDismissed = true
            onDismissGestureTutorial()
            AnalyticsTracker.track("reader_tutorial_dismissed", "reading", mapOf("method" to method))
        }
    }

    key(selectedBook.bookId) {
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(
            initialPage = if (sameBook) ((chapters.chapterId ?: 1) - 1).coerceIn(0, chapterCount - 1) else 0,
            pageCount = { chapterCount }
        )

        LaunchedEffect(BibleState.navigationGeneration) {
            if (BibleState.navigationGeneration == 0) return@LaunchedEffect
            val requestedChapter = BibleState.selectedChapter.substringAfter('.').toIntOrNull() ?: return@LaunchedEffect
            if (BibleState.selectedBook.bookId == selectedBook.bookId) {
                val targetPage = (requestedChapter - 1).coerceIn(0, chapterCount - 1)
                if (pagerState.currentPage != targetPage) {
                    pagerState.scrollToPage(targetPage)
                }
            }
        }

        // One request per settled navigation. Recreating an already loaded reader
        // (rotation, foregrounding) must not reload chapter 1 or add history.
        LaunchedEffect(selectedBook.bookId, bibleVersion, pagerState.settledPage) {
            val chapter = pagerState.settledPage + 1
            if (sameBook) {
                val settledSelectedChapter = "${selectedBook.bookId}.$chapter"
                if (BibleState.selectedChapter != settledSelectedChapter) {
                    BibleState.updateSelectedChapter(settledSelectedChapter)
                }
            }
            BibleState.clearDailyVerseSwipeOriginIfMoved(selectedBook, chapter)
            ReadingSession.updateLocation(canonicalBookKey(selectedBook), chapter, bibleVersion)
            AnalyticsTracker.track(
                name = "chapter_page_changed",
                category = "reading",
                properties = mapOf(
                    "bookKey" to canonicalBookKey(selectedBook),
                    "bookName" to (selectedBook.name ?: canonicalBookKey(selectedBook)),
                    "chapter" to chapter.toString(),
                    "version" to bibleVersion,
                ),
            )
            val loaded = sameBook && chapters.chapterId == chapter && chapters.text != null &&
                chapters.version.equals(bibleVersion, ignoreCase = true)
            if (!loaded) {
                val newLocation = !sameBook || chapters.chapterId != chapter
                loadChapter(selectedBook, chapter, bibleVersion, newLocation)
            }
        }

        // Quietly warm chapter + 1 into local DB + memory LRU once the user settles on a chapter.
        LaunchedEffect(selectedBook.bookId, bibleVersion, pagerState.settledPage, chapterCount, sameBook) {
            if (!sameBook) return@LaunchedEffect
            val nextChapter = pagerState.settledPage + 2
            if (nextChapter > chapterCount) return@LaunchedEffect
            kotlinx.coroutines.delay(1_200L)
            prefetchChapter(selectedBook, nextChapter, bibleVersion)
        }

        LaunchedEffect(isAISummaryLoading, showAISummary, pagerState.settledPage) {
            if (bottomSheetScaffoldState.bottomSheetState.isExpanded) {
                bottomSheetScaffoldState.bottomSheetState.collapse()
            }
        }

        // Keep AI summary mode sticky as the user swipes chapters within the same book.
        LaunchedEffect(
            selectedBook.bookId,
            chapters.chapterId,
            bibleVersion,
            showAISummary,
            pagerState.settledPage,
        ) {
            val settledChapter = pagerState.settledPage + 1
            if (
                showAISummary &&
                sameBook &&
                chapters.chapterId == settledChapter &&
                chapters.text != null &&
                GeminiModel.geminiDataText.isNullOrBlank() &&
                !GeminiModel.isLoading
            ) {
                GeminiModel.isLoading = true
                try {
                    GeminiModel.generateAISummary()
                } finally {
                    GeminiModel.isLoading = false
                }
                if (GeminiModel.showSummary && GeminiModel.geminiDataText.isNullOrBlank()) {
                    val errorMsg = GeminiModel.errorMessage ?: "Gemini returned no summary. Please try again."
                    BibleState.updateErrorSnackBar(errorMsg)
                    GeminiModel.cancelPendingSummary(preserveShowSummary = false)
                }
            }
        }

        Column(Modifier.fillMaxSize()) {
            if (sameBook && !chapters.chapterList.isNullOrEmpty()) {
                val chapterListState = rememberLazyListState()
                val density = LocalDensity.current
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(MaterialTheme.colors.surface),
                ) {
                    val viewportPx = with(density) { maxWidth.toPx() }
                    val itemWidthPx = with(density) { 42.dp.toPx() }
                    val selectedIdx = pagerState.currentPage.coerceIn(0, chapterCount - 1)

                    // Re-center the active chapter on page changes AND orientation/width changes (landscape <-> portrait)
                    LaunchedEffect(selectedIdx, chapterCount, maxWidth) {
                        val centerOffset = -((viewportPx - itemWidthPx) / 2f).toInt()
                        chapterListState.animateScrollToItem(index = selectedIdx, scrollOffset = centerOffset)
                    }

                    LazyRow(
                        state = chapterListState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(chapters.chapterList) { index, chapter ->
                            val isSelected = selectedIdx == index
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.dp, Color(0xFFFBD38D), CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        AnalyticsTracker.track(
                                            "chapter_tab_clicked",
                                            "reading",
                                            mapOf(
                                                "bookKey" to canonicalBookKey(selectedBook),
                                                "chapter" to chapter.toString(),
                                            ),
                                        )
                                        scope.launch { pagerState.animateScrollToPage(index) }
                                    },
                            ) {
                                Text(
                                    text = chapter.toString(),
                                    color = if (isSelected) Color(0xFFFBD38D) else Color.White.copy(alpha = 0.78f),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                Divider(
                    color = Color(0x33FFFFFF),
                    thickness = 1.dp,
                )
            }

            val activePlanContext = BibleState.activePlanReading?.takeIf {
                CloudSessionManager.readingPlansEnabled &&
                    sameBook &&
                    (it.reading.bookNumber == bookId || it.reading.bookId.equals(selectedBook.bookId, ignoreCase = true)) &&
                    it.reading.chapter == (chapters.chapterId ?: -1)
            }
            val isPlanReadingCompleted = activePlanContext != null &&
                activePlanContext.reading.id in ReadingPlanRepository.completedReadingIds
            val nextPlanPassage = activePlanContext?.let {
                ReadingPlanRepository.nextReadingAfter(it.planId, it.reading.id)
            }
            if (activePlanContext != null) {
                PlanReaderTopStrip(
                    context = activePlanContext,
                    scrollProgress = BibleState.activeReaderScrollProgress,
                )
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(
                lifecycleOwner,
                selectedBook.bookId,
                chapters.chapterId,
                bibleVersion,
                sameBook,
                chapters.text != null,
            ) {
                val activeChapter = chapters.chapterId
                if (!sameBook || chapters.text == null || activeChapter == null) {
                    onDispose { }
                } else {
                    var activeSegmentStartMs: Long? = currentTimeMillis()
                    var accumulatedMs = 0L

                    fun flushReadingTime() {
                        activeSegmentStartMs?.let { start ->
                            accumulatedMs += (currentTimeMillis() - start).coerceAtLeast(0L)
                            activeSegmentStartMs = null
                        }
                        if (accumulatedMs >= 2000L) {
                            AnalyticsTracker.trackChapterReadingTime(
                                bookKey = canonicalBookKey(selectedBook),
                                bookName = selectedBook.name ?: canonicalBookKey(selectedBook),
                                chapter = activeChapter,
                                version = bibleVersion,
                                durationSeconds = accumulatedMs / 1000L,
                            )
                            accumulatedMs = 0L
                        }
                    }

                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> flushReadingTime()
                            Lifecycle.Event.ON_RESUME -> {
                                if (activeSegmentStartMs == null) {
                                    activeSegmentStartMs = currentTimeMillis()
                                }
                            }
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        flushReadingTime()
                    }
                }
            }

            val scriptureScrollState = key(chapters.bookId, chapters.chapterId, bibleVersion) {
                rememberScrollState()
            }
            val summaryScrollState = key(chapters.bookId, chapters.chapterId, bibleVersion) {
                rememberScrollState()
            }
            val readingState = key(chapters.bookId, chapters.chapterId, bibleVersion) {
                val savedPosition = ReadingSession.current?.takeIf {
                    !it.isHome && it.bookKey == canonicalBookKey(selectedBook) &&
                        it.chapter == chapters.chapterId && it.version.equals(bibleVersion, true)
                }
                rememberScriptureReadingState(
                    savedPosition?.let { ScriptureAnchor(it.characterOffset, it.lineFraction) } ?: ScriptureAnchor()
                )
            }
            LaunchedEffect(readingState, selectedBook.bookId, chapters.chapterId, bibleVersion) {
                snapshotFlow { readingState.anchor }.collect { anchor ->
                    ReadingSession.updateAnchor(
                        canonicalBookKey(selectedBook), chapters.chapterId ?: 1, bibleVersion,
                        anchor.character, anchor.lineFraction
                    )
                }
            }
            DisposableEffect(readingState, scriptureScrollState, selectedBook.bookId, chapters.chapterId, bibleVersion) {
                val unregister = ReadingSession.registerAnchorCapture {
                    readingState.capture(scriptureScrollState.value)
                    ReadingSession.updateAnchor(
                        canonicalBookKey(selectedBook), chapters.chapterId ?: 1, bibleVersion,
                        readingState.anchor.character, readingState.anchor.lineFraction
                    )
                }
                onDispose { unregister() }
            }
            // Scripture and AI summary positions are deliberately separate.
            LaunchedEffect(scriptureScrollState) {
                scriptureScrollState.interactionSource.interactions.collect {
                    bottomSheetScaffoldState.bottomSheetState.collapse()
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().testTag("reader_chapter_pager").then(
                        if (homeBackSwipe != null && !isDesktopPlatform()) {
                            Modifier.readerRightBackSwipe(
                                canStart = {
                                    !pagerState.isScrollInProgress &&
                                        ((animateHomeRightSwipe && pagerState.settledPage == 0) ||
                                            BibleState.returnsHomeFromDailyVerse(selectedBook, pagerState.settledPage + 1))
                                },
                                onDrag = homeBackSwipe::drag,
                                onFinish = { cancelled ->
                                    if (!animateHomeRightSwipe && !cancelled && homeBackSwipe.progress >= 0.24f) {
                                        homeBackSwipe.dragTo(0f)
                                        onSwipeHome()
                                    } else {
                                        homeBackSwipe.finish(cancelled, onSwipeHome)
                                    }
                                },
                            )
                        } else {
                            Modifier
                        }
                    ).verticalEdgeTurns(
                        upward = {
                            if (showAISummary || !sameBook || chapters.text == null ||
                                chapters.chapterId != pagerState.settledPage + 1 || pagerState.isScrollInProgress ||
                                scriptureScrollState.maxValue == Int.MAX_VALUE || scriptureScrollState.canScrollForward
                            ) {
                                null
                            } else {
                                val nextPage = pagerState.settledPage + 1
                                if (nextPage < chapterCount) {
                                    chapterTurn?.invoke(selectedBook, nextPage + 1) {
                                        scope.launch { pagerState.scrollToPage(nextPage) }
                                    }
                                } else if (chapters.chapterList.isNullOrEmpty()) {
                                    null
                                } else {
                                    nextCanonicalBook(selectedBook, BibleState.uiBooks.data.orEmpty())?.let { nextBook ->
                                        chapterTurn?.invoke(nextBook, 1) {
                                            BibleState.returnToShortsOnBack = false
                                            BibleState.restoreReadingLocation(nextBook, 1)
                                        }
                                    }
                                }
                            }
                        },
                        downward = {
                            verticalHomeTurn.takeIf {
                                !showAISummary && sameBook && chapters.text != null &&
                                    chapters.chapterId == pagerState.settledPage + 1 &&
                                    !pagerState.isScrollInProgress && !scriptureScrollState.canScrollBackward
                            }
                        },
                    ),
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = !isDesktopPlatform()
                ) { page ->
                    val targetChapter = page + 1
                    val isChapterReady = sameBook && chapters.chapterId == targetChapter &&
                        chapters.version.equals(bibleVersion, ignoreCase = true) && chapters.text != null
                    val pageMode = resolveReaderPageMode(
                        showAISummary = showAISummary,
                        isSummaryReady = !isAISummaryLoading && !GeminiModel.geminiDataText.isNullOrBlank(),
                        isChapterReady = isChapterReady,
                    )
                    key(showAISummary) {
                        Crossfade(
                            targetState = pageMode,
                            animationSpec = tween(durationMillis = 180),
                            modifier = Modifier.fillMaxSize(),
                            label = "chapter_page_crossfade",
                        ) { mode ->
                            when (mode) {
                                "loading" -> ScriptureShimmerLoading()
                                "summary_loading" -> AiSummaryLoadingView()
                                "summary" -> key(chapters.bookId, chapters.chapterId, bibleVersion) {
                                    GeminiSummary(summaryScrollState, BibleState.selectedFontSize)
                                }
                                else -> BibleScriptures(
                                    chapters,
                                    scriptureScrollState,
                                    BibleState.selectedFontSize,
                                    onFontSizeChanged = { BibleState.selectedFontSize = it },
                                    readingState = readingState,
                                    onFontGestureUsed = { dismissGestureTutorial("gesture") },
                                )
                            }
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showGestureTutorial &&
                        !showAISummary &&
                        sameBook &&
                        chapters.text != null &&
                        !isPlanReadingCompleted &&
                        !BibleState.autoDemoTourEnabled,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    ReaderGestureTutorialCard(
                        onDismiss = { dismissGestureTutorial("button") },
                    )
                }

                PlanReaderCompletionBar(
                    visible = isPlanReadingCompleted && !showAISummary,
                    hasNextPassage = nextPlanPassage != null,
                    onBackToPlan = { BibleState.navigateBackFromReader() },
                    onNextPassage = {
                        nextPlanPassage?.let { nextContext ->
                            BibleState.openPlanReading(nextContext)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    }
}

internal fun nextCanonicalBook(book: BookData, books: List<BookData>): BookData? {
    val ordinal = bookNumberForUsfm(book.bookId) ?: return null
    val nextUsfm = usfmForBookNumber(ordinal + 1) ?: return null
    return books.firstOrNull { it.bookId.equals(nextUsfm, ignoreCase = true) }
}

@Composable
private fun ReaderGestureTutorialCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xF015243A),
        border = BorderStroke(1.dp, Color(0x66FBD38D)),
        elevation = 8.dp,
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .testTag("reader_gesture_tutorial_card"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Reading Tips",
                    color = Color(0xFFFBD38D),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFFBD38D),
                        contentColor = Color(0xFF0F172A),
                    ),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("reader_tutorial_dismiss_btn"),
                ) {
                    Text(
                        text = "Got it",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            ReaderTutorialTipRow(
                badge = "Pinch",
                title = "Pinch to zoom",
                subtitle = "Spread or pinch two fingers to resize text",
            )
            ReaderTutorialTipRow(
                badge = "2×",
                title = "Double-tap text",
                subtitle = "Cycle quickly between normal and large font",
            )
        }
    }
}

@Composable
private fun ReaderTutorialTipRow(
    badge: String,
    title: String,
    subtitle: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(50.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x28FBD38D)),
        ) {
            Text(
                text = badge,
                color = Color(0xFFFBD38D),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
            )
        }
    }
}
