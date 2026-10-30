package ui

import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import data.apiBible.BookData
import data.apiBible.Chapter
import data.appPrefs.ReadingSession
import data.bibleIQ.BibleChapterUIState
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalMaterialApi::class)
fun ComposeUiTest.runRestoredChapterLoadTest() {
    val restoredBook = BookData(
        id = "GEN", name = "Genesis",
        chapters = listOf(Chapter(bookId = "GEN", number = "intro")) +
            (1..3).map { Chapter(bookId = "GEN", number = it.toString()) },
    )
    val placeholder = BibleChapterUIState(
        bookId = 1, chapterId = 3, chapterList = listOf(1, 2, 3),
    )
    val requests = mutableListOf<Triple<Int, String, Boolean>>()
    setContent {
        BibleBibleTheme {
            BibleScripturesPager(
                chapters = placeholder,
                bibleVersion = "KJV",
                selectedBook = restoredBook,
                isAISummaryLoading = false,
                showAISummary = false,
                bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
                ),
                loadChapter = { _, chapter, version, history -> requests.add(Triple(chapter, version, history)) },
            )
        }
    }
    mainClock.advanceTimeBy(64)
    waitForIdle()
    runOnIdle {
        assertEquals(listOf(Triple(3, "KJV", false)), requests)
        assertEquals("GEN", ReadingSession.current?.bookKey)
        assertEquals(3, ReadingSession.current?.chapter)
    }
}
