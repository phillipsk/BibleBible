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
import data.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import data.apiBible.Chapter
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
        requestNotificationPermissionAndScheduleVotd()
        applyLaunchIntent(intent)

        setContent {
            App(modifier = Modifier)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyLaunchIntent(intent)
    }

    private fun requestNotificationPermissionAndScheduleVotd() {
        VotdNotificationReceiver.scheduleDaily607Am(applicationContext)
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
            BibleIQDataModel.showBiblicalShorts = false
            BibleIQDataModel.showHomePage = true
            BibleIQDataModel.autoDemoTourEnabled = true
            return
        }
        val bookUsfm = launchIntent?.getStringExtra(VotdNotificationReceiver.EXTRA_BOOK_USFM).orEmpty()
        val chapter = launchIntent?.getIntExtra(VotdNotificationReceiver.EXTRA_CHAPTER, -1) ?: -1
        if (bookUsfm.isNotBlank() && chapter >= 1) {
            val book = BibleAPIDataModel.bibleBooks.data?.find {
                it.bookId.equals(bookUsfm, ignoreCase = true)
            } ?: BookData(
                name = bookUsfm,
                chapters = listOf(Chapter(bookId = "$bookUsfm.intro")) +
                    (1..chapter).map { ch ->
                        Chapter(
                            name = "$bookUsfm.$ch",
                            bookId = bookUsfm,
                            number = ch.toString(),
                            position = ch,
                        )
                    },
            )
            BibleIQDataModel.returnToShortsOnBack = false
            BibleIQDataModel.closeBiblicalShorts()
            BibleIQDataModel.restoreReadingLocation(book, chapter)
        }
    }
}

@Preview
@Composable
internal fun AppAndroidPreview() {
    App()
}
