package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import data.apiBible.BibleAPIBook
import data.apiBible.BibleAPIDataModel
import data.apiBible.json.JSON_BOOKS_API_BIBLE
import data.bibleIQ.BibleIQDataModel
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class BookNavigationTest {
    @Test
    fun selectingBookOpensItsFirstChapter() = runComposeUiTest {
        val previousBooks = BibleAPIDataModel.bibleBooks
        val previousBook = BibleIQDataModel.selectedBook
        val previousApiBook = BibleAPIDataModel.selectedBookData
        val previousChapter = BibleAPIDataModel.selectedChapter
        val previousHome = BibleIQDataModel.showHomePage
        try {
            val books = Json.decodeFromString<BibleAPIBook>(JSON_BOOKS_API_BIBLE)
            BibleAPIDataModel.updateBooks(books)
            BibleIQDataModel.showHomePage = true
            setContent { BibleBookList(books.data) }
            onNodeWithText("Exodus").performClick()
            runOnIdle {
                assertEquals("Exodus", BibleIQDataModel.selectedBook.name)
                assertEquals("EXO.1", BibleAPIDataModel.selectedChapter)
                assertFalse(BibleIQDataModel.showHomePage)
            }
        } finally {
            BibleAPIDataModel.updateBooks(previousBooks)
            BibleAPIDataModel.updateBookData(previousApiBook)
            BibleAPIDataModel.updateSelectedChapter(previousChapter)
            BibleIQDataModel.updateSelectedBook(previousBook)
            BibleIQDataModel.showHomePage = previousHome
        }
    }
}
