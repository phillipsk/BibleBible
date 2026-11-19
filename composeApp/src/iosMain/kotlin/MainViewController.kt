import androidx.compose.ui.window.ComposeUIViewController
import data.bibleIQ.BibleIQDataModel
import platform.Foundation.NSProcessInfo

fun MainViewController() = run {
    val processInfo = NSProcessInfo.processInfo
    val hasDemoArg = processInfo.arguments.any { it.toString() == "--auto-demo" }
    val hasDemoEnv = processInfo.environment["BIBLEBIBLE_AUTO_DEMO"]?.toString() == "1"
    if (hasDemoArg || hasDemoEnv) {
        BibleIQDataModel.showBiblicalShorts = false
        BibleIQDataModel.showHomePage = true
        BibleIQDataModel.autoDemoTourEnabled = true
    }
    ComposeUIViewController { App() }
}
