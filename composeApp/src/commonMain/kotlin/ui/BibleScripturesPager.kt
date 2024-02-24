
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
import data.bibleIQ.BibleIQDataModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import ui.BiblePagerUiState
import ui.BibleScriptures
import ui.LoadingScreen

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BibleScripturesPager(
    biblePagerUiState: BiblePagerUiState,
    bibleVersion: String,
    selectedBook: BookData,
) {
    val scope = rememberCoroutineScope()
    val pagerColumnScrollState = rememberScrollState()
//    var selectedTabIndex by remember(chapters.bookId) { mutableStateOf(0) }
    var selectedTabIndex by remember() { mutableStateOf(0) }

    val pagerState = rememberPagerState(0, 0f) {
        if (biblePagerUiState is BiblePagerUiState.Success) {
            biblePagerUiState.bibleChapterUIState.chapterList?.size ?: 0
        } else 0
    }
    var isPageChangeFromTabClick by remember { mutableStateOf(false) }
    var lastTabClickTime by remember { mutableStateOf(0L) }
    val debounceDuration = 300L  // 300 ms for debounce duration
    var apiCallMade by remember(selectedBook) { mutableStateOf(false) }

    // Fetch chapter content when selectedTabIndex changes
    LaunchedEffect(selectedTabIndex) {
        Napier.v(
            "LaunchedEffect: selectedTabIndex: $bibleVersion $selectedBook $selectedTabIndex",
            tag = "BB2460"
        )
        if (!apiCallMade) {
            val chapter = selectedTabIndex.plus(1)
            scope.launch {
                BibleIQDataModel.fetchBibleChapter(selectedBook, chapter, bibleVersion)
            }
        }
    }

    LaunchedEffect(bibleVersion, selectedBook) {
//        LaunchedEffect(selectedBook) {
        Napier.v(
            "LaunchedEffect: bibleVersion selectedBook :: $bibleVersion $selectedBook $selectedTabIndex",
            tag = "BB2460"
        )
        scope.launch {
            BibleIQDataModel.fetchBibleChapter(selectedBook, 1, bibleVersion)
            apiCallMade = true
            if (selectedTabIndex != 0) {
                selectedTabIndex = 0
                pagerState.animateScrollToPage(0)
            }
            apiCallMade = false
        }
    }
//    LaunchedEffect(bibleVersion) {
//        Napier.v(
//            "LaunchedEffect: bibleVersion selectedBook :: $bibleVersion $selectedBook $selectedTabIndex",
//            tag = "BB2460"
//        )
//        scope.launch {
//            BibleIQDataModel.fetchBibleChapter(selectedBook, 1, bibleVersion)
//            apiCallMade = true
//        }
//    }

    // Sync selectedTabIndex with HorizontalPager's currentPage
    LaunchedEffect(pagerState.currentPage) {
        Napier.v("LaunchedEffect: currentPage: ${pagerState.currentPage}", tag = "BB2460")
        val currentTime = Clock.System.now().toEpochMilliseconds()
        if (!isPageChangeFromTabClick && currentTime - lastTabClickTime > debounceDuration && pagerState.currentPage != selectedTabIndex) {
            selectedTabIndex = pagerState.currentPage
        }
        // Reset the flag after handling the page change
        isPageChangeFromTabClick = false
    }

    when (biblePagerUiState) {
        is BiblePagerUiState.Success -> {
            Napier.v(
                "params :: bookId ${biblePagerUiState.bibleChapterUIState.bookId} :: chapterListBookData?.size ${biblePagerUiState.bibleChapterUIState.chapterList?.size} " +
                        " :: selectedTabIndex $selectedTabIndex", tag = "BB2460"
            )
            AnimatedVisibility(
                visible = biblePagerUiState.bibleChapterUIState.bookId != null,
                enter = fadeIn(initialAlpha = 0.4f),
                exit = fadeOut(animationSpec = tween(durationMillis = 150))
            ) {
                Column {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        edgePadding = 16.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = MaterialTheme.colors.primary
                            )
                        }
                    ) {
                        Napier.d(
                            "chapterList: ${biblePagerUiState.bibleChapterUIState.chapterList}",
                            tag = "BB2460"
                        )
                        biblePagerUiState.bibleChapterUIState.chapterList?.forEachIndexed { index, e ->
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
                        BibleScriptures(
                            biblePagerUiState.bibleChapterUIState,
                            pagerColumnScrollState
                        )
                    }
                }
            }
        }

        is BiblePagerUiState.Loading -> {
            LoadingScreen()
        }

        else -> {}
    }
}