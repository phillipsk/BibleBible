
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import data.appPrefs.getUserPreferences
import data.apiBible.getBooksBibleAPI
import data.appPrefs.checkDatabaseSize
import data.appPrefs.cleanReadingHistory
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.BibleIQRepository.getVersionsBibleIQ
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import ui.BibleBibleTheme
import ui.BibleHomeScreen
import ui.LoadingScreen

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun App() {
    initializeNapier()
    val isLoading = remember { mutableStateOf(true) }
    LaunchedEffect(true) {
        launch {
            async { checkDatabaseSize() }
            async { cleanReadingHistory() }
        }
        val versionsJob = async { getVersionsBibleIQ() }
        val booksJob = async { getBooksBibleAPI() }
        versionsJob.await()
        booksJob.await()

        getUserPreferences()

        Napier.v("App :: LaunchedEffect", tag = "BB2452")
        isLoading.value = false
    }

    BibleBibleTheme {
        if (isLoading.value) {
            LoadingScreen(true)
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
        println("Release build")
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