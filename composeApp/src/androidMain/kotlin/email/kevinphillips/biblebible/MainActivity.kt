package email.kevinphillips.biblebible

import App
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import data.bibleIQ.BibleIQDataModel
import email.kevinphillips.biblebible.cache.DriverFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        DriverFactory.context = applicationContext
        applyAutoDemoIntent(intent)

        setContent {
            App(modifier = Modifier)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyAutoDemoIntent(intent)
    }

    private fun applyAutoDemoIntent(launchIntent: Intent?) {
        if (launchIntent?.getBooleanExtra("auto_demo", false) == true) {
            BibleIQDataModel.showBiblicalShorts = false
            BibleIQDataModel.showHomePage = true
            BibleIQDataModel.autoDemoTourEnabled = true
        }
    }
}

@Preview
@Composable
internal fun AppAndroidPreview() {
    App()
}
