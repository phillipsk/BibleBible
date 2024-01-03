import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import data.api.bible.BibleAPIDataModel
import data.api.bible.getBiblesBibleAPI
import data.api.bible.getBooksBibleAPI
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import ui.BibleHomeScreen
import ui.MyCustomMaterialTheme

@Composable
fun App() {
    initializeNapier()
    val uiState by remember {
        mutableStateOf(BibleAPIDataModel.uiState)
    }

    LaunchedEffect(true) {
        getBiblesBibleAPI()
        getBooksBibleAPI()
        Napier.v("App :: LaunchedEffect", tag = "BB2452"  )
    }

    MyCustomMaterialTheme {
        BibleHomeScreen()
    }
}


private val napierInitialized: Boolean by lazy {
    configureNapier()
}

fun initializeNapier() {
    if (!napierInitialized) {
        println("Napier initialization error: Napier is not initialized.")
    }
}

fun configureNapier(): Boolean {
    return try {
        Napier.base(DebugAntilog())
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}