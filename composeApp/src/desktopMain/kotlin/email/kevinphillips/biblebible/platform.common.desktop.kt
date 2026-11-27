package email.kevinphillips.biblebible

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal actual fun isDesktopPlatform(): Boolean {
    return true
}

internal actual fun getPlatformName(): String = "desktop"

@Composable
internal actual fun SetSystemBarColor(darkColor: Color) {
}