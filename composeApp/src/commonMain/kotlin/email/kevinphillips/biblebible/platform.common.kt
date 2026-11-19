package email.kevinphillips.biblebible

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

expect fun isDesktopPlatform(): Boolean

expect fun getPlatformName(): String

@Composable
expect fun SetSystemBarColor(darkColor: Color)