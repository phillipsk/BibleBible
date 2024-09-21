package email.kevinphillips.biblebible

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import platform.UIKit.UIApplication
import platform.UIKit.UIColor

actual fun isDesktopPlatform(): Boolean {
    return false
}

@Composable
actual fun SetSystemBarColor(darkColor: Color) {
    val uiColor = UIColor.yellowColor()
    val keyWindow = UIApplication.sharedApplication.keyWindow
    keyWindow?.backgroundColor = uiColor

    // Optional: change the status bar appearance (iOS 13+)
    // This approach modifies the appearance of the top status bar.
    keyWindow?.rootViewController?.setNeedsStatusBarAppearanceUpdate()
}