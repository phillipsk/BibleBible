package ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import fake.TestData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookListTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBookListRendersWithTestData() {
        composeTestRule.setContent {
            BibleBookList(bookData = TestData.testBooks)
        }

        // Verify that book names are displayed
        composeTestRule.onNodeWithText("Genesis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exodus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Matthew").assertIsDisplayed()
    }

    @Test
    fun testBookItemClickBehavior() {
        composeTestRule.setContent {
            BibleBookList(
                bookData = TestData.testBooks
            )
        }

        // Click on Genesis book
        composeTestRule.onNodeWithText("Genesis").performClick()
        
        // TODO: Verify click behavior when onBookClick callback is implemented
        // For now, we just verify the book item is clickable
        composeTestRule.onNodeWithText("Genesis").assertIsDisplayed()
    }

    @Test
    fun testBookNamesAreDisplayed() {
        composeTestRule.setContent {
            BibleBookList(bookData = TestData.testBooks)
        }

        // Verify book names are displayed
        composeTestRule.onNodeWithText("Genesis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exodus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Matthew").assertIsDisplayed()
    }
}
