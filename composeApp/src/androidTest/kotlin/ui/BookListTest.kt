package ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import fake.TestData
import data.bibleIQ.BibleIQDataModel
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookListTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetNavigationState() {
        BibleIQDataModel.showHomePage = true
        BibleIQDataModel.sortAZ = false
    }

    @Test
    fun testBookListRendersWithTestData() {
        composeTestRule.setContent {
            BibleBibleTheme { BibleBookList(bookData = TestData.testBooks) }
        }

        // Verify that book names are displayed
        composeTestRule.onNodeWithText("Genesis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exodus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Matthew").assertIsDisplayed()
    }

    @Test
    fun testBookItemClickBehavior() {
        composeTestRule.setContent {
            BibleBibleTheme { BibleBookList(bookData = TestData.testBooks) }
        }

        // Click on Genesis book
        composeTestRule.onNodeWithText("Genesis").performClick()
        
        composeTestRule.runOnIdle {
            assertEquals("Genesis", BibleIQDataModel.selectedBook.cleanedName)
            assertFalse(BibleIQDataModel.showHomePage)
        }
    }

    @Test
    fun testBookNamesAreDisplayed() {
        composeTestRule.setContent {
            BibleBibleTheme { BibleBookList(bookData = TestData.testBooks) }
        }

        // Verify book names are displayed
        composeTestRule.onNodeWithText("Genesis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exodus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Matthew").assertIsDisplayed()
    }
}
