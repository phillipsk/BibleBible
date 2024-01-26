import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.apiBible.BibleAPIDataModel
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
    val chaptersSize by rememberUpdatedState(chapterListBookData?.size ?: 0)
    var selectedChapter by remember(chapters.data?.bookId) { mutableStateOf<Chapter?>(null) }
    var selectedTabIndex by remember(chapterListBookData) { mutableStateOf(0) }
    // when recomposing with a different chapterListBookData, the pagerState.currentPage is not updated
    Napier.v("selectedTabIndex: $selectedTabIndex", tag = "BB2452")
    val pagerState = rememberPagerState(0, 0f) {
        chapterListBookData?.size ?: 0
    }
    Napier.v("pagerState.currentPage: ${pagerState.currentPage}", tag = "BB2452")

    LaunchedEffect(key1 = chapters.data?.bookId) {
        selectedTabIndex = 0  // Reset to the first tab
        pagerState.scrollToPage(0)  // Reset the pager to the first page
        Napier.v("LaunchedEffect: selectedTabIndex: $selectedTabIndex", tag = "BB2452")
    }
    LaunchedEffect(selectedTabIndex) {
        Napier.v("LaunchedEffect: selectedTabIndex key: $selectedTabIndex", tag = "BB2452")
    }
    LaunchedEffect(key1 = pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
        selectedChapter = chapterListBookData?.getOrNull(selectedTabIndex)
        // When currentPage changes, load the chapter content
        selectedChapter?.let {
            val chapterString = it.bookId + "." + it.number
            scope.launch {
                getChapterBibleAPI(
                    chapterNumber = chapterString,
                    bibleId = bibleId
                )
            }
        }
    }
    /*    LaunchedEffect(pagerState.currentPage) {
            Napier.v("LaunchedEffect: pagerState.currentPage: ${pagerState.currentPage}", tag = "BB2452")
            selectedTabIndex = pagerState.currentPage
            selectedChapter = chapterListBookData?.getOrNull(selectedTabIndex)
            selectedChapter?.let {
                val chapterString = it.bookId + "." + it.number
                scope.launch {
                    getChapterBibleAPI(
                        chapterNumber = chapterString,
                        bibleId = bibleId
                    )
                }
            }
        }*/

    AnimatedVisibility(
        visible = chapters.data?.cleanedContent != null,
        enter = fadeIn(initialAlpha = 0.4f),
        exit = fadeOut(animationSpec = tween(durationMillis = 250))
    ) {
        Column {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                chapterListBookData?.forEachIndexed { index, chapter ->
                    if (!chapter.number.isNullOrEmpty()) {
                        val chapterString = chapter.bookId + "." + chapter.number
                        Tab(
                            selected = selectedTabIndex == index,
//                            selected = pagerState.currentPage == index,
                            onClick = {
                                Napier.v("Tab onClick: $index", tag = "BB2452")
                                selectedChapter = chapter
                                selectedTabIndex = index
                                BibleAPIDataModel.updateSelectedChapter(chapterString)
                                scope.launch {
                                    getChapterBibleAPI(
                                        chapterNumber = chapterString,
                                        bibleId = bibleId
                                    )
//                                    pagerState.animateScrollToPage(selectedTabIndex)
//                                    selectedTabIndex = pagerState.currentPage
//                                    selectedTabIndex = index
//                                    pagerState.scrollToPage(selectedTabIndex)  // Reset the pager to the first page
                                }
                            },
                            text = { Text(chapter.number) }
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
//                selectedTabIndex = pagerState.currentPage
                Napier.v("HorizontalPager: selectedTabIndex: $selectedTabIndex", tag = "BB2452")
                BibleScriptures(chapters)
            }
        }
    }
}