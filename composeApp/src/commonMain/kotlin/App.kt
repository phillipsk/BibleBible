
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import data.appPrefs.getUserPreferences
import data.apiBible.getBooksBibleAPI
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.checkDatabaseSize
import data.bibleIQ.cleanReadingHistory
import data.bibleIQ.getVersionsBibleIQ
import email.kevinphillips.biblebible.SetSystemBarColor
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
    BibleBibleTheme {

        initializeNapier()
        val isLoading = remember { mutableStateOf(true) }

        SetSystemBarColor(MaterialTheme.colors.background)

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