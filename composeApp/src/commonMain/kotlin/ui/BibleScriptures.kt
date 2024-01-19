package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.apiBible.BibleAPIDataModel
import data.apiBible.Chapter
import data.apiBible.ChapterContent
import data.apiBible.getChapterBibleAPI
import kotlinx.coroutines.launch

@Composable
internal fun ScrollableTabScriptures(
    chapters: ChapterContent,
    chapterListBookData: List<Chapter>?,
    bibleId: String
) {
    val scope = rememberCoroutineScope()
    var selectedChapter by remember(chapters.data?.bookId) { mutableStateOf<Chapter?>(null) }
    var selectedTabIndex by remember(chapterListBookData) { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    LaunchedEffect(key1 = chapters.data?.bookId) {
        scrollState.scrollTo(0)
    }

    AnimatedVisibility(chapters.data?.cleanedContent != null) {
        Column {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colors.primary
                    )
                }
            ) {
                chapterListBookData?.forEachIndexed { index, chapter ->
                    if (!chapter.number.isNullOrEmpty()) {
                        val chapterString = chapter.bookId + "." + chapter.number
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedChapter = chapter
                                selectedTabIndex = index
                                BibleAPIDataModel.updateSelectedChapter(chapterString)
                                scope.launch {
                                    getChapterBibleAPI(
                                        chapterNumber = chapterString,
                                        bibleId = bibleId
                                    )
                                    scrollState.scrollTo(0) // Scroll to the top
                                }
                            },
                            text = { Text(chapter.number) },
                        )
                    }
                }
            }

            BibleScriptures(chapters, scrollState)
        }
    }
}

@Composable
private fun BibleScriptures(chapters: ChapterContent, scrollState: ScrollState) {
    Column(modifier = Modifier.verticalScroll(scrollState)) {
        chapters.data?.let {
            Text(
                text = it.uiContent,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}