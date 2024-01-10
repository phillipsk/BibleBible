
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import data.api.bible.getBiblesBibleAPI
import data.api.bible.getBooksBibleAPI
import data.bibleIQ.BibleIQ
import ui.BibleHomeScreen
import ui.MyCustomMaterialTheme

@Composable
fun App() {

    val uiState by remember {
        mutableStateOf(BibleIQ.uiState)
    }

    LaunchedEffect(uiState.updateView) {
        getBiblesBibleAPI()
        getBooksBibleAPI()
        println("refreshView")
    }

    MyCustomMaterialTheme {
        BibleHomeScreen()
    }
}