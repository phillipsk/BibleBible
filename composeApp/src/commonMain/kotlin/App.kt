
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import data.appPrefs.getUserPreferences
import data.appPrefs.ObserveReadingSessionLifecycle
import data.appPrefs.ReadingSession
import data.analytics.AnalyticsTracker
import data.apiBible.getBooksBibleAPI
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.checkDatabaseSize
import data.bibleIQ.cleanReadingHistory
import data.bibleIQ.getVersionsBibleIQ
import data.config.CloudSessionManager
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
fun App(modifier: Modifier = Modifier) {
    BibleBibleTheme {
        ObserveReadingSessionLifecycle()

        initializeNapier()
        val isLoading = remember { mutableStateOf(true) }

        SetSystemBarColor(MaterialTheme.colors.background)

        LaunchedEffect(true) {
            AnalyticsTracker.track("app_launch", "lifecycle")
            launch {
                async { checkDatabaseSize() }
                async { cleanReadingHistory() }
                async { CloudSessionManager.initializeStartupConfig() }
            }
            val versionsJob = async { getVersionsBibleIQ() }
            val booksJob = async { getBooksBibleAPI() }
            versionsJob.await()
            booksJob.await()

            getUserPreferences()
            ReadingSession.restore()

            Napier.v("App :: LaunchedEffect", tag = "BB2452")
            isLoading.value = false
            AnalyticsTracker.track(
                name = "app_startup_complete",
                category = "lifecycle",
                properties = mapOf(
                    "version" to BibleIQDataModel.selectedVersion,
                    "startScreen" to if (BibleIQDataModel.showHomePage) "home" else "scripture_reader",
                ),
            )
        }

        if (isLoading.value) {
            LoadingScreen(true)
        } else {
            BibleHomeScreen(
                scaffoldState = BibleIQDataModel.bottomSheetScaffoldState,
                modifier = modifier,
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
