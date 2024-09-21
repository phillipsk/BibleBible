package email.kevinphillips.biblebible

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

actual fun isDesktopPlatform(): Boolean {
    return true
}

@Composable
actual fun SetSystemBarColor(darkColor: Color) {
}