
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import data.apiBible.BibleAPIDataModel
import data.apiBible.checkDatabaseSize
import data.apiBible.getBiblesBibleAPI
import data.apiBible.getBooksBibleAPI
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import ui.BibleHomeScreen
import ui.MyCustomMaterialTheme

@Composable
internal fun App() {
    initializeNapier()
    val uiState by remember {
        mutableStateOf(BibleAPIDataModel.uiState)
    }

    LaunchedEffect(true) {
        getBiblesBibleAPI()
        getBooksBibleAPI()
        Napier.v("App :: LaunchedEffect", tag = "BB2452")
        checkDatabaseSize()
    }

    MyCustomMaterialTheme {
        BibleHomeScreen()
    }
}


private val napierInitialized: Boolean by lazy {
    configureNapier()
}

internal fun initializeNapier() {
    if (BibleAPIDataModel.RELEASE_BUILD) {
        if (!napierInitialized) {
            println("Napier initialization error: Napier is not initialized.")
        }
    } else println("Release build :: ${BibleAPIDataModel.RELEASE_BUILD} :: BB2452")
}

internal fun configureNapier(): Boolean {
    return try {
        Napier.base(DebugAntilog())
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}