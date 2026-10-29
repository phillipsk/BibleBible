package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSimpleTextDisplay() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                Text("Hello, BibleBible!")
            }
        }

        composeTestRule.onNodeWithText("Hello, BibleBible!").assertIsDisplayed()
    }
}
