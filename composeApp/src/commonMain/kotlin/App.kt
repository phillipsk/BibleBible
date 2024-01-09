
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import data.api.bible.getBiblesBibleAPI
import data.api.bible.getBooksBibleAPI
import ui.BibleHomeScreen
import ui.MyCustomMaterialTheme

@Composable
fun App() {

    LaunchedEffect(true) {
        getBiblesBibleAPI()
        getBooksBibleAPI()
    }

    MyCustomMaterialTheme {
        BibleHomeScreen()
    }
}