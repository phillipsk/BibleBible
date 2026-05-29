package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BibleScripturesPagerTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testPagerDisplaysContent() = runComposeUiTest {
        runBibleScripturesPagerTest()
    }
}
