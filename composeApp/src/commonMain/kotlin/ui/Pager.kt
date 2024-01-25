import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import data.apiBible.Chapter
import data.apiBible.ChapterContent
import data.apiBible.getChapterBibleAPI
import kotlinx.coroutines.launch
import ui.BibleScriptures

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ScripturesPager(
    chapters: ChapterContent,
    chapterListBookData: List<Chapter>?,
    bibleId: String
) {
    val scope = rememberCoroutineScope()
    var selectedChapter by remember { mutableStateOf<Chapter?>(null) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) {
        chapterListBookData?.size ?: 0
    }

    LaunchedEffect(key1 = pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
        selectedChapter = chapterListBookData?.getOrNull(pagerState.currentPage)
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
    AnimatedVisibility(
        visible = chapters.data?.cleanedContent != null,
        enter = fadeIn(initialAlpha = 0.4f),
        exit = fadeOut(animationSpec = tween(durationMillis = 250))
    ) {
        Column {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                chapterListBookData?.forEachIndexed { index, chapter ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { /* Pager will handle page changes */ },
                        text = { chapter.number?.let { Text(it) } }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                BibleScriptures(chapters)
            }
        }
    }
}