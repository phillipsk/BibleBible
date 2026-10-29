package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

class BibleScripturesPagerTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun preservesTextThroughPinchZoom() = runComposeUiTest { runScriptureReflowTest("pinch") }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun preservesTextWhenOnlyViewportHeightChanges() = runComposeUiTest { runScriptureReflowTest("height") }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun preservesBottomAnchorThroughResizeRoundtrip() = runComposeUiTest { runScriptureReflowTest("bottom-roundtrip") }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun preservesTextAcrossRotation() = runComposeUiTest { runScriptureReflowTest("rotation") }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun preservesTextWhenFontIncreases() = runComposeUiTest { runScriptureReflowTest("font-increase") }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun preservesTextWhenFontDecreases() = runComposeUiTest { runScriptureReflowTest("font-decrease") }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun preservesTextWhenSystemFontScaleChanges() = runComposeUiTest { runScriptureReflowTest("font-scale") }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun preservesTextOnDoubleTapZoom() = runComposeUiTest { runScriptureReflowTest("double-tap") }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun preservesTextWhenReaderIsRecreated() = runComposeUiTest { runScriptureReflowTest("recreation") }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testPagerDisplaysContent() = runComposeUiTest {
        runBibleScripturesPagerTest()
    }
}
