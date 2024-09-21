package email.kevinphillips.biblebible

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

expect fun isDesktopPlatform(): Boolean

@Composable
expect fun SetSystemBarColor(darkColor: Color)