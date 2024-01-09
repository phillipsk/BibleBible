
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import data.api.bible.getBooksBibleAPI
import data.api.bible.getVersionsBibleAPI
import ui.BibleHomeScreen
import ui.MyCustomMaterialTheme

@Composable
fun App() {

    LaunchedEffect(true) {
        getVersionsBibleAPI()
        getBooksBibleAPI()
    }

    MyCustomMaterialTheme {
        BibleHomeScreen()
    }
}