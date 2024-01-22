package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import data.apiBible.getChapterBibleAPI
import data.bibleIQ.BibleVersion
import kotlinx.coroutines.launch

val homeUiState get() = HomeUiState()

internal fun updateAbbreviation(abv: String) {
    homeUiState.version = BibleVersion(abbreviation = abv)
}

@Composable
internal fun BibleHomeScreen() {
    Scaffold(topBar = { HomeTopBar(onClick = { BibleAPIDataModel.onHomeClick() }) }) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BibleBookList(
                bookData = BibleAPIDataModel.books.data,
                selectedChapter = BibleAPIDataModel.selectedChapter,
                bibleId = BibleAPIDataModel.selectedBibleId
            )
            ScrollableTabScriptures(
                chapters = BibleAPIDataModel.chapterContent,
                chapterListBookData = BibleAPIDataModel.selectedBookData.chapterListBookData,
                bibleId = BibleAPIDataModel.selectedBibleId
            )
        }
    }
}

@Composable
internal fun BibleBookList(bookData: List<BookData>?, selectedChapter: String, bibleId: String) {
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = !bookData.isNullOrEmpty() && selectedChapter == "",
        enter = slideInVertically(initialOffsetY = { -40 }),
        exit = slideOutVertically(targetOffsetY = { -40 })
    ) {
        bookData?.let { bookDataList ->
            Column(modifier = Modifier.padding(4.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(10.dp),
                    userScrollEnabled = true,
                ) {
                    items(items = bookDataList) {
                        it.let {
                            Button(
                                onClick = {
                                    BibleAPIDataModel.run {
                                        updateBookData(it)
                                        updateSelectedChapter(it.remoteKey)
                                    }
                                    scope.launch {
                                        getChapterBibleAPI(
                                            chapterNumber = it.remoteKey,
                                            bibleId = bibleId
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(50), // Rounded corners
                                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                                modifier = Modifier
                                    .padding(2.dp)
                                    .height(IntrinsicSize.Min) // Allow the button to expand to fit the text
                                    .defaultMinSize(
                                        minWidth = 100.dp,
                                        minHeight = 40.dp
                                    ) // Set a minimum size
                            ) {
                                it.cleanedName?.let { name ->
                                    Text(
                                        text = name,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colors.onPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}