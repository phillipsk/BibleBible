package ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.api.bible.ChapterContent
import data.api.bible.getChapterBibleAPI
import data.bibleIQ.BibleIQ
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

@Composable
private fun BibleScriptures() {
    val chapters: ChapterContent = BibleIQ.chapter.value
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        chapters.data?.cleanedContent?.let {
            Text(
                it,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
fun ScrollableTabScriptures() {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    var selectedTabIndex by remember {
        mutableStateOf(0)
    }
    val chapters: ChapterContent = BibleIQ.chapter.value
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
                BibleIQ.selectedBookData.value.chapters?.forEachIndexed { index, chapter ->
                    if (!chapter.number.isNullOrEmpty()) {
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                BibleIQ.updateSelectedChapter(chapter.number)
                                scope.launch {
                                    getChapterBibleAPI()
                                }
                            },
                            text = { Text(chapter.number) },
//                    icon = { Icon(imageVector = Icons.Rounded.Home, contentDescription = null) }
                        )
                    }
                }
            }

            BibleScriptures()
        }
    }
}