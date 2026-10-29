package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the real Android Compose renderer on an emulator or connected device. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ReadingPositionInstrumentedTest {
    @Test fun preservesTextAcrossRotation() = runComposeUiTest { runScriptureReflowTest("rotation") }
    @Test fun preservesTextWhenFontIncreases() = runComposeUiTest { runScriptureReflowTest("font-increase") }
    @Test fun preservesTextWhenFontDecreases() = runComposeUiTest { runScriptureReflowTest("font-decrease") }
    @Test fun preservesTextWhenSystemFontScaleChanges() = runComposeUiTest { runScriptureReflowTest("font-scale") }
    @Test fun preservesTextOnDoubleTapZoom() = runComposeUiTest { runScriptureReflowTest("double-tap") }
    @Test fun preservesTextWhenReaderIsRecreated() = runComposeUiTest { runScriptureReflowTest("recreation") }
}
