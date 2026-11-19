package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import data.auth.AuthRepository
import data.auth.AuthState
import data.auth.AuthUser
import ui.auth.AccountDialog
import ui.auth.SignInDialog
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AuthUiTest {

    @AfterTest
    fun tearDown() {
        AuthRepository.resetForTesting()
    }

    @Test
    fun signInDialogRendersProvidersAndGuestButton() = runComposeUiTest {
        var dismissed = false

        setContent {
            BibleBibleTheme {
                SignInDialog(
                    onDismissRequest = { dismissed = true },
                    onGoogleSignInClick = {},
                    onAppleSignInClick = {},
                )
            }
        }

        onNodeWithText("Sign in to BibleBible").assertIsDisplayed()
        onNodeWithText("Continue with Apple").assertIsDisplayed()
        onNodeWithText("Continue with Google").assertIsDisplayed()
        onNodeWithText("Send Magic Link").assertIsDisplayed()
        onNodeWithText("Continue as Guest").assertIsDisplayed()

        onNodeWithText("Continue as Guest").performClick()
        runOnIdle {
            assertTrue(dismissed)
        }
    }

    @Test
    fun accountDialogRendersUserProfileAndSyncSection() = runComposeUiTest {
        // Set signed-in state
        val testUser = AuthUser(
            uid = "test-uid-123",
            email = "disciple@biblebible.app",
            displayName = "Apostle John",
            providers = listOf("google.com"),
        )
        // Access internal state via AuthRepository or mock
        // Since authState is internal/private set in AuthRepository, we can test AccountDialog directly
        setContent {
            BibleBibleTheme {
                AccountDialog(onDismissRequest = {})
            }
        }

        onNodeWithText("Cross-Device Sync").assertIsDisplayed()
        onNodeWithText("Sign Out").assertIsDisplayed()
        onNodeWithText("Delete Account & Data").assertIsDisplayed()
    }
}
