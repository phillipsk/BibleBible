package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag

@Composable
fun LoadingScreen(isAppStartup: Boolean = false) {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().testTag("LoadingScreen")
            .background(color = if (isAppStartup) MaterialTheme.colors.surface else Color.Transparent)
    ) {
        AnimatedVisibility(visible = true, enter = fadeIn(), exit = slideOutVertically()) {
            CircularProgressIndicator()
        }
    }
}