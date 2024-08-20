package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.apiBible.BookData
import data.bibleIQ.BibleChapterUIState
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.getChapterBibleIQ
import email.kevinphillips.biblebible.isDesktopPlatform
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
internal fun BibleScripturesPager(
    chapters: BibleChapterUIState,
    bibleVersion: String,
    selectedBook: BookData,
    isAISummaryLoading: Boolean,
    showAISummary: Boolean,
    bottomSheetScaffoldState: BottomSheetScaffoldState,
) {

    var initialLoadDone by remember { mutableStateOf(false) }
    val pagerColumnScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember(selectedBook) { mutableStateOf(0) }
    Napier.v(
        "params :: bookId ${chapters.bookId} :: chapterListBookData?.size ${chapters.chapterList?.size} " +
                " :: selectedTabIndex $selectedTabIndex", tag = "BB2460"
    )
    val pagerState = rememberPagerState(0, 0f) {
        chapters.chapterList?.size ?: 0
    }
    var isPageChangeFromTabClick by remember { mutableStateOf(false) }
    var lastTabClickTime by remember { mutableStateOf(0L) }
    val debounceDuration = 50L  // 300 ms for debounce duration
    val uiStateReady =
        BibleIQDataModel.getAPIBibleOrdinal(BibleIQDataModel.selectedBook.remoteKey) == BibleIQDataModel.bibleChapter?.bookId

    LaunchedEffect(selectedBook) {
//    TODO: calling and updating BibleChapterUIState on selectedBook change
        Napier.v(
            "LaunchedEffect: selectedBook: $bibleVersion ${selectedBook.bookId} ${chapters.bookId}",
            tag = "BB2470"
        )
        initialLoadDone = true
        getChapterBibleIQ(book = selectedBook, chapter = selectedTabIndex + 1)
        pagerState.scrollToPage(0)
        selectedTabIndex = 0
        initialLoadDone = false
        Napier.v(
            "LaunchedEffect: selectedBook :: canScrollBackward: ${pagerColumnScrollState.canScrollBackward} :: initialLoadDone: $initialLoadDone",
            tag = "BB2470"
        )
        BibleIQDataModel.bottomSheetViewCount = 0
    }

    LaunchedEffect(bibleVersion) {
        Napier.v(
            "LaunchedEffect: bibleVersion: $bibleVersion ${selectedBook.bookId} ${chapters.bookId}",
            tag = "BB2460"
        )
        if (uiStateReady) {
            getChapterBibleIQ(book = selectedBook, chapter = selectedTabIndex + 1, updateReadingHistory = false)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        Napier.v(
            "LaunchedEffect: currentPage: ${pagerState.currentPage} initialLoadDone $initialLoadDone",
            tag = "BB2470"
        )
        val currentTime = Clock.System.now().toEpochMilliseconds()
        if (isPageChangeFromTabClick) {
            val time = currentTime - lastTabClickTime
            Napier.v("debounceDuration: time: $time", tag = "debounceDuration")
            if (time > debounceDuration) {
                selectedTabIndex = pagerState.currentPage
                getChapterBibleIQ(book = selectedBook, chapter = selectedTabIndex + 1)
            }
            isPageChangeFromTabClick = false
//            bottomSheetScaffoldState.bottomSheetState.expand()
        } else if (!initialLoadDone) {
            selectedTabIndex = pagerState.currentPage
            getChapterBibleIQ(book = selectedBook, chapter = selectedTabIndex + 1)
//            bottomSheetScaffoldState.bottomSheetState.collapse()
        }
        bottomSheetScaffoldState.bottomSheetState.collapse()
        pagerColumnScrollState.scrollTo(0)
//        GeminiModel.showSummary = false // done in API call
    }

    /*    LaunchedEffect(pagerColumnScrollState.canScrollBackward) {
            Napier.v(
                "LaunchedEffect: canScrollBackward: ${pagerColumnScrollState.canScrollBackward} :: initialLoadDone: $initialLoadDone",
                tag = "BB2470"
            )
            if (pagerColumnScrollState.canScrollBackward || showAISummary) {
                bottomSheetScaffoldState.bottomSheetState.collapse()
            } else if (BibleIQDataModel.bottomSheetViewCount < 3) {
                bottomSheetScaffoldState.bottomSheetState.expand()
                BibleIQDataModel.bottomSheetViewCount++
            }
        }*/

    LaunchedEffect(Unit) {
        pagerColumnScrollState.interactionSource.interactions.collect {
            bottomSheetScaffoldState.bottomSheetState.collapse()
        }
    }

    LaunchedEffect(isAISummaryLoading || showAISummary) {
        Napier.v(
            "LaunchedEffect: isAISummaryLoading: $isAISummaryLoading showAISummary: $showAISummary",
            tag = "BB2470"
        )
        bottomSheetScaffoldState.bottomSheetState.collapse()
    }

    if (chapters.bookId != null) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(initialAlpha = 0.4f),
            exit = fadeOut(animationSpec = tween(durationMillis = 150))
        ) {
            Column {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            Box(
                                Modifier
                                    .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                    .padding(horizontal = 16.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50)) // Make it rounded
                                    .background(MaterialTheme.colors.primary)
                            )
                        } else {
                            Napier.e(
                                "Error: tabPositions: $tabPositions out of bounds",
                                tag = "BB2460"
                            )
                        }
                    }
                ) {
                    Napier.d("chapterList: ${chapters.chapterList}", tag = "BB2460")
                    chapters.chapterList?.forEachIndexed { index, e ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                Napier.v("Tab onClick: $index", tag = "BB2460")
                                if (index != selectedTabIndex) {
                                    isPageChangeFromTabClick = true
                                    lastTabClickTime = Clock.System.now().toEpochMilliseconds()
                                    selectedTabIndex = index
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            },
                            text = { Text(e.toString()) }
                        )
                    }
                }

                HorizontalPager(
//                    key = { page -> selectedBook.bookId + page.toString()},
                    state = pagerState,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.weight(1f),
                    userScrollEnabled = !isDesktopPlatform()
                ) { page ->
                    Napier.v(
                        "HorizontalPager: currentPage: ${pagerState.currentPage} + Pager LoadingScreen: $isAISummaryLoading $showAISummary",
                        tag = "Gemini"
                    )
                    when {
                        isAISummaryLoading && showAISummary -> {
                            LoadingScreen()
                        }

                        showAISummary -> {
                            GeminiSummary(
                                pagerColumnScrollState,
                                BibleIQDataModel.selectedFontSize.sp
                            )
                        }

                        else -> {
                            BibleScriptures(
                                chapters,
                                pagerColumnScrollState,
                                BibleIQDataModel.selectedFontSize.sp
                            )
                        }
                    }
                }
            }
        }
    } else {
        LoadingScreen()
    }
}