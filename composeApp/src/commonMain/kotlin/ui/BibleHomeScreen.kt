package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiModel
import data.analytics.AnalyticsTracker
import data.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import data.apiBible.Chapter
import data.bibleIQ.BibleIQDataModel
import data.config.CloudSessionManager
import data.shorts.BiblicalShortDto
import email.kevinphillips.biblebible.PlatformBackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import io.github.aakira.napier.Napier
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

    PlatformBackHandler(enabled = canNavigateBack) {
        when {
            isBottomSheetExpanded -> scope.launch {
                localScaffoldState.bottomSheetState.collapse()
            }
            BibleIQDataModel.showBiblicalShorts -> {
                BibleIQDataModel.closeBiblicalShorts()
            }
            !BibleIQDataModel.showHomePage -> {
                BibleIQDataModel.onHomeClick()
            }
        }
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
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!BibleIQDataModel.showBiblicalShorts) {
                    BibleBibleTopBar(
                        onClick = BibleIQDataModel.onHomeClick,
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
                        header = {
                            Column {
                                if (CloudSessionManager.verseOfTheDayEnabled) {
                                    VerseOfTheDay(BibleIQDataModel.selectedVersion)
                                }
                                if (CloudSessionManager.biblicalShortsEnabled) {
                                    BiblicalShortsEntryCard(
                                        onOpenShorts = { BibleIQDataModel.openBiblicalShorts() }
                                    )
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
    header: (@Composable () -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = BibleIQDataModel.showHomePage,
        enter = fadeIn(initialAlpha = 0.4f),
        exit = fadeOut(animationSpec = tween(durationMillis = 250))
    ) {
        bookData?.let { bookDataList ->
            Column(modifier = Modifier.padding(4.dp)) {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(10.dp),
                    userScrollEnabled = true,
                ) {
                    if (header != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) { header() }
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
                    items(items = bookDataList) {
                        it.let {
                            Button(
                                onClick = {
                                    AnalyticsTracker.trackBookSelected(
                                        bookId = it.bookId,
                                        bookName = it.name ?: it.cleanedName ?: it.bookId,
                                        source = "home_grid",
                                    )
                                    BibleAPIDataModel.run {
                                        updateBookData(it)
                                        updateSelectedChapter(it.remoteKey)
                                    }
                                    BibleIQDataModel.updateSelectedBook(it)
                                    BibleIQDataModel.showHomePage = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0x38FFFFFF)),
                                elevation = ButtonDefaults.elevation(
                                    defaultElevation = 3.dp,
                                    pressedElevation = 1.dp,
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = BibleAPIDataModel.getBibleBookColor(
                                        BibleIQDataModel.getAPIBibleOrdinal(it.bookId)
                                    )
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .height(IntrinsicSize.Min)
                                    .defaultMinSize(
                                        minWidth = 100.dp,
                                        minHeight = 44.dp
                                    )
                            ) {
                                it.cleanedName?.let { name ->
                                    Text(
                                        fontFamily = MaterialTheme.typography.h3.fontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        text = name,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colors.onPrimary,
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
    BibleIQDataModel.restoreReadingLocation(book, shortItem.chapter)
}