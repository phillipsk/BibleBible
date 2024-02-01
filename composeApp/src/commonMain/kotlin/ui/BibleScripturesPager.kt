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
import data.apiBible.Chapter
import data.bibleIQ.BibleChapterUIState
import data.bibleIQ.getChapterBibleIQ
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import ui.BibleScriptures

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BibleScripturesPager(
    chapters: BibleChapterUIState,
    bibleId: String
) {
    val scope = rememberCoroutineScope()
    val pagerColumnScrollState = rememberScrollState()
    var selectedChapter by remember(chapters.bookId) { mutableStateOf<Chapter?>(null) }
    var selectedTabIndex by remember() { mutableStateOf(0) }
    Napier.v(
        "params :: bookId ${chapters.bookId} :: chapterListBookData?.size ${chapters.chapterList?.size} " +
                " :: selectedTabIndex $selectedTabIndex", tag = "BB2460"
    )
    val pagerState = rememberPagerState(0, 0f) {
        chapters.chapterList?.size ?: 0
    }
    var isPageChangeFromTabClick by remember { mutableStateOf(false) }
    var lastTabClickTime by remember { mutableStateOf(0L) }
    val debounceDuration = 300L  // 300 ms for debounce duration

    // Fetch chapter content when selectedTabIndex changes
    LaunchedEffect(selectedTabIndex) {
        Napier.v("LaunchedEffect: selectedTabIndex: $selectedTabIndex", tag = "BB2460")
        Napier.v("Fetching chapter: $selectedTabIndex", tag = "BB2460")
        val chapter = selectedTabIndex.plus(1)
            scope.launch {
                getChapterBibleIQ(book = chapters.bookId, chapter = chapter)
                pagerColumnScrollState.scrollTo(0) // Scroll to the top
            }
    }
    LaunchedEffect(chapters.bookId) {
        Napier.v("LaunchedEffect: bookId: ${chapters.bookId}", tag = "BB2460")
        selectedTabIndex = 0
        pagerState.animateScrollToPage(0)
    }

    // Sync selectedTabIndex with HorizontalPager's currentPage
    LaunchedEffect(pagerState.currentPage) {
        Napier.v("LaunchedEffect: currentPage: ${pagerState.currentPage}", tag = "BB2460")
        val currentTime = Clock.System.now().toEpochMilliseconds()
        if (!isPageChangeFromTabClick && currentTime - lastTabClickTime > debounceDuration && pagerState.currentPage != selectedTabIndex) {
            Napier.v("LaunchedEffect: currentPage: ${pagerState.currentPage}", tag = "BB2460")
            selectedTabIndex = pagerState.currentPage
        }
        // Reset the flag after handling the page change
        isPageChangeFromTabClick = false
    }

    AnimatedVisibility(
        visible = chapters.text != null,
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
                Napier.v("HorizontalPager: currentPage: ${pagerState.currentPage}", tag = "BB2460")
                BibleScriptures(chapters, pagerColumnScrollState)
            }
        }
    }
}