import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.runComposeUiTest
import data.apiBible.BookData
import data.bibleIQ.BibleChapterUIState
import ui.BibleScripturesPager
import kotlin.test.Test

@OptIn(ExperimentalMaterialApi::class, ExperimentalTestApi::class)
class BibleScripturesPagerTest {

    @Test
    fun testTabClickNavigatesToPage() = runComposeUiTest {
        val initialChapters = BibleChapterUIState(bookId = 2, chapterList = listOf(1, 2, 3))
        val selectedBook = BookData("Exodus", "EXO")

        setContent {
            BibleScripturesPager(
                chapters = initialChapters,
                bibleVersion = "KJV",
                selectedBook = selectedBook,
                isAISummaryLoading = false,
                showAISummary = false,
                bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
            )
        }

        onNodeWithText("2").performClick()
        onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun testAISummaryVisibility() = runComposeUiTest {
        val chapters = BibleChapterUIState(bookId = 1, chapterList = listOf(1))

        setContent {
            BibleScripturesPager(
                chapters = chapters,
                bibleVersion = "KJV",
                selectedBook = BookData("Genesis", "GEN"),
                isAISummaryLoading = true,
                showAISummary = true,
                bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
            )
        }
        onRoot().printToLog("BibleScripturesPagerTest")
        onNodeWithTag("LoadingScreen").assertIsDisplayed()
    }
}