package email.kevinphillips.biblebible

import App
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import java.nio.file.Files
import java.nio.file.Path

private fun configureDesktopEnvironment() {
    val osName = System.getProperty("os.name").orEmpty()
    val isMac = osName.contains("mac", ignoreCase = true)
    if (isMac) {
        System.setProperty("apple.awt.application.name", "My BibleBible")
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")
    }
    if (System.getProperty("org.sqlite.tmpdir").isNullOrBlank()) {
        val userHome = System.getProperty("user.home")
        val tmpDir = if (isMac) {
            Path.of(userHome, "Library", "Application Support", "BibleBible", "sqlite-tmp")
        } else {
            Path.of(userHome, ".biblebible", "sqlite-tmp")
        }
        runCatching { Files.createDirectories(tmpDir) }
        System.setProperty("org.sqlite.tmpdir", tmpDir.toAbsolutePath().toString())
    }
}

fun main() {
    configureDesktopEnvironment()
    application {
        val windowState = rememberWindowState(width = 1120.dp, height = 780.dp)
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "My BibleBible",
        ) {
            remember(window) {
                window.minimumSize = Dimension(420, 620)
                true
            }
            App()
        }
    }
}