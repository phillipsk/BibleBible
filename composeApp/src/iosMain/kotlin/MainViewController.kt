import androidx.compose.ui.window.ComposeUIViewController
import data.bibleIQ.BibleIQDataModel
import platform.Foundation.NSProcessInfo

fun MainViewController() = ComposeUIViewController {
    if (NSProcessInfo.processInfo.environment["BIBLEBIBLE_AUTO_DEMO"]?.toString() == "1") {
        if (!BibleIQDataModel.autoDemoTourEnabled) {
            BibleIQDataModel.showBiblicalShorts = false
            BibleIQDataModel.showHomePage = true
            BibleIQDataModel.autoDemoTourEnabled = true
        }
    }
    App()
}
