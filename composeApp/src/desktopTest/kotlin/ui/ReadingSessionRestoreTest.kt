package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

class ReadingSessionRestoreTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun restoresSavedChapterWithOneLoadAndNoDuplicateHistory() = runComposeUiTest {
        runRestoredChapterLoadTest()
    }
}
