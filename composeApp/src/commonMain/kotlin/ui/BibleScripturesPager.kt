
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
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
import androidx.compose.ui.unit.dp
import data.apiBible.BookData
import data.bibleIQ.BibleChapterUIState
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.getChapterBibleIQ
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import ui.GeminiSummary
import ui.LoadingScreen

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BibleScripturesPager(
    chapters: BibleChapterUIState,
    bibleVersion: String,
    selectedBook: BookData,
) {
    val scope = rememberCoroutineScope()
    val pagerColumnScrollState = rememberScrollState()
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
    val debounceDuration = 100L  // 300 ms for debounce duration
    var initialLoadDone by remember { mutableStateOf(false) }
    val uiStateReady =
        BibleIQDataModel.getAPIBibleCardinal(BibleIQDataModel.selectedBook.remoteKey) == BibleIQDataModel.bibleChapter?.bookId

    LaunchedEffect(selectedBook) {
//    TODO: calling and updating BibleChapterUIState on selectedBook change
        Napier.v(
            "LaunchedEffect: selectedBook: $bibleVersion ${selectedBook.bookId} ${chapters.bookId}",
            tag = "BB2460"
        )
        initialLoadDone = true
        getChapterBibleIQ(book = selectedBook, chapter = selectedTabIndex + 1)
        pagerState.scrollToPage(0)
        selectedTabIndex = 0
        initialLoadDone = false
    }

    LaunchedEffect(bibleVersion) {
        Napier.v(
            "LaunchedEffect: bibleVersion: $bibleVersion ${selectedBook.bookId} ${chapters.bookId}",
            tag = "BB2460"
        )
        if (uiStateReady) {
            getChapterBibleIQ(book = selectedBook, chapter = selectedTabIndex + 1)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        Napier.v("LaunchedEffect: currentPage: ${pagerState.currentPage}", tag = "BB2460")
        val currentTime = Clock.System.now().toEpochMilliseconds()
        if (isPageChangeFromTabClick) {
            if (currentTime - lastTabClickTime > debounceDuration) {
                selectedTabIndex = pagerState.currentPage
                getChapterBibleIQ(book = selectedBook, chapter = selectedTabIndex + 1)
            }
            isPageChangeFromTabClick = false
        } else if (!initialLoadDone) {
            selectedTabIndex = pagerState.currentPage
            getChapterBibleIQ(book = selectedBook, chapter = selectedTabIndex + 1)
        }
        pagerColumnScrollState.scrollTo(0)
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
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = MaterialTheme.colors.primary
                            )
                        } else {
                            Napier.e("Error: tabPositions: $tabPositions out of bounds", tag = "BB2460")
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
                    state = pagerState,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    Napier.v(
                        "HorizontalPager: currentPage: ${pagerState.currentPage}",
                        tag = "BB2460"
                    )
//                    BibleScriptures(chapters, pagerColumnScrollState)
                    GeminiSummary(pagerColumnScrollState)
                }
            }
        }
    } else {
        LoadingScreen()
    }
}