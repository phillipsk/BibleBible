package ui

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ComposeUiTest
import data.apiBible.BookData
import data.bibleIQ.BibleChapterUIState

@OptIn(ExperimentalTestApi::class, ExperimentalMaterialApi::class)
fun ComposeUiTest.runBibleScripturesPagerTest() {
    val mockChapters = BibleChapterUIState(
        id = "1",
        bookId = 1,
        chapterId = 1,
        text = "[1] In the beginning...",
        chapterList = listOf(1, 2, 3)
    )
    
    setContent {
        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
        )
        
        BibleScripturesPager(
            chapters = mockChapters,
            bibleVersion = "KJV",
            selectedBook = BookData(null, null, null, "Genesis", null, emptyList()),
            isAISummaryLoading = false,
            showAISummary = false,
            bottomSheetScaffoldState = scaffoldState
        )
    }

    onNodeWithText("[1] In the beginning...", substring = true).assertIsDisplayed()
    onNodeWithText("1").assertIsDisplayed()
    onNodeWithText("2").assertIsDisplayed()
}
