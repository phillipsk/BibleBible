
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
import data.apiBible.ChapterContent
import data.apiBible.getChapterBibleAPI
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import ui.BibleScriptures

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BibleScripturesPager(
    chapters: ChapterContent,
    chapterListBookData: List<Chapter>?,
    bibleId: String
) {
    val scope = rememberCoroutineScope()
    val pagerColumnScrollState = rememberScrollState()
    var selectedChapter by remember(chapters.data?.bookId) { mutableStateOf<Chapter?>(null) }
    var selectedTabIndex by remember() { mutableStateOf(0) }
    Napier.v("selectedTabIndex: $selectedTabIndex", tag = "BB2460")
    val pagerState = rememberPagerState(0, 0f) {
        chapterListBookData?.size ?: 0
    }

    LaunchedEffect(pagerState.currentPage) {
        val currentPage = pagerState.currentPage
        Napier.v("LaunchedEffect: currentPage: $currentPage", tag = "BB2460")
        val chapterString = chapterListBookData?.getOrNull(currentPage)?.let {
            it.bookId + "." + it.number
        }
        scope.launch {
            chapterString?.let {
                getChapterBibleAPI(
                    chapterNumber = it,
                    bibleId = bibleId
                )
                pagerState.animateScrollToPage(currentPage)
                pagerColumnScrollState.scrollTo(0) // Scroll to the top
            }
        }
    }

    LaunchedEffect(selectedTabIndex) {
        Napier.v("LaunchedEffect: selectedTabIndex: $selectedTabIndex", tag = "BB2460")
        if (selectedTabIndex != pagerState.currentPage) {
            scope.launch {
                pagerState.animateScrollToPage(selectedTabIndex)
            }
        }
    }

    AnimatedVisibility(
        visible = chapters.data?.cleanedContent != null,
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
                chapterListBookData?.forEachIndexed { index, chapter ->
                    if (!chapter.number.isNullOrEmpty()) {
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                Napier.v("Tab onClick: $index", tag = "BB2460")
                                scope.launch {
                                    selectedTabIndex = index
                                }
//                                selectedChapter = chapter
                            },
                            text = { Text(chapter.number) }
                        )
                    }
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