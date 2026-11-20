package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiModel
import data.analytics.AnalyticsTracker
import data.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import data.apiBible.Chapter
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.DailyVerse
import data.bibleIQ.bibleBooksFull
import data.config.CloudSessionManager
import data.shorts.BiblicalShortDto
import email.kevinphillips.biblebible.PlatformBackHandler
import email.kevinphillips.biblebible.getPlatformName
import io.github.aakira.napier.Napier
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ui.configs.BibleBibleTopBar
import ui.configs.BottomSheetConfigView
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
    val channel = remember { BibleIQDataModel.snackBarChannel }

    LaunchedEffect(BibleIQDataModel.showHomePage) {
        localScaffoldState.bottomSheetState.collapse()
    }

    LaunchedEffect(
        BibleIQDataModel.showHomePage,
        BibleIQDataModel.showBiblicalShorts,
    ) {
        val activeScreen = when {
            BibleIQDataModel.showBiblicalShorts -> "biblical_shorts"
            BibleIQDataModel.showHomePage -> "home"
            else -> "scripture_reader"
        }
        AnalyticsTracker.trackScreenView(activeScreen)
    }

    LaunchedEffect(
        BibleIQDataModel.autoDemoTourEnabled,
        BibleIQDataModel.showBiblicalShorts,
    ) {
        if (BibleIQDataModel.autoDemoTourEnabled && !BibleIQDataModel.showBiblicalShorts) {
            BibleIQDataModel.showHomePage = true
            delay(3500L)
            if (BibleIQDataModel.autoDemoTourEnabled) {
                BibleIQDataModel.openBiblicalShorts()
            }
        }
    }

    LaunchedEffect(Unit) {
        channel.receiveAsFlow().collect { errorMsg ->
            localScaffoldState.snackbarHostState.showSnackbar(message = errorMsg)
        }
    }

    val isBottomSheetExpanded = localScaffoldState.bottomSheetState.isExpanded
    val canNavigateBack = isBottomSheetExpanded ||
        BibleIQDataModel.showBiblicalShorts ||
        !BibleIQDataModel.showHomePage

    val handleBackNavigation: () -> Unit = {
        when {
            isBottomSheetExpanded -> scope.launch {
                localScaffoldState.bottomSheetState.collapse()
            }
            BibleIQDataModel.showBiblicalShorts -> {
                BibleIQDataModel.closeBiblicalShorts()
            }
            !BibleIQDataModel.showHomePage -> {
                BibleIQDataModel.navigateBackFromReader()
            }
        }
    }

    PlatformBackHandler(enabled = canNavigateBack, onBack = handleBackNavigation)

    // iOS native-style left-edge swipe-right back gesture
    val iosEdgeBackModifier = if (getPlatformName() == "ios" && canNavigateBack) {
        Modifier.pointerInput(canNavigateBack) {
            var startedAtLeftEdge = false
            var accumulatedDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { startOffset ->
                    startedAtLeftEdge = startOffset.x <= 32.dp.toPx()
                    accumulatedDrag = 0f
                },
                onHorizontalDrag = { _, dragAmount ->
                    if (startedAtLeftEdge) {
                        accumulatedDrag += dragAmount
                        if (accumulatedDrag > 64.dp.toPx()) {
                            startedAtLeftEdge = false
                            accumulatedDrag = 0f
                            handleBackNavigation()
                        }
                    }
                },
            )
        }
    } else {
        Modifier
    }

    BottomSheetScaffold(
        snackbarHost = {
            CustomSnackbarHost(snackbarHostState = scaffoldState.snackbarHostState)
        },
        scaffoldState = scaffoldState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetPeekHeight = 0.dp,
        sheetContent = {
            BottomSheetConfigView(
                bibleVersionsList = BibleIQDataModel.bibleVersions,
                showAISummary = GeminiModel.showSummary,
                bottomSheetState = localScaffoldState.bottomSheetState
            )
        },
        content = ({
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .then(iosEdgeBackModifier),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!BibleIQDataModel.showBiblicalShorts) {
                    BibleBibleTopBar(
                        onClick = { BibleIQDataModel.navigateBackFromReader() },
                        showBottomSheet = {
                            scope.launch {
                                if (localScaffoldState.bottomSheetState.isExpanded) {
                                    AnalyticsTracker.track(
                                        "bottom_sheet_toggled",
                                        "navigation",
                                        mapOf("state" to "collapsed"),
                                    )
                                    localScaffoldState.bottomSheetState.collapse()
                                } else {
                                    Napier.v("BibleAPIDataModel.readingHistory :: count ${BibleAPIDataModel.readingHistory?.size} :: dataClass ${BibleAPIDataModel.readingHistory?.take(100)}", tag = "RH1283")
                                    AnalyticsTracker.track(
                                        "bottom_sheet_toggled",
                                        "navigation",
                                        mapOf("state" to "expanded"),
                                    )
                                    localScaffoldState.bottomSheetState.expand()
                                }
                            }
                        }
                    )
                }
                if (BibleIQDataModel.showBiblicalShorts) {
                    BiblicalShortsScreen(
                        onBack = { BibleIQDataModel.closeBiblicalShorts() },
                        onReadChapter = { shortItem -> openShortScriptureChapter(shortItem) },
                    )
                } else if (BibleIQDataModel.showHomePage) {
                    BibleBookList(
                        bookData = BibleAPIDataModel.uiBooks.data,
                        header = { isWideLayout ->
                            val showVotd = CloudSessionManager.verseOfTheDayEnabled
                            val showShorts = CloudSessionManager.biblicalShortsEnabled
                            if (isWideLayout && showVotd && showShorts) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                ) {
                                    VerseOfTheDay(
                                        version = BibleIQDataModel.selectedVersion,
                                        modifier = Modifier
                                            .weight(1.15f)
                                            .fillMaxHeight(),
                                        onVerseClick = { dailyVerse -> openDailyVerseChapter(dailyVerse) },
                                    )
                                    BiblicalShortsEntryCard(
                                        onOpenShorts = { BibleIQDataModel.openBiblicalShorts() },
                                        modifier = Modifier
                                            .weight(0.85f)
                                            .fillMaxHeight(),
                                    )
                                }
                            } else {
                                Column {
                                    if (showVotd) {
                                        VerseOfTheDay(
                                            version = BibleIQDataModel.selectedVersion,
                                            onVerseClick = { dailyVerse -> openDailyVerseChapter(dailyVerse) },
                                        )
                                    }
                                    if (showShorts) {
                                        BiblicalShortsEntryCard(
                                            onOpenShorts = { BibleIQDataModel.openBiblicalShorts() }
                                        )
                                    }
                                }
                            }
                        },
                    )
                } else {
                    BibleIQDataModel.bibleChapter?.let { it1 ->
                        BibleScripturesPager(
                            chapters = it1,
                            bibleVersion = BibleIQDataModel.selectedVersion,
                            selectedBook = BibleIQDataModel.selectedBook,
                            isAISummaryLoading = GeminiModel.isLoading,
                            showAISummary = GeminiModel.showSummary,
                            bottomSheetScaffoldState = localScaffoldState
                        )
                    }
                }
            }
        })
    )
}

