package email.kevinphillips.biblebible

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal expect fun isDesktopPlatform(): Boolean

internal expect fun getPlatformName(): String

@Composable
internal expect fun SetSystemBarColor(darkColor: Color)