package ui

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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiModel
import data.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import data.bibleIQ.BibleIQDataModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import ui.configs.BibleBibleTopBar
import ui.configs.BottomSheetConfigView

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BibleHomeScreen(
    scaffoldState: BottomSheetScaffoldState,
) {
    val errorMsg = BibleIQDataModel.errorSnackBar
    val scope = rememberCoroutineScope()
    val localScaffoldState = remember { scaffoldState }

    LaunchedEffect(BibleIQDataModel.showHomePage) {
        localScaffoldState.bottomSheetState.collapse()
    }

    BottomSheetScaffold(
        snackbarHost = {
            CustomSnackbarHost(snackbarHostState = scaffoldState.snackbarHostState)
        },
        scaffoldState = scaffoldState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetPeekHeight = 0.dp,
        sheetContent = {
            BottomSheetConfigView(
                bibleVersionsList = BibleIQDataModel.bibleVersions,
                showAISummary = GeminiModel.showSummary,
                readingHistory = BibleAPIDataModel.readingHistory
            )
        },
        content = ({
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BibleBibleTopBar(
                    onClick = BibleIQDataModel.onHomeClick,
                    showBottomSheet = {
                        scope.launch {
                            if (localScaffoldState.bottomSheetState.isExpanded) {
                                localScaffoldState.bottomSheetState.collapse()
                            } else {
                                Napier.v("BibleAPIDataModel.readingHistory :: ${BibleAPIDataModel.readingHistory}", tag = "RH1283")
                                localScaffoldState.bottomSheetState.expand()
                            }
                        }
                    }
                )
                if (BibleIQDataModel.showHomePage) {
                    BibleBookList(
                        bookData = BibleAPIDataModel.uiBooks.data,
                    )
                } else {
                    BibleIQDataModel.bibleChapter?.let { it1 ->
                        BibleScripturesPager(
                            chapters = it1,
                            bibleVersion = BibleIQDataModel.selectedVersion,
                            selectedBook = BibleIQDataModel.selectedBook,
                            isAISummaryLoading = GeminiModel.isLoading,
                            showAISummary = GeminiModel.showSummary,
                            bottomSheetScaffoldState = localScaffoldState
                        )
                    }
                }
            }
            if (errorMsg.isNotEmpty()) {
                scope.launch {
                    localScaffoldState.snackbarHostState.showSnackbar(errorMsg).also {
                        BibleIQDataModel.clearErrorSnackBar()
                    }
//                    BibleIQDataModel.showHomePage = true
                }
            }
        })
    )
}

@Composable
internal fun BibleBookList(
    bookData: List<BookData>?,
    lazyGridState: LazyGridState = rememberLazyGridState(),
) {
    AnimatedVisibility(
        visible = BibleIQDataModel.showHomePage,
        enter = fadeIn(initialAlpha = 0.4f),
        exit = fadeOut(animationSpec = tween(durationMillis = 250))
    ) {
        bookData?.let { bookDataList ->
            Column(modifier = Modifier.padding(4.dp)) {
                LazyVerticalGrid(
                    state = lazyGridState,
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
                                    BibleIQDataModel.updateSelectedBook(it)
                                    BibleIQDataModel.showHomePage = false
                                },
                                shape = RoundedCornerShape(50), // Rounded corners
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = BibleAPIDataModel.getBibleBookColor(
                                        BibleIQDataModel.getAPIBibleOrdinal(it.bookId)
                                    )
                                ),
                                modifier = Modifier
                                    .padding(2.dp)
                                    .height(IntrinsicSize.Min)
                                    .defaultMinSize(
                                        minWidth = 100.dp,
                                        minHeight = 40.dp
                                    )
                            ) {
                                it.cleanedName?.let { name ->
                                    Text(
                                        fontFamily = MaterialTheme.typography.h3.fontFamily,
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