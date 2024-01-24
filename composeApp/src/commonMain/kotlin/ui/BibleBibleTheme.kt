package ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        colorPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

private val LightColorPalette = lightColors(
    primary = Color(0xFF2196F3), // Blue
    primaryVariant = Color(0xFF1976D2), // Cyan
    secondary = Color(0xFFFFD600), // Yellowish
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

private val DarkColorPalette = darkColors(
    primary = Color(0xFF2196F3), // Blue
    primaryVariant = Color(0xFF1976D2), // Cyan
    secondary = Color(0xFFFFD600), // Yellowish
    background = Color(0xFF121212), // Dark background color
    surface = Color(0xFF121212), // Dark surface color
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val Typography = Typography(
    defaultFontFamily = FontFamily.Default,
    h1 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = 0.15.sp
    ),
)

private val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)

