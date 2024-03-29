package email.kevinphillips.biblebible

import App
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import email.kevinphillips.biblebible.cache.DriverFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DriverFactory.context = applicationContext

        setContent {
            App()
        }
    }
}

@Preview
@Composable
internal fun AppAndroidPreview() {
    App()
}