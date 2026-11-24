
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import data.analytics.AnalyticsTracker
import data.appPrefs.ObserveReadingSessionLifecycle
import data.appPrefs.ReadingSession
import data.appPrefs.getUserPreferences
import data.bible.BibleState
import data.bible.checkDatabaseSize
import data.bible.cleanReadingHistory
import data.bible.loadBibleBooks
import data.bible.loadBibleVersions
import data.config.CloudSessionManager
import data.plans.ReadingPlanRepository
import email.kevinphillips.biblebible.SetSystemBarColor
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import ui.BibleBibleTheme
import ui.BibleHomeScreen
import ui.LoadingScreen

@OptIn(ExperimentalMaterialApi::class, ExperimentalObjCRefinement::class)
@HiddenFromObjC
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
                async { ReadingPlanRepository.loadLocalState() }
                async {
                    CloudSessionManager.initializeStartupConfig()
                    ReadingPlanRepository.refreshRemoteCatalog()
                }
            }
            val versionsJob = async { loadBibleVersions() }
            val booksJob = async { loadBibleBooks() }
            versionsJob.await()
            booksJob.await()

            getUserPreferences()
            val handledDeepLink = BibleState.consumePendingDeepLink()
            if (!handledDeepLink) {
                ReadingSession.restore()
            }

            Napier.v("App :: LaunchedEffect", tag = "BB2452")
            isLoading.value = false
            AnalyticsTracker.track(
                name = "app_startup_complete",
                category = "lifecycle",
                properties = mapOf(
                    "version" to BibleState.selectedVersion,
                    "startScreen" to if (BibleState.showHomePage) "home" else "scripture_reader",
                ),
            )
        }

        LaunchedEffect(isLoading.value, BibleState.pendingDeepLink) {
            if (!isLoading.value && BibleState.pendingDeepLink != null) {
                BibleState.consumePendingDeepLink()
            }
        }

        if (isLoading.value) {
            LoadingScreen(true)
        } else {
            BibleHomeScreen(
                scaffoldState = BibleState.bottomSheetScaffoldState,
                modifier = modifier,
            )
        }
    }
}


private val napierInitialized: Boolean by lazy {
    configureNapier()
}

internal fun initializeNapier() {
    if (BibleState.RELEASE_BUILD) {
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
