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
import data.api.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import data.apiBible.Chapter
import data.apiBible.ChapterContent
import data.apiBible.getChapterBibleAPI
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

@Composable
fun ScrollableTabScriptures(
    chapters: ChapterContent,
    chapterListBookData: List<Chapter>?,
    bookDataList: List<BookData>?,
    bibleId: String
) {
    val scope = rememberCoroutineScope()
    var selectedChapter by remember(chapters.data?.bookId) { mutableStateOf<Chapter?>(null) }
    var selectedTabIndex by remember(chapterListBookData) { mutableStateOf(0) }
    Napier.d(
        "selectedTabIndex $selectedTabIndex :: bookId ${chapters.data?.bookId} " +
                ":: number ${chapters.data?.number}", tag = "BB2454"
    )
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
                                Napier.v(
                                    "ScrollableTabScriptures :: updateSelectedChapter: $chapter",
                                    tag = "BB2452"
                                )
                                scope.launch {
                                    Napier.i("scope.launch start: $chapterString")
                                    getChapterBibleAPI(
                                        chapterNumber = chapterString,
                                        bibleId = bibleId
                                    )
                                    Napier.i("scope.launch finished")
                                }
                            },
                            text = { Text(chapter.number) },
                        )
                    }
                }
            }

            BibleScriptures(chapters, bookDataList)
        }
    }
}

@Composable
private fun BibleScriptures(chapters: ChapterContent, bookDataList: List<BookData>?) {
    val bookData = bookDataList?.find { it.bookId == chapters.data?.bookId }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        chapters.data?.let {
            Text(
                text = "${bookData?.name} ${it.cleanedContent}",
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}