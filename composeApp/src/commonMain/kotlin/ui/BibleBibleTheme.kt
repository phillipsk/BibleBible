package ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun BibleBibleTheme(
    content: @Composable () -> Unit
) {
    val colorPalette = if (isSystemInDarkTheme()) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colorScheme = colorPalette,
        shapes = Shapes,
        content = content
    )
}

private val LightColorPalette = lightColorScheme(
    primary = Color(0xFF3A5F8F), // Dark Blue
    primaryContainer = Color(0xFF8A9BB3), // Light Blue for the pages
    secondary = Color(0xFF3A5F8F), // Dark Blue
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

private val DarkColorPalette = darkColorScheme(
    primary = Color(0xFF3A5F8F), // Dark Blue
    primaryContainer = Color(0xFF8A9BB3), // Light Blue for the pages
    secondary = Color(0xFF3A5F8F), // Dark Blue
    background = Color(0xFF2C3E50), // Even Darker Blue for dark theme background
    surface = Color(0xFF2C3E50), // Even Darker Blue for dark theme surface
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)