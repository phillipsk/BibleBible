package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import data.bible.BibleBooksResponse
import data.bible.BibleState
import data.bible.json.BUNDLED_BIBLE_BOOKS_JSON
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class BookNavigationTest {
    @Test
    fun selectingBookOpensItsFirstChapter() = runComposeUiTest {
        val previousBooks = BibleState.bibleBooks
        val previousBook = BibleState.selectedBook
        val previousChapter = BibleState.selectedChapter
        val previousHome = BibleState.showHomePage
        try {
            val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
            BibleState.updateBooks(books)
            BibleState.showHomePage = true
            setContent { BibleBookList(books.data) }
            onNodeWithText("Exodus").performClick()
            runOnIdle {
                assertEquals("Exodus", BibleState.selectedBook.name)
                assertEquals("EXO.1", BibleState.selectedChapter)
                assertFalse(BibleState.showHomePage)
            }
        } finally {
            BibleState.updateBooks(previousBooks)
            BibleState.updateSelectedChapter(previousChapter)
            BibleState.updateSelectedBook(previousBook)
            BibleState.showHomePage = previousHome
        }
    }
}
