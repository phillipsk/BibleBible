package ui

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import data.apiBible.BookData
import data.bibleIQ.BibleChapterUIState
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.BibleChapter
import data.bibleIQ.ChapterCount
import data.apiBible.BibleAPIDataModel
import data.apiBible.BibleAPIBook
import data.apiBible.json.JSON_BOOKS_API_BIBLE
import kotlinx.serialization.json.Json

@OptIn(ExperimentalTestApi::class, ExperimentalMaterialApi::class)
fun ComposeUiTest.runBibleScripturesPagerTest() {
    val mockChapters = BibleChapterUIState(
        id = "1",
        bookId = 1,
        chapterId = 1,
        text = "[1] In the beginning...",
        chapterList = listOf(1, 2, 3)
    )
    
    val oldBooks = BibleAPIDataModel.bibleBooks
    val oldBook = BibleIQDataModel.selectedBook
    val books = Json.decodeFromString<BibleAPIBook>(JSON_BOOKS_API_BIBLE)
    BibleAPIDataModel.updateBooks(books)
    BibleIQDataModel.updateSelectedBook(books.data!!.first())
    BibleIQDataModel.updateBibleChapter(listOf(BibleChapter("1", "1", "1", "1", "In the beginning...")), ChapterCount(3), "KJV")
    try {
    setContent {
        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
        )
        
        BibleBibleTheme {
        Box(Modifier.fillMaxSize(), propagateMinConstraints = true) {
        BibleScripturesPager(
            chapters = mockChapters,
            bibleVersion = "KJV",
            selectedBook = BookData(null, null, null, "Genesis", null, emptyList()),
            isAISummaryLoading = false,
            showAISummary = false,
            bottomSheetScaffoldState = scaffoldState,
            loadChapter = { _, _, _ -> }
        )
        }
        }
    }

    onNodeWithText("[1] In the beginning...", substring = true).assertIsDisplayed()
    onNodeWithText("1").assertIsDisplayed()
    onNodeWithText("2").assertIsDisplayed()
    } finally {
        BibleAPIDataModel.updateBooks(oldBooks)
        BibleIQDataModel.updateSelectedBook(oldBook)
        BibleIQDataModel.updateBibleChapter(emptyList(), null, "KJV")
    }
}
