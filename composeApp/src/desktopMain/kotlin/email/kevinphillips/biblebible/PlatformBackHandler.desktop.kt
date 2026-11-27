package email.kevinphillips.biblebible

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // Desktop has no hardware back button.
}
