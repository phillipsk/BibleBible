package email.kevinphillips.biblebible

import App
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import data.bible.BibleState
import data.notifications.AndroidNotificationPermissionHost
import data.notifications.VotdNotificationController
import data.notifications.parseVotdNotificationPayload
import email.kevinphillips.biblebible.cache.DriverFactory

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            AndroidNotificationPermissionHost.onPermissionResult(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        DriverFactory.context = applicationContext
        AndroidNotificationPermissionHost.attach(this, notificationPermissionLauncher)
        VotdNotificationController.refresh()
        applyLaunchIntent(intent)

        setContent {
            App(modifier = Modifier)
        }
    }

    override fun onResume() {
        super.onResume()
        VotdNotificationController.refresh()
    }

    override fun onDestroy() {
        AndroidNotificationPermissionHost.detach(this)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLaunchIntent(intent)
    }

    private fun applyLaunchIntent(launchIntent: Intent?) {
        if (launchIntent?.getBooleanExtra("auto_demo", false) == true) {
            BibleState.showBiblicalShorts = false
            BibleState.showHomePage = true
            BibleState.autoDemoTourEnabled = true
            return
        }
        parseVotdNotificationPayload(extractNotificationExtras(launchIntent))?.let { deepLink ->
            BibleState.enqueueDeepLink(deepLink)
        }
    }

    companion object {
        internal fun extractNotificationExtras(intent: Intent?): Map<String, String?> {
            val extras = intent?.extras ?: return emptyMap()
            val mapped = mutableMapOf<String, String?>()
            for (key in extras.keySet()) {
                @Suppress("DEPRECATION")
                mapped[key] = extras.get(key)?.toString()
            }
            return mapped
        }
    }
}

@Preview
@Composable
internal fun AppAndroidPreview() {
    App()
}
