package ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
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

// Define primary and secondary colors
val primary = Color(0xFFFFC107) // Sunny yellow
val secondary = Color(0xFF2196F3) // Vibrant blue

// Define other colors based on primary and secondary
val primaryVariant = Color(0xFFFDB515) // Slightly darker yellow
val secondaryVariant = Color(0xFF1976D2) // Slightly darker blue
val surface = Color(0xFFF2F2F2) // Light gray surface
val background = Color(0xFFE5E5E5) // Slightly darker gray background
val onPrimary = Color(0xFF000000) // Black text on yellow
val onSecondary = Color(0xFFFFFFFF) // White text on blue
val error = Color(0xFFF44336) // Red for errors

// Combine all colors into a Colors instance
val colors = Colors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    secondaryVariant = secondaryVariant,
    surface = surface,
    background = background,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    error = error,
    isLight = true,
    onBackground = error,
    onError = error,
    onSurface = error,
)

object BibleBibleTheme {
    val colors = lightColors(
        primary = Color(0xFFFDB843),
        primaryVariant = Color(0xFFFCAF2B),
        secondary = Color(0xFF4682B4),
        secondaryVariant = Color(0xFF406EAE),
        background = Color(0xFFF7F7F7),
        surface = Color(0xFFFAF9F8),
        error = Color(0xFFD32F2F)
    )
    val typography = Typography(
        defaultFontFamily = FontFamily.Monospace,
/*        h1 = TextStyle(fontSize = 24.sp, fontFamily = FontFamily(Merriweather)),
        h2 = TextStyle(fontSize = 22.sp, fontFamily = FontFamily(Merriweather)),
        h3 = TextStyle(fontSize = 20.sp, fontFamily = FontFamily(Merriweather)),
        body1 = TextStyle(fontSize = 16.sp, fontFamily = FontFamily(NunitoSans)),
        body2 = TextStyle(fontSize = 18.sp, fontFamily = FontFamily(NunitoSans)),
        bold = TextStyle(fontWeight = FontWeight.Bold, fontFamily = FontFamily(NunitoSans))*/
    )
}

@Composable
internal fun BibleBibleApp(content: @Composable () -> Unit) {
    MaterialTheme(colors = BibleBibleTheme.colors, typography = BibleBibleTheme.typography) {
        content()
    }
}


@Composable
internal fun MyCustomMaterialTheme(
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
    primaryVariant = Color(0xFF1976D2),
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
    primaryVariant = Color(0xFF1976D2),
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
    // Add more text styles as needed
)

private val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)