@Composable
internal fun BibleBookList(
    bookData: List<BookData>?,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    header: (@Composable (isWideLayout: Boolean) -> Unit)? = null,
) {
    // Unified 2-phase electrifying diagonal lightning cycle:
    // Phase 0..1: crackles diagonally down-right starting from Genesis (top-left)
    // Phase 1..2: crackles diagonally down-left starting from Leviticus (top-right)
    val infiniteTransition = rememberInfiniteTransition(label = "living_word_lightning")
    val lightningCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lightning_cycle",
    )
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    AnimatedVisibility(
        visible = BibleIQDataModel.showHomePage,
        enter = fadeIn(initialAlpha = 0.4f),
        exit = fadeOut(animationSpec = tween(durationMillis = 250))
    ) {
        bookData?.let { bookDataList ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideLayout = maxWidth >= 560.dp
                val columnsCount = if (isWideLayout) 5 else 3
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Fixed(columnsCount),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp,
                        bottom = 20.dp + navBarBottomPadding,
                    ),
                    userScrollEnabled = true,
                ) {
                    if (header != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) { header(isWideLayout) }
                        item(span = { GridItemSpan(maxLineSpan) }) {
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
                    }
                    itemsIndexed(items = bookDataList) { index, book ->
                        val row = index / columnsCount
                        val col = index % columnsCount
                        // Composite the book tint solidly over the deep background so there is NEVER any translucent shadow box artifact
                        val rawColor = BibleAPIDataModel.getBibleBookColor(
                            BibleIQDataModel.getAPIBibleOrdinal(book.bookId)
                        )
                        val solidBase = rawColor.compositeOver(Color(0xFF1A2838))
                        val tileBrush = Brush.verticalGradient(
                            colors = listOf(
                                solidBase.copy(alpha = 1f),
                                Color(0x1809111E).compositeOver(solidBase),
                            )
                        )
                        // Phase 1 (0..1): Genesis -> down-right diagonal; Phase 2 (1..2): Leviticus -> down-left diagonal
                        val isLeftToRight = lightningCycle < 1f
                        val passProgress = if (isLeftToRight) lightningCycle else (lightningCycle - 1f)
                        val waveHead = -0.25f + (passProgress * 1.55f)
                        val horizStep = if (isLeftToRight) col else (columnsCount - 1 - col)
                        val tileCoord = (horizStep * 0.16f) + ((row % 7) * 0.11f)
                        val distance = abs(waveHead - tileCoord)
                        val boltPower = (1f - (distance / 0.24f)).coerceIn(0f, 1f)

                        val tileShape = RoundedCornerShape(14.dp)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth()
                                .defaultMinSize(minWidth = 100.dp, minHeight = 46.dp)
                                .clip(tileShape)
                                .background(tileBrush)
                                .border(
                                    width = if (boltPower > 0.2f) 2.dp else 1.dp,
                                    color = if (boltPower > 0.08f) {
                                        Color(0xFFFBD38D).copy(alpha = (0.35f + boltPower * 0.65f).coerceIn(0f, 1f))
                                    } else {
                                        Color(0x36FFFFFF)
                                    },
                                    shape = tileShape,
                                )
                                .drawWithContent {
                                    drawContent()
                                    if (boltPower > 0.04f) {
                                        // Soft electric surface flash
                                        drawRect(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xFF90CDF4).copy(alpha = 0.28f * boltPower),
                                                    Color(0xFFFBD38D).copy(alpha = 0.36f * boltPower),
                                                    Color.Transparent,
                                                ),
                                                start = if (isLeftToRight) Offset.Zero else Offset(size.width, 0f),
                                                end = if (isLeftToRight) Offset(size.width, size.height) else Offset(0f, size.height),
                                            )
                                        )
                                        // Crisp jagged electric lightning arc crackling diagonally across the tile
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
                                        // Outer warm-gold / sky-blue lightning aura
                                        drawPath(
                                            path = boltPath,
                                            color = Color(0xFFFBD38D).copy(alpha = 0.55f * boltPower),
                                            style = Stroke(
                                                width = 5.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round,
                                            ),
                                        )
                                        // Bright white electrifying core filament
                                        drawPath(
                                            path = boltPath,
                                            color = Color.White.copy(alpha = 0.92f * boltPower),
                                            style = Stroke(
                                                width = 1.6.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round,
                                            ),
                                        )
                                    }
                                }
                                .clickable {
                                    AnalyticsTracker.trackBookSelected(
                                        bookId = book.bookId,
                                        bookName = book.name ?: book.cleanedName ?: book.bookId,
                                        source = "home_grid",
                                    )
                                    BibleIQDataModel.returnToShortsOnBack = false
                                    BibleAPIDataModel.run {
                                        updateBookData(book)
                                        updateSelectedChapter(book.remoteKey)
                                    }
                                    BibleIQDataModel.updateSelectedBook(book)
                                    BibleIQDataModel.showHomePage = false
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
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openShortScriptureChapter(shortItem: BiblicalShortDto) {
    val book = BibleAPIDataModel.bibleBooks.data?.find {
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
    BibleIQDataModel.closeBiblicalShorts()
    BibleIQDataModel.returnToShortsOnBack = true
    BibleIQDataModel.restoreReadingLocation(book, shortItem.chapter)
}

private fun openDailyVerseChapter(dailyVerse: DailyVerse) {
    val data = dailyVerse.data
    val targetUsfm = data.bookUsfm.orEmpty()
    val targetBook = BibleAPIDataModel.bibleBooks.data?.let { books ->
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
    BibleIQDataModel.returnToShortsOnBack = false
    BibleIQDataModel.restoreReadingLocation(targetBook, data.chapter.coerceAtLeast(1))
}
