
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import data.apiBible.BibleAPIDataModel
import data.apiBible.checkDatabaseSize
import data.apiBible.getBiblesBibleAPI
import data.apiBible.getBooksBibleAPI
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import ui.BibleBibleTheme
import ui.BibleHomeScreen

@Composable
internal fun App() {
    initializeNapier()

    LaunchedEffect(true) {
        getBiblesBibleAPI()
        getBooksBibleAPI()
        Napier.v("App :: LaunchedEffect", tag = "BB2452")
        checkDatabaseSize()
    }

    BibleBibleTheme {
        BibleHomeScreen()
    }
}


private val napierInitialized: Boolean by lazy {
    configureNapier()
}

internal fun initializeNapier() {
    if (BibleAPIDataModel.RELEASE_BUILD) {
        println("Release build :: ${BibleAPIDataModel.RELEASE_BUILD} :: BB2452")
    }
    else {
        if (!napierInitialized) {
            println("Napier initialization error: Napier is not initialized.")
        }
    }
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