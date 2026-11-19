package email.kevinphillips.biblebible

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

actual fun isDesktopPlatform(): Boolean {
    return true
}

actual fun getPlatformName(): String = "desktop"

@Composable
actual fun SetSystemBarColor(darkColor: Color) {
}