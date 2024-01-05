
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import data.bibleIQ.getBooks
import data.bibleIQ.getVersions
import ui.BibleHomeScreen
import ui.MyCustomMaterialTheme

@Composable
fun App() {

    LaunchedEffect(true) {
        getVersions()
        getBooks()
    }

    MyCustomMaterialTheme {
        BibleHomeScreen()
    }
}