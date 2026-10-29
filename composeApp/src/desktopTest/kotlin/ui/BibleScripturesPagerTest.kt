package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

class BibleScripturesPagerTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testPagerDisplaysContent() = runComposeUiTest {
        runBibleScripturesPagerTest()
    }
}
