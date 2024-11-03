package email.kevinphillips.biblebible

import androidx.compose.ui.graphics.toArgb
import android.app.Activity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.Composable

actual fun isDesktopPlatform(): Boolean {
    return false
}

@Composable
actual fun SetSystemBarColor(darkColor: Color) {
    val activity = LocalContext.current as Activity
    activity.window.apply {
        navigationBarColor = darkColor.toArgb()

        // Ensure icons are visible if using a dark color for the nav bar
        WindowInsetsControllerCompat(this, this.decorView).apply {
            isAppearanceLightNavigationBars = false
        }
    }
}