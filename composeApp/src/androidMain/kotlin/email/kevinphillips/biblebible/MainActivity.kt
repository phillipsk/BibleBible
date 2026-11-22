package email.kevinphillips.biblebible

import App
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import data.bible.BibleState
import data.notifications.parseVotdNotificationPayload
import email.kevinphillips.biblebible.cache.DriverFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        DriverFactory.context = applicationContext
        requestNotificationPermissionAndScheduleVotd()
        applyLaunchIntent(intent)

        setContent {
            App(modifier = Modifier)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLaunchIntent(intent)
    }

    private fun requestNotificationPermissionAndScheduleVotd() {
        VotdNotificationReceiver.scheduleDailyVotd(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001,
                )
            }
        }
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
