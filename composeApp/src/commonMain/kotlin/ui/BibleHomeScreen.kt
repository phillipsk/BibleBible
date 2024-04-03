package ui

import BibleScripturesPager
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
import androidx.compose.material.BackdropScaffold
import androidx.compose.material.BackdropScaffoldState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiModel
import data.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import data.bibleIQ.BibleIQDataModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.configs.BackLayerConfigs
import ui.configs.FrontLayerTopBar
import ui.configs.HomeTopBar

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BibleHomeScreen(backdropScaffoldState: BackdropScaffoldState) {
    val errorMsg = BibleIQDataModel.errorSnackBar
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val localBackdropScaffoldState = remember { backdropScaffoldState }
    LaunchedEffect(BibleIQDataModel.isFirstLaunch) {
        delay(450)
        localBackdropScaffoldState.conceal()
        BibleIQDataModel.isFirstLaunch = false
    }
    var showSubtitle by mutableStateOf(true)
    LaunchedEffect(Unit) {
        delay(1000)
        showSubtitle = false
    }
    BackdropScaffold(
        scaffoldState = backdropScaffoldState,
        appBar = {
            HomeTopBar(
                onClick = { BibleIQDataModel.onHomeClick() },
                showSubtitle = showSubtitle
            )
        },
        backLayerContent = {
            BackLayerConfigs(bibleVersionsList = BibleIQDataModel.bibleVersions)
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        frontLayerShape = TopCutShape(Orientation.TOP),
        frontLayerContent = ({
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FrontLayerTopBar {
                    BibleIQDataModel.onHomeClick()
                }
                if (BibleIQDataModel.showHomePage) {
                    BibleBookList(
                        bookData = BibleAPIDataModel.uiBooks.data,
                        selectedChapter = BibleAPIDataModel.selectedChapter,
                        bibleId = BibleIQDataModel.selectedVersion
                    )
                } else {
                    BibleIQDataModel.bibleChapter?.let { it1 ->
                        BibleScripturesPager(
                            chapters = it1,
                            bibleVersion = BibleIQDataModel.selectedVersion,
                            selectedBook = BibleIQDataModel.selectedBook,
                            isAISummaryLoading = GeminiModel.isLoading,
                            showAISummary = GeminiModel.showSummary
                        )
                    }
                }
            }
            if (errorMsg.isNotEmpty()) {
                scope.launch {
                    snackbarHostState.showSnackbar(errorMsg).also {
                        BibleIQDataModel.clearErrorSnackBar()
                    }
                    BibleIQDataModel.showHomePage = true
                }
            }
        })
    )
}

@Composable
internal fun BibleBookList(bookData: List<BookData>?, selectedChapter: String, bibleId: String) {
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = BibleIQDataModel.showHomePage,
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
                                    BibleIQDataModel.updateSelectedBook(it)
                                    BibleIQDataModel.showHomePage = false
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