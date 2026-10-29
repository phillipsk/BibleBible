package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/** Run on a booted iOS simulator; these use the native Compose renderer. */
@OptIn(ExperimentalTestApi::class)
class ReadingPositionTest {
    @Test fun preservesTextAcrossLayoutReflow() = runComposeUiTest { runScriptureReflowTest("rotation") }
    @Test fun preservesTextWhenFontIncreases() = runComposeUiTest { runScriptureReflowTest("font-increase") }
    @Test fun preservesTextWhenFontDecreases() = runComposeUiTest { runScriptureReflowTest("font-decrease") }
    @Test fun preservesTextWhenSystemFontScaleChanges() = runComposeUiTest { runScriptureReflowTest("font-scale") }
    @Test fun preservesTextOnDoubleTapZoom() = runComposeUiTest { runScriptureReflowTest("double-tap") }
    @Test fun preservesTextThroughPinchZoom() = runComposeUiTest { runScriptureReflowTest("pinch") }
    @Test fun preservesTextWhenReaderIsRecreated() = runComposeUiTest { runScriptureReflowTest("recreation") }
    @Test fun preservesTextWhenOnlyViewportHeightChanges() = runComposeUiTest { runScriptureReflowTest("height") }
    @Test fun preservesBottomAnchorThroughResizeRoundtrip() = runComposeUiTest { runScriptureReflowTest("bottom-roundtrip") }
}
