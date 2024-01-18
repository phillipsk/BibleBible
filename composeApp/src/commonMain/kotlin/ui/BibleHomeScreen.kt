package ui

import androidx.compose.animation.AnimatedVisibility
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
import data.api.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import data.apiBible.getChapterBibleAPI
import data.bibleIQ.BibleVersion
import kotlinx.coroutines.launch

val homeUiState get() = HomeUiState()

fun updateAbbreviation(abv: String) {
    homeUiState.version = BibleVersion(abbreviation = abv)
}

@Composable
fun BibleHomeScreen() {
    Scaffold(topBar = { HomeTopBar() }) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BibleBookList(
                bookData = BibleAPIDataModel.books.data,
                selectedChapter = BibleAPIDataModel.selectedChapter
            )
            ScrollableTabScriptures(
                chapters = BibleAPIDataModel.chapterContent,
                chapterListBookData = BibleAPIDataModel.selectedBookData.chapterListBookData,
                bookDataList = BibleAPIDataModel.books.data
            )
        }
    }
}

@Composable
fun BibleBookList(bookData: List<BookData>?, selectedChapter: String) {
    val scope = rememberCoroutineScope()
    AnimatedVisibility(!bookData.isNullOrEmpty() && selectedChapter == "") {
        Column(modifier = Modifier.padding(4.dp)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(10.dp),
                userScrollEnabled = true,
            ) {
                items(items = bookData!!) {
                    it.let {
                        Button(
                            onClick = {
                                BibleAPIDataModel.run {
                                    updateBookData(it)
                                    updateSelectedChapter(it.key)
                                }
                                scope.launch {
                                    getChapterBibleAPI()
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