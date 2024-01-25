package ui

import ScripturesPager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import data.apiBible.getChapterBibleAPI
import kotlinx.coroutines.launch

@Composable
internal fun BibleHomeScreen() {
    val errorMsg = BibleAPIDataModel.errorSnackBar
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = { HomeTopBar(onClick = { BibleAPIDataModel.onHomeClick() }) },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BibleBookList(
                bookData = BibleAPIDataModel.books.data,
                selectedChapter = BibleAPIDataModel.selectedChapter,
                bibleId = BibleAPIDataModel.selectedBibleId
            )
            ScripturesPager(
                chapters = BibleAPIDataModel.chapterContent,
                chapterListBookData = BibleAPIDataModel.selectedBookData.chapterListBookData,
                bibleId = BibleAPIDataModel.selectedBibleId
            )
        }
        if (errorMsg.isNotEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(errorMsg).also {
                    BibleAPIDataModel.clearErrorSnackBar()
                }
            }
        }
    }
}

@Composable
internal fun BibleBookList(bookData: List<BookData>?, selectedChapter: String, bibleId: String) {
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = !bookData.isNullOrEmpty() && selectedChapter == "",
        enter = fadeIn(initialAlpha = 0.4f),
        exit = fadeOut(animationSpec = tween(durationMillis = 250))
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