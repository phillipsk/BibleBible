package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiModel
import data.analytics.AnalyticsTracker
import data.bible.BibleState
import data.bible.BookData
import data.bible.Chapter
import data.bible.DailyVerse
import data.bible.DailyVerseRepository
import data.bible.dailyVerseRepository
import data.bible.bibleBooksFull
import data.config.CloudSessionManager
import data.notifications.VotdNotificationController
import data.notifications.parseVerseRange
import data.plans.ReadingPlanRepository
import data.shorts.BiblicalShortDto
import email.kevinphillips.biblebible.PlatformBackHandler
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ui.configs.BibleBibleTopBar
import ui.configs.BottomSheetConfigView
import ui.configs.DailyReminderTimePickerRow
import ui.configs.FloatingHomeTopBar
import ui.configs.HomeTopBarHeight
import ui.plans.ReadingPlanBannerCard
import ui.plans.ReadingPlanTwinCard
import ui.plans.ReadingPlansScreen
import ui.search.ElasticSearchTriggerBar
import ui.search.SpotlightSearchScreen
import ui.shorts.BiblicalShortsEntryCard
import ui.shorts.BiblicalShortsScreen

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BibleHomeScreen(
    scaffoldState: BottomSheetScaffoldState,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    val localScaffoldState = remember { scaffoldState }
    val channel = remember { BibleState.snackBarChannel }

    LaunchedEffect(BibleState.showHomePage) {
        localScaffoldState.bottomSheetState.collapse()
    }

    LaunchedEffect(
        BibleState.showHomePage,
        BibleState.showBiblicalShorts,
        BibleState.isSearchActive,
        BibleState.showReadingPlans,
    ) {
        val activeScreen = when {
            BibleState.isSearchActive -> "spotlight_search"
            BibleState.showReadingPlans -> "reading_plans"
            BibleState.showBiblicalShorts -> "biblical_shorts"
            BibleState.showHomePage -> "home"
            else -> "scripture_reader"
        }
        AnalyticsTracker.trackScreenView(activeScreen)
    }

    LaunchedEffect(
        BibleState.autoDemoTourEnabled,
        BibleState.showBiblicalShorts,
        BibleState.isSearchActive,
        BibleState.showReadingPlans,
    ) {
        if (BibleState.autoDemoTourEnabled &&
            !BibleState.showBiblicalShorts &&
            !BibleState.isSearchActive &&
            !BibleState.showReadingPlans
        ) {
            BibleState.showHomePage = true
            delay(3500L)
            if (BibleState.autoDemoTourEnabled) {
                BibleState.openBiblicalShorts()
            }
        }
    }

    LaunchedEffect(Unit) {
        VotdNotificationController.refresh()
        channel.receiveAsFlow().collect { errorMsg ->
            localScaffoldState.snackbarHostState.showSnackbar(message = errorMsg)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                VotdNotificationController.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (BibleState.showHomePage &&
        !BibleState.showBiblicalShorts &&
        !BibleState.isSearchActive &&
        !BibleState.showReadingPlans &&
        !BibleState.autoDemoTourEnabled &&
        VotdNotificationController.shouldShowOptInPrompt
    ) {
        Dialog(onDismissRequest = { VotdNotificationController.dismissOptInPrompt() }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF132238),
                border = BorderStroke(1.5.dp, Color(0x66FBD38D)),
                elevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .testTag("votd_opt_in_dialog"),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        text = "Daily Verse of the Day",
                        color = Color(0xFFFBD38D),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Start your morning in God's Word. Choose your daily reminder time below and tap straight to that book, chapter, and verse. You can adjust or turn this off anytime in Settings.",
                        color = Color(0xFFE2E8F0),
                        fontSize = 16.sp,
                        lineHeight = 23.sp,
                    )
                    DailyReminderTimePickerRow(
                        selectedHour = VotdNotificationController.reminderHour,
                        selectedMinute = VotdNotificationController.reminderMinute,
                        onTimeSelected = { hour, minute ->
                            VotdNotificationController.updateReminderTime(hour, minute)
                        },
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextButton(onClick = { VotdNotificationController.dismissOptInPrompt() }) {
                            Text(
                                text = "Not now",
                                color = Color(0xFFCBD5E1),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { VotdNotificationController.onEnableRequested() },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFFBD38D),
                                contentColor = Color(0xFF09111E),
                            ),
                            modifier = Modifier.testTag("votd_opt_in_enable_btn"),
                        ) {
                            Text(
                                text = "Enable",
                                color = Color(0xFF09111E),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    val isBottomSheetExpanded = localScaffoldState.bottomSheetState.isExpanded
    val isHome = BibleState.showHomePage &&
        !BibleState.showBiblicalShorts &&
        !BibleState.isSearchActive &&
        !BibleState.showReadingPlans
    val homeBackSwipe = remember(scope) { SwipeTransition(scope) }
    val animateHomeRightSwipe = CloudSessionManager.featureFlags.homeSwipePreviewEnabled
    val horizontalHomeOffset by remember(homeBackSwipe, animateHomeRightSwipe) {
        derivedStateOf { if (animateHomeRightSwipe) homeBackSwipe.offset else 0f }
    }
    val readerDownTransition = remember(scope) { SwipeTransition(scope) }
    val chapterEntryTransition = remember(scope) { SwipeTransition(scope) }
    val showChapterPreview by remember { derivedStateOf { chapterEntryTransition.offset > 0f } }
    val revealHomeVertically by remember { derivedStateOf { readerDownTransition.offset > 0f } }
    var chapterPreview by remember { mutableStateOf<ChapterPreviewTarget?>(null) }
    var chapterPreviewCommitted by remember { mutableStateOf(false) }

    fun chapterTurn(book: BookData, chapter: Int, open: () -> Unit): EdgeTurn =
        EdgeTurn(
            transition = chapterEntryTransition,
            onStart = {
                chapterPreview = ChapterPreviewTarget(book, chapter)
                chapterPreviewCommitted = false
            },
            onCommit = {
                chapterPreviewCommitted = true
                open()
            },
        )
    val homeGridState = rememberLazyGridState()
    val flags = CloudSessionManager.featureFlags
    val genesis by rememberUpdatedState(
        BibleState.uiBooks.data?.firstOrNull { it.bookId.equals("GEN", ignoreCase = true) }
    )

    LaunchedEffect(flags.biblicalShortsEnabled) {
        if (!flags.biblicalShortsEnabled && BibleState.showBiblicalShorts) {
            BibleState.exitBiblicalShortsToHome()
        }
    }
    LaunchedEffect(flags.elasticSearchEnabled) {
        if (!flags.elasticSearchEnabled && BibleState.isSearchActive) {
            BibleState.closeSpotlightSearch()
        }
    }
    LaunchedEffect(flags.readingPlansEnabled) {
        if (!flags.readingPlansEnabled && BibleState.showReadingPlans) {
            BibleState.exitReadingPlansToHome()
        }
    }
    val exitShorts: () -> Unit = { BibleState.exitBiblicalShortsToHome() }
    val exitReader: () -> Unit = { BibleState.onHomeClick() }

    val navigateBackFromReader: () -> Unit = {
        if (BibleState.returnToShortsOnBack || BibleState.returnToPlansOnBack) {
            BibleState.navigateBackFromReader()
        } else if (flags.homeSwipePreviewEnabled) {
            homeBackSwipe.goBack(exitReader)
        } else {
            exitReader()
        }
    }
    val navigateBackFromShorts: () -> Unit = {
        if (flags.homeSwipePreviewEnabled) homeBackSwipe.goBack(exitShorts) else exitShorts()
    }
    val handleBackNavigation: () -> Unit = {
        when {
            isBottomSheetExpanded -> scope.launch {
                localScaffoldState.bottomSheetState.collapse()
            }
            BibleState.isSearchActive -> BibleState.closeSpotlightSearch()
            BibleState.showReadingPlans -> BibleState.exitReadingPlansToHome()
            BibleState.showBiblicalShorts -> navigateBackFromShorts()
            !isHome -> navigateBackFromReader()
        }
    }

    PlatformBackHandler(enabled = isBottomSheetExpanded || !isHome, onBack = handleBackNavigation)

    BottomSheetScaffold(
        snackbarHost = {
            CustomSnackbarHost(snackbarHostState = scaffoldState.snackbarHostState)
        },
        scaffoldState = scaffoldState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetPeekHeight = 0.dp,
        sheetContent = {
            BottomSheetConfigView(
                bibleVersionsList = BibleState.bibleVersions,
                showAISummary = GeminiModel.showSummary,
                bottomSheetState = localScaffoldState.bottomSheetState
            )
        },
        content = {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .onSizeChanged {
                        homeBackSwipe.extent = it.width
                        readerDownTransition.extent = it.height
                        chapterEntryTransition.extent = it.height
                    },
            ) {
                if (isHome || horizontalHomeOffset > 0f || revealHomeVertically) {
                    Column(
                        modifier = if (isHome) {
                            Modifier.fillMaxSize()
                                .then(
                                    if (!isBottomSheetExpanded && flags.homeSwipeToShortsEnabled && flags.biblicalShortsEnabled) {
                                        Modifier.homeSwipeToWords { BibleState.openBiblicalShorts() }
                                    } else Modifier,
                                )
                                .verticalEdgeTurns(upward = {
                                    val book = genesis
                                    if (!isBottomSheetExpanded && flags.homeScrollToGenesisEnabled && book != null &&
                                        !BibleState.sortAZ && homeGridState.canScrollBackward && !homeGridState.canScrollForward
                                    ) {
                                        chapterTurn(book, 1) {
                                            AnalyticsTracker.trackBookSelected(book.bookId, book.name ?: "Genesis", "home_grid_end")
                                            BibleState.returnToShortsOnBack = false
                                            BibleState.restoreReadingLocation(book, 1)
                                        }
                                    } else null
                                })
                        } else {
                            Modifier.fillMaxSize().clearAndSetSemantics { hideFromAccessibility() }
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.fillMaxSize().graphicsLayer {
                            val progress = if (isHome) chapterEntryTransition.progress else 0f
                            alpha = 1f - progress
                            translationY = -size.height * progress * 0.055f
                        }) {
                            BibleHomeContent(homeGridState, interactive = isHome)
                        }
                    }
                }

                if (!isHome) {
                    if (horizontalHomeOffset > 0f || revealHomeVertically) {
                        // The revealed Home screen remains a preview until the navigation completes.
                        Box(Modifier.matchParentSize().pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) awaitPointerEvent().changes.forEach { it.consume() }
                            }
                        })
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .absoluteOffset { IntOffset(horizontalHomeOffset.roundToInt(), 0) }
                            .graphicsLayer {
                                val forward = chapterEntryTransition.progress
                                translationY = readerDownTransition.offset - size.height * forward * 0.055f
                                alpha = (1f - readerDownTransition.progress * 0.72f) * (1f - forward)
                            }
                            .background(MaterialTheme.colors.background),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (BibleState.isSearchActive) {
                            SpotlightSearchScreen(
                                onBack = { BibleState.closeSpotlightSearch() },
                                onSelectBook = { book ->
                                    AnalyticsTracker.trackBookSelected(
                                        bookId = book.bookId,
                                        bookName = book.name ?: book.bookId,
                                        source = "spotlight_search",
                                    )
                                    BibleState.closeSpotlightSearch()
                                    BibleState.returnToShortsOnBack = false
                                    BibleState.returnToPlansOnBack = false
                                    BibleState.activePlanReading = null
                                    BibleState.restoreReadingLocation(book, 1)
                                },
                                onJumpToReference = { bookId, bookNumber, bookName, chapter, verseStart, verseEnd, source ->
                                    val targetBook = BibleState.resolveBookData(
                                        bookId = bookId,
                                        bookNumber = bookNumber,
                                        fallbackName = bookName,
                                        minChapters = chapter,
                                    )
                                    AnalyticsTracker.trackBookSelected(
                                        bookId = targetBook.bookId,
                                        bookName = targetBook.name ?: targetBook.bookId,
                                        source = source,
                                    )
                                    BibleState.closeSpotlightSearch()
                                    BibleState.returnToShortsOnBack = false
                                    BibleState.returnToPlansOnBack = false
                                    BibleState.activePlanReading = null
                                    val safeChapter = chapter.coerceAtLeast(1)
                                    BibleState.restoreReadingLocation(targetBook, safeChapter)
                                    if (verseStart >= 1) {
                                        BibleState.focusScriptureVerse(
                                            bookId = targetBook.bookId,
                                            chapter = safeChapter,
                                            verseStart = verseStart,
                                            verseEnd = verseEnd.coerceAtLeast(verseStart),
                                        )
                                    } else {
                                        BibleState.clearHighlightedVerse()
                                        BibleState.clearPendingTargetVerse()
                                    }
                                },
                                onOpenPlan = { planId ->
                                    scope.launch {
                                        ReadingPlanRepository.selectActivePlan(planId)
                                    }
                                    BibleState.closeSpotlightSearch()
                                    BibleState.openReadingPlans()
                                },
                            )
                        } else if (BibleState.showReadingPlans) {
                            ReadingPlansScreen(
                                onBack = { BibleState.exitReadingPlansToHome() },
                                onOpenReading = { context -> BibleState.openPlanReading(context) },
                            )
                        } else if (BibleState.showBiblicalShorts) {
                            BiblicalShortsScreen(
                                onBack = navigateBackFromShorts,
                                onReadChapter = { shortItem -> openShortScriptureChapter(shortItem) },
                                resumeShortId = BibleState.resumeShortIdOnReturn,
                                onBackSwipeDrag = if (flags.homeSwipePreviewEnabled) homeBackSwipe::drag else { _ -> },
                                onBackSwipeFinish = if (flags.homeSwipePreviewEnabled) ({ cancelled ->
                                    homeBackSwipe.finish(cancelled) {
                                        AnalyticsTracker.track("short_swipe_back_home", "biblical_shorts")
                                        exitShorts()
                                    }
                                }) else null,
                            )
                        } else {
                            BibleBibleTopBar(
                                onClick = navigateBackFromReader,
                                showBottomSheet = {
                                    scope.launch {
                                        val sheet = localScaffoldState.bottomSheetState
                                        val target = if (sheet.isExpanded) "collapsed" else "expanded"
                                        AnalyticsTracker.track(
                                            "bottom_sheet_toggled",
                                            "navigation",
                                            mapOf("state" to target),
                                        )
                                        if (sheet.isExpanded) sheet.collapse() else sheet.expand()
                                    }
                                },
                            )
                            BibleState.bibleChapter?.let { chapter ->
                                BibleScripturesPager(
                                    chapters = chapter,
                                    bibleVersion = BibleState.selectedVersion,
                                    selectedBook = BibleState.selectedBook,
                                    isAISummaryLoading = GeminiModel.isLoading,
                                    showAISummary = GeminiModel.showSummary,
                                    bottomSheetScaffoldState = localScaffoldState,
                                    homeBackSwipe = homeBackSwipe.takeIf {
                                        !isBottomSheetExpanded && (animateHomeRightSwipe || BibleState.hasDailyVerseSwipeOrigin)
                                    },
                                    animateHomeRightSwipe = animateHomeRightSwipe,
                                    onSwipeHome = exitReader,
                                    verticalHomeTurn = if (flags.homeSwipePreviewEnabled && !isBottomSheetExpanded) {
                                        EdgeTurn(readerDownTransition, onCommit = exitReader)
                                    } else null,
                                    chapterTurn = if (!isBottomSheetExpanded) ::chapterTurn else null,
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showChapterPreview,
                    enter = EnterTransition.None,
                    exit = fadeOut(animationSpec = tween(140)),
                ) {
                    chapterPreview?.let { target ->
                        ChapterEntryPreview(
                            bookName = target.book.name ?: target.book.bookId,
                            chapter = target.chapter,
                            showGenesisVerse = target.chapter == 1 && target.book.bookId.equals("GEN", true) &&
                                BibleState.selectedVersion.equals("KJV", true),
                            progress = { if (chapterPreviewCommitted) 1f else chapterEntryTransition.progress },
                        )
                    }
                }
            }
        },
    )
}

private data class ChapterPreviewTarget(val book: BookData, val chapter: Int)

private const val StudyHeadingKey = "study_the_word_heading"

internal fun homeBarGlassProgress(gridState: LazyGridState, fadeDistance: Float): Float =
    if (gridState.firstVisibleItemIndex > 0) {
        1f
    } else {
        (gridState.firstVisibleItemScrollOffset / fadeDistance).coerceIn(0f, 1f)
    }

@Composable
internal fun BibleHomeContent(
    gridState: LazyGridState,
    interactive: Boolean = true,
    verseRepository: DailyVerseRepository = dailyVerseRepository,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fadeDistance = with(LocalDensity.current) { 48.dp.toPx() }
    val glassProgress by remember(gridState, fadeDistance) {
        derivedStateOf { homeBarGlassProgress(gridState, fadeDistance) }
    }
    val version = BibleState.selectedVersion
    val dailyVerse = if (CloudSessionManager.verseOfTheDayEnabled) rememberDailyVerse(version, verseRepository) else null
    val refreshing = dailyVerse?.refreshing == true
    Box(Modifier.fillMaxSize()) {
    HomePullRefreshBox(
        enabled = interactive && dailyVerse != null,
        refreshing = refreshing,
        onRefresh = { dailyVerse?.refresh() },
        indicatorTop = topInset + HomeTopBarHeight + 8.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
      Box(Modifier.fillMaxSize()) {
        BibleBookList(
            bookData = BibleState.uiBooks.data,
            lazyGridState = gridState,
            visible = true,
            topChromePadding = topInset + HomeTopBarHeight,
            header = { windowMetrics ->
                val showShorts = CloudSessionManager.biblicalShortsEnabled
                val showPlans = CloudSessionManager.readingPlansEnabled
                val plansLayout = CloudSessionManager.homePlansLayout
                val useTwinCards = showShorts && showPlans &&
                    (plansLayout == "twin_cards" || (plansLayout == "adaptive" && windowMetrics.isWideHome))
                val hingeGap = windowMetrics.hingeSafeGap.coerceAtLeast(12.dp)

                if (windowMetrics.isWideHome && dailyVerse != null) {
                    when {
                        showShorts && showPlans -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(hingeGap),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                            ) {
                                VerseOfTheDay(
                                    presentation = dailyVerse,
                                    version = version,
                                    modifier = Modifier
                                        .weight(1.05f)
                                        .fillMaxHeight(),
                                    onVerseClick = { dv -> openDailyVerseChapter(dv) },
                                )
                                if (plansLayout == "twin_cards") {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .weight(0.95f)
                                            .fillMaxHeight(),
                                    ) {
                                        BiblicalShortsEntryCard(
                                            onOpenShorts = {
                                                BibleState.resumeShortIdOnReturn = null
                                                BibleState.openBiblicalShorts()
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                        )
                                        ReadingPlanTwinCard(
                                            onOpenPlans = { BibleState.openReadingPlans() },
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                        )
                                    }
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(0.dp),
                                        modifier = Modifier.weight(0.95f),
                                    ) {
                                        BiblicalShortsEntryCard(
                                            onOpenShorts = {
                                                BibleState.resumeShortIdOnReturn = null
                                                BibleState.openBiblicalShorts()
                                            },
                                        )
                                        ReadingPlanTwinCard(
                                            onOpenPlans = { BibleState.openReadingPlans() },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }
                        showShorts -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(hingeGap),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                            ) {
                                VerseOfTheDay(
                                    presentation = dailyVerse,
                                    version = version,
                                    modifier = Modifier
                                        .weight(1.15f)
                                        .fillMaxHeight(),
                                    onVerseClick = { dv -> openDailyVerseChapter(dv) },
                                )
                                BiblicalShortsEntryCard(
                                    onOpenShorts = {
                                        BibleState.resumeShortIdOnReturn = null
                                        BibleState.openBiblicalShorts()
                                    },
                                    modifier = Modifier
                                        .weight(0.85f)
                                        .fillMaxHeight(),
                                )
                            }
                        }
                        showPlans -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(hingeGap),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                            ) {
                                VerseOfTheDay(
                                    presentation = dailyVerse,
                                    version = version,
                                    modifier = Modifier
                                        .weight(1.15f)
                                        .fillMaxHeight(),
                                    onVerseClick = { dv -> openDailyVerseChapter(dv) },
                                )
                                ReadingPlanBannerCard(
                                    onOpenPlans = { BibleState.openReadingPlans() },
                                    modifier = Modifier
                                        .weight(0.85f)
                                        .fillMaxHeight(),
                                )
                            }
                        }
                        else -> {
                            VerseOfTheDay(
                                presentation = dailyVerse,
                                version = version,
                                onVerseClick = { dv -> openDailyVerseChapter(dv) },
                            )
                        }
                    }
                } else {
                    Column {
                        if (dailyVerse != null) {
                            VerseOfTheDay(
                                presentation = dailyVerse,
                                version = version,
                                onVerseClick = { dv -> openDailyVerseChapter(dv) },
                            )
                        }
                        if (useTwinCards) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                            ) {
                                BiblicalShortsEntryCard(
                                    onOpenShorts = {
                                        BibleState.resumeShortIdOnReturn = null
                                        BibleState.openBiblicalShorts()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )
                                ReadingPlanTwinCard(
                                    onOpenPlans = { BibleState.openReadingPlans() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )
                            }
                        } else {
                            if (showShorts) {
                                BiblicalShortsEntryCard(
                                    onOpenShorts = {
                                        BibleState.resumeShortIdOnReturn = null
                                        BibleState.openBiblicalShorts()
                                    }
                                )
                            }
                            if (showPlans) {
                                ReadingPlanBannerCard(
                                    onOpenPlans = { BibleState.openReadingPlans() },
                                )
                            }
                        }
                    }
                }
            },
        )
      }
    }
    FloatingHomeTopBar(progress = { glassProgress })
    }
}

private fun partitionBookSections(
    indexedBooks: List<IndexedValue<BookData>>,
    sectionBreakIds: Set<String>,
    applySections: Boolean,
): List<List<IndexedValue<BookData>>> {
    if (!applySections || indexedBooks.isEmpty()) {
        return listOf(indexedBooks)
    }
    val sections = mutableListOf<List<IndexedValue<BookData>>>()
    var currentSection = mutableListOf<IndexedValue<BookData>>()
    indexedBooks.forEachIndexed { pos, indexedBook ->
        currentSection.add(indexedBook)
        val usfm = indexedBook.value.bookId.uppercase()
        if (usfm in sectionBreakIds && pos < indexedBooks.lastIndex) {
            sections.add(currentSection)
            currentSection = mutableListOf()
        }
    }
    if (currentSection.isNotEmpty()) {
        sections.add(currentSection)
    }
    return sections
}

@Composable
internal fun BibleBookList(
    bookData: List<BookData>?,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    visible: Boolean = BibleState.showHomePage,
    topChromePadding: Dp = 0.dp,
    header: (@Composable (windowMetrics: AdaptiveWindowMetrics) -> Unit)? = null,
) {
    val shimmerStyle = CloudSessionManager.bookShimmerStyle
    val shimmerPace = CloudSessionManager.bookShimmerPace
    val shimmerIntensity = CloudSessionManager.bookShimmerIntensity
    val sectionPaddingDp = CloudSessionManager.bookSectionPaddingDp
    val tabletLayoutMode = CloudSessionManager.bookGridTabletLayout
    val mobileBreakIds = CloudSessionManager.bookSectionBreaksMobile
    val tabletBreakIds = CloudSessionManager.bookSectionBreaksTablet

    val baseDurationMs = when (shimmerStyle) {
        "soft_gold_sheen" -> 5400
        "candle_breathe" -> 6200
        "row_wave" -> 7000
        "horizon_pulse" -> 8000
        "electric" -> 4800
        else -> 5400
    }
    val cycleDurationMs = (baseDurationMs / shimmerPace).roundToInt().coerceAtLeast(800)
    val shimmerProgress by if (shimmerStyle == "none" || shimmerIntensity <= 0.01f) {
        remember { mutableStateOf(0f) }
    } else {
        key(shimmerStyle, cycleDurationMs) {
            val infiniteTransition = rememberInfiniteTransition(label = "study_word_shimmer")
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = cycleDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "shimmer_progress",
            )
        }
    }
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(initialAlpha = 0.4f),
        exit = fadeOut(animationSpec = tween(durationMillis = 250))
    ) {
        bookData?.let { bookDataList ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val windowMetrics = computeAdaptiveWindowMetrics(maxWidth, maxHeight)
                val columnsCount = windowMetrics.bookGridColumns
                val indexedAll = remember(bookDataList) { bookDataList.withIndex().toList() }
                val otBooks = remember(indexedAll) {
                    indexedAll.filter { BibleState.getBookOrdinal(it.value.bookId) <= 39 }
                }
                val ntBooks = remember(indexedAll) {
                    indexedAll.filter { BibleState.getBookOrdinal(it.value.bookId) > 39 }
                }
                val useSplitTestaments = !BibleState.sortAZ &&
                    columnsCount >= 6 &&
                    tabletLayoutMode == "split_testaments" &&
                    otBooks.isNotEmpty() &&
                    ntBooks.isNotEmpty()
                val applySectionBreaks = !BibleState.sortAZ && sectionPaddingDp > 0
                val activeBreakIds = when {
                    columnsCount <= 3 || tabletLayoutMode == "mobile_breaks" -> mobileBreakIds
                    else -> tabletBreakIds
                }
                val flatSections = remember(indexedAll, activeBreakIds, applySectionBreaks) {
                    partitionBookSections(indexedAll, activeBreakIds, applySectionBreaks)
                }

                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Fixed(columnsCount),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp + topChromePadding,
                        bottom = 20.dp + navBarBottomPadding,
                    ),
                    userScrollEnabled = true,
                ) {
                    if (header != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) { header(windowMetrics) }
                        item(key = StudyHeadingKey, span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "Study the Word",
                                style = TextStyle(
                                    fontFamily = MaterialTheme.typography.h1.fontFamily,
                                    letterSpacing = 1.8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 23.sp,
                                    color = Color.White,
                                ),
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp, start = 4.dp)
                            )
                        }
                        if (CloudSessionManager.elasticSearchEnabled) {
                            item(key = "elastic_search_bar", span = { GridItemSpan(maxLineSpan) }) {
                                ElasticSearchTriggerBar(
                                    onActivateSearch = { BibleState.openSpotlightSearch() },
                                    modifier = Modifier.padding(bottom = 12.dp),
                                )
                            }
                        }
                    }

                    if (useSplitTestaments) {
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "tablet_split_testaments_grid",
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tablet_split_testaments_grid"),
                            ) {
                                SplitTestamentColumn(
                                    heading = "Old Testament",
                                    indexedBooks = otBooks,
                                    breakIds = mobileBreakIds,
                                    sectionPaddingDp = sectionPaddingDp,
                                    shimmerStyle = shimmerStyle,
                                    shimmerProgress = shimmerProgress,
                                    shimmerIntensity = shimmerIntensity,
                                    modifier = Modifier.weight(1f),
                                )
                                SplitTestamentColumn(
                                    heading = "New Testament",
                                    indexedBooks = ntBooks,
                                    breakIds = mobileBreakIds,
                                    sectionPaddingDp = sectionPaddingDp,
                                    shimmerStyle = shimmerStyle,
                                    shimmerProgress = shimmerProgress,
                                    shimmerIntensity = shimmerIntensity,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    } else {
                        flatSections.forEachIndexed { sectionIndex, sectionBooks ->
                            itemsIndexed(
                                items = sectionBooks,
                                key = { _, indexedBook -> "${indexedBook.value.bookId}-${indexedBook.index}" },
                            ) { _, indexedBook ->
                                BookTileCard(
                                    index = indexedBook.index,
                                    book = indexedBook.value,
                                    columnsCount = columnsCount,
                                    shimmerStyle = shimmerStyle,
                                    shimmerProgress = shimmerProgress,
                                    shimmerIntensity = shimmerIntensity,
                                )
                            }
                            if (applySectionBreaks && sectionIndex < flatSections.lastIndex) {
                                item(
                                    span = { GridItemSpan(maxLineSpan) },
                                    key = "book_section_spacer_$sectionIndex",
                                ) {
                                    val isTestamentDivider = columnsCount > 3 &&
                                        sectionBooks.lastOrNull()?.value?.bookId.equals("MAL", ignoreCase = true)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("book_section_spacer_$sectionIndex"),
                                    ) {
                                        Spacer(modifier = Modifier.height(sectionPaddingDp.dp))
                                        if (isTestamentDivider) {
                                            Text(
                                                text = "New Testament",
                                                color = Color(0xFFFBD38D),
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 6.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitTestamentColumn(
    heading: String,
    indexedBooks: List<IndexedValue<BookData>>,
    breakIds: Set<String>,
    sectionPaddingDp: Int,
    shimmerStyle: String,
    shimmerProgress: Float,
    shimmerIntensity: Float,
    modifier: Modifier = Modifier,
) {
    val sections = remember(indexedBooks, breakIds, sectionPaddingDp) {
        partitionBookSections(indexedBooks, breakIds, sectionPaddingDp > 0)
    }
    Column(modifier = modifier) {
        Text(
            text = heading,
            color = Color(0xFFFBD38D),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
        )
        sections.forEachIndexed { sectionIndex, sectionBooks ->
            val chunkRows = sectionBooks.chunked(3)
            chunkRows.forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (slot in 0 until 3) {
                        val entry = rowItems.getOrNull(slot)
                        if (entry != null) {
                            Box(modifier = Modifier.weight(1f)) {
                                BookTileCard(
                                    index = entry.index,
                                    book = entry.value,
                                    columnsCount = 3,
                                    shimmerStyle = shimmerStyle,
                                    shimmerProgress = shimmerProgress,
                                    shimmerIntensity = shimmerIntensity,
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            if (sectionPaddingDp > 0 && sectionIndex < sections.lastIndex) {
                Spacer(
                    modifier = Modifier
                        .height(sectionPaddingDp.dp)
                        .testTag("split_${heading.lowercase().replace(' ', '_')}_spacer_$sectionIndex"),
                )
            }
        }
    }
}

@Composable
private fun BookTileCard(
    index: Int,
    book: BookData,
    columnsCount: Int,
    shimmerStyle: String,
    shimmerProgress: Float,
    shimmerIntensity: Float,
) {
    val row = index / columnsCount
    val col = index % columnsCount
    // Composite the book tint solidly over the deep background so there is no translucent box artifact
    val rawColor = BibleState.getBibleBookColor(BibleState.getBookOrdinal(book.bookId))
    val solidBase = rawColor.compositeOver(Color(0xFF1A2838))
    val tileBrush = Brush.verticalGradient(
        colors = listOf(
            solidBase.copy(alpha = 1f),
            Color(0x1809111E).compositeOver(solidBase),
        )
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shimmerBorderAlpha = computeShimmerBorderAlpha(
        shimmerStyle = shimmerStyle,
        shimmerProgress = shimmerProgress,
        shimmerIntensity = shimmerIntensity,
        index = index,
        row = row,
        col = col,
        columnsCount = columnsCount,
    )
    val borderWidth = if (isPressed || (shimmerStyle == "electric" && shimmerBorderAlpha > 0.25f)) 2.dp else 1.dp
    val borderColor = when {
        isPressed -> Color(0xFFFBD38D).copy(alpha = 0.85f)
        shimmerBorderAlpha > 0.02f -> Color(0xFFFBD38D).copy(alpha = shimmerBorderAlpha)
        else -> Color(0x36FFFFFF)
    }

    val tileShape = RoundedCornerShape(14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .defaultMinSize(minWidth = 100.dp, minHeight = 46.dp)
            .clip(tileShape)
            .background(tileBrush)
            .border(width = borderWidth, color = borderColor, shape = tileShape)
            .drawWithContent {
                drawContent()
                drawBookTileShimmer(
                    shimmerStyle = shimmerStyle,
                    shimmerProgress = shimmerProgress,
                    shimmerIntensity = shimmerIntensity,
                    index = index,
                    row = row,
                    col = col,
                    columnsCount = columnsCount,
                    isPressed = isPressed,
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                AnalyticsTracker.trackBookSelected(
                    bookId = book.bookId,
                    bookName = book.name ?: book.cleanedName ?: book.bookId,
                    source = "home_grid",
                )
                BibleState.returnToShortsOnBack = false
                BibleState.updateSelectedChapter(book.remoteKey)
                BibleState.updateSelectedBook(book)
                BibleState.showHomePage = false
            }
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        book.cleanedName?.let { name ->
            Text(
                fontFamily = MaterialTheme.typography.h3.fontFamily,
                fontWeight = FontWeight.SemiBold,
                text = name,
                fontSize = 13.5.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun computeShimmerBorderAlpha(
    shimmerStyle: String,
    shimmerProgress: Float,
    shimmerIntensity: Float,
    index: Int,
    row: Int,
    col: Int,
    columnsCount: Int,
): Float {
    if (shimmerIntensity <= 0.01f) return 0f
    return when (shimmerStyle) {
        "soft_gold_sheen" -> {
            val cycle = (shimmerProgress + col * 0.08f + (row % 7) * 0.12f) % 1f
            if (cycle >= 0.36f) 0f else {
                val env = sin((cycle / 0.36f) * PI).toFloat()
                (0.28f * env * shimmerIntensity).coerceIn(0f, 0.6f)
            }
        }
        "candle_breathe" -> {
            val wave = ((sin((shimmerProgress * 2.0 * PI) + index * 0.65) + 1.0) * 0.5).toFloat()
            (0.10f + wave * 0.24f * shimmerIntensity).coerceIn(0f, 0.55f)
        }
        "row_wave" -> {
            val rowPos = shimmerProgress * 9.5f - (row % 8) * 0.72f
            if (rowPos <= 0f || rowPos >= 1.2f) 0f else {
                val env = sin((rowPos / 1.2f) * PI).toFloat()
                (0.24f * env * shimmerIntensity).coerceIn(0f, 0.45f)
            }
        }
        "electric" -> {
            val cycle = shimmerProgress * 2f
            val isLeftToRight = cycle < 1f
            val passProgress = if (isLeftToRight) cycle else (cycle - 1f)
            val waveHead = -0.25f + (passProgress * 1.55f)
            val horizStep = if (isLeftToRight) col else (columnsCount - 1 - col)
            val tileCoord = (horizStep * 0.16f) + ((row % 7) * 0.11f)
            val power = (1f - (abs(waveHead - tileCoord) / 0.24f)).coerceIn(0f, 1f) * shimmerIntensity
            if (power > 0.08f) (0.35f + power * 0.65f).coerceIn(0f, 1f) else 0f
        }
        else -> 0f
    }
}

private fun DrawScope.drawBookTileShimmer(
    shimmerStyle: String,
    shimmerProgress: Float,
    shimmerIntensity: Float,
    index: Int,
    row: Int,
    col: Int,
    columnsCount: Int,
    isPressed: Boolean,
) {
    if (isPressed) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFBD38D).copy(alpha = 0.22f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                radius = size.width * 0.75f,
            )
        )
    }
    if (shimmerStyle == "none" || shimmerIntensity <= 0.01f) return

    when (shimmerStyle) {
        "soft_gold_sheen" -> {
            val cycle = (shimmerProgress + col * 0.08f + (row % 7) * 0.12f) % 1f
            if (cycle < 0.36f) {
                val p = cycle / 0.36f
                val env = sin(p * PI).toFloat()
                val alpha = (0.20f * env * shimmerIntensity).coerceIn(0f, 0.45f)
                val cx = (p * 1.7f - 0.35f) * size.width
                val bandWidth = 34.dp.toPx()
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFBD38D).copy(alpha = alpha),
                            Color.Transparent,
                        ),
                        start = Offset(cx - bandWidth, 0f),
                        end = Offset(cx + bandWidth, size.height),
                    )
                )
            }
        }
        "candle_breathe" -> {
            val wave = ((sin((shimmerProgress * 2.0 * PI) + index * 0.65) + 1.0) * 0.5).toFloat()
            val alpha = ((0.04f + wave * 0.12f * shimmerIntensity)).coerceIn(0f, 0.28f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFBD38D).copy(alpha = alpha),
                        Color.Transparent,
                    )
                )
            )
        }
        "row_wave" -> {
            val rowPos = shimmerProgress * 9.5f - (row % 8) * 0.72f
            if (rowPos > 0f && rowPos < 1.2f) {
                val p = rowPos / 1.2f
                val env = sin(p * PI).toFloat()
                val alpha = (0.17f * env * shimmerIntensity).coerceIn(0f, 0.35f)
                val ySweep = p * size.height
                val band = 24.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFBD38D).copy(alpha = alpha),
                            Color.Transparent,
                        ),
                        startY = ySweep - band,
                        endY = ySweep + band,
                    )
                )
            }
        }
        "horizon_pulse" -> {
            val cycle = (shimmerProgress + index * 0.11f) % 1f
            if (cycle < 0.28f) {
                val p = cycle / 0.28f
                val env = sin(p * PI).toFloat()
                val alpha = (0.42f * env * shimmerIntensity).coerceIn(0f, 0.65f)
                val lx = p * size.width
                val halfSpan = 26.dp.toPx()
                val barHeight = 2.4.dp.toPx()
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFBD38D).copy(alpha = alpha),
                            Color.Transparent,
                        ),
                        startX = lx - halfSpan,
                        endX = lx + halfSpan,
                    ),
                    topLeft = Offset(0f, size.height - barHeight),
                    size = Size(size.width, barHeight),
                )
            }
        }
        "electric" -> {
            val cycle = shimmerProgress * 2f
            val isLeftToRight = cycle < 1f
            val passProgress = if (isLeftToRight) cycle else (cycle - 1f)
            val waveHead = -0.25f + (passProgress * 1.55f)
            val horizStep = if (isLeftToRight) col else (columnsCount - 1 - col)
            val tileCoord = (horizStep * 0.16f) + ((row % 7) * 0.11f)
            val boltPower = (1f - (abs(waveHead - tileCoord) / 0.24f)).coerceIn(0f, 1f) * shimmerIntensity
            if (boltPower > 0.04f) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF90CDF4).copy(alpha = (0.28f * boltPower).coerceIn(0f, 0.6f)),
                            Color(0xFFFBD38D).copy(alpha = (0.36f * boltPower).coerceIn(0f, 0.7f)),
                            Color.Transparent,
                        ),
                        start = if (isLeftToRight) Offset.Zero else Offset(size.width, 0f),
                        end = if (isLeftToRight) Offset(size.width, size.height) else Offset(0f, size.height),
                    )
                )
                val w = size.width
                val h = size.height
                val jitter = if ((index % 2) == 0) 0.14f else -0.14f
                val boltPath = Path().apply {
                    if (isLeftToRight) {
                        moveTo(w * 0.04f, h * 0.10f)
                        lineTo(w * 0.34f, h * (0.44f + jitter))
                        lineTo(w * 0.46f, h * (0.28f - jitter))
                        lineTo(w * 0.70f, h * (0.68f + jitter))
                        lineTo(w * 0.96f, h * 0.92f)
                    } else {
                        moveTo(w * 0.96f, h * 0.10f)
                        lineTo(w * 0.66f, h * (0.44f + jitter))
                        lineTo(w * 0.54f, h * (0.28f - jitter))
                        lineTo(w * 0.30f, h * (0.68f + jitter))
                        lineTo(w * 0.04f, h * 0.92f)
                    }
                }
                drawPath(
                    path = boltPath,
                    color = Color(0xFFFBD38D).copy(alpha = (0.55f * boltPower).coerceIn(0f, 0.9f)),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                drawPath(
                    path = boltPath,
                    color = Color.White.copy(alpha = (0.92f * boltPower).coerceIn(0f, 1f)),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

private fun openShortScriptureChapter(shortItem: BiblicalShortDto) {
    val book = BibleState.bibleBooks.data?.find {
        it.bookId.equals(shortItem.bookUsfm, ignoreCase = true)
    } ?: BookData(
        name = shortItem.scriptureReference.substringBeforeLast(" "),
        chapters = listOf(Chapter(bookId = "${shortItem.bookUsfm}.intro")) +
            (1..shortItem.chapter.coerceAtLeast(1)).map { ch ->
                Chapter(
                    name = "${shortItem.bookUsfm}.$ch",
                    bookId = shortItem.bookUsfm,
                    number = ch.toString(),
                    position = ch,
                )
            },
    )
    AnalyticsTracker.trackBookSelected(
        bookId = shortItem.bookUsfm,
        bookName = book.name ?: shortItem.bookUsfm,
        source = "biblical_short",
    )
    BibleState.closeBiblicalShorts()
    BibleState.resumeShortIdOnReturn = shortItem.id
    BibleState.returnToShortsOnBack = true
    BibleState.restoreReadingLocation(book, shortItem.chapter)
    val verseRange = parseVerseRange(shortItem.scriptureReference, shortItem.verse)
    BibleState.focusScriptureVerse(
        bookId = book.bookId,
        chapter = shortItem.chapter,
        verseStart = verseRange.first,
        verseEnd = verseRange.last,
    )
}

private fun openDailyVerseChapter(dailyVerse: DailyVerse) {
    val data = dailyVerse.data
    val targetUsfm = data.bookUsfm.orEmpty()
    val targetBook = BibleState.bibleBooks.data?.let { books ->
        books.find { targetUsfm.isNotBlank() && it.bookId.equals(targetUsfm, ignoreCase = true) }
            ?: books.getOrNull((data.bookNumber - 1).coerceAtLeast(0))
    } ?: BookData(
        name = bibleBooksFull.getOrNull(data.bookNumber - 1) ?: targetUsfm,
        chapters = listOf(Chapter(bookId = "$targetUsfm.intro")) +
            (1..data.chapter.coerceAtLeast(1)).map { ch ->
                Chapter(
                    name = "$targetUsfm.$ch",
                    bookId = targetUsfm,
                    number = ch.toString(),
                    position = ch,
                )
            },
    )
    AnalyticsTracker.trackBookSelected(
        bookId = targetBook.bookId,
        bookName = targetBook.name ?: targetBook.bookId,
        source = "verse_of_the_day",
    )
    BibleState.returnToShortsOnBack = false
    BibleState.restoreReadingLocation(targetBook, data.chapter.coerceAtLeast(1), fromHomeDailyVerse = true)
    BibleState.focusScriptureVerse(
        bookId = targetBook.bookId,
        chapter = data.chapter.coerceAtLeast(1),
        verseStart = data.verse.coerceAtLeast(1),
        verseEnd = data.verse.coerceAtLeast(1),
    )
}
