package email.kevinphillips.biblebible

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // System back gestures are handled by iOS navigation chrome.
}
