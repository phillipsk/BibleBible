
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import data.apiBible.getBooksBibleAPI
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.checkDatabaseSize
import data.bibleIQ.getVersionsBibleIQ
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import ui.BibleBibleTheme
import ui.BibleHomeScreen

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun App() {
    initializeNapier()
    val isLoading = remember { mutableStateOf(true) }
    LaunchedEffect(true) {
        getVersionsBibleIQ()
        getBooksBibleAPI()
        Napier.v("App :: LaunchedEffect", tag = "BB2452")
        isLoading.value = false
        checkDatabaseSize()
    }

    BibleBibleTheme {
        if (isLoading.value) {
            // Loading screen
        } else {
            BibleHomeScreen(
                scaffoldState = BibleIQDataModel.bottomSheetScaffoldState,
            )
        }
    }
}


private val napierInitialized: Boolean by lazy {
    configureNapier()
}

internal fun initializeNapier() {
    if (BibleIQDataModel.RELEASE_BUILD) {
        println("Release build :: ${BibleIQDataModel.RELEASE_BUILD} :: BB2452")
    } else {
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