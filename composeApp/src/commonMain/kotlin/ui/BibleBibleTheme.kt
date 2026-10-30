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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biblebible.composeapp.generated.resources.Courgette_Regular
import biblebible.composeapp.generated.resources.Kalam_Regular
import biblebible.composeapp.generated.resources.Res
import biblebible.composeapp.generated.resources.nunito_bold
import biblebible.composeapp.generated.resources.nunito_regular
import biblebible.composeapp.generated.resources.nunito_semibold
import org.jetbrains.compose.resources.Font

@Composable
internal fun BibleBibleTheme(
    content: @Composable () -> Unit
) {
    val colorPalette = if (isSystemInDarkTheme()) DarkColorPalette else LightColorPalette

    MaterialTheme(
        colors = colorPalette,
        typography = getTypography(),
        shapes = Shapes,
        content = content
    )
}

private val LightColorPalette = lightColors(
    primary = Color(0xFF214D48),
    primaryVariant = Color(0xFF173B38),
    secondary = Color(0xFFC28A42),
    background = Color(0xFFF7F3EA),
    surface = Color(0xFFFFFCF6),
    onPrimary = Color(0xFFFFFBF2),
    onSecondary = Color(0xFF281A0D),
    onBackground = Color(0xFF1D2927),
    onSurface = Color(0xFF1D2927)
)

private val DarkColorPalette = darkColors(
    primary = Color(0xFF77B8AE),
    primaryVariant = Color(0xFF173B38),
    secondary = Color(0xFFE1AD69),
    background = Color(0xFF101817),
    surface = Color(0xFF192321),
    onPrimary = Color(0xFFFFFBF2),
    onSecondary = Color(0xFF2B1B09),
    onBackground = Color(0xFFF2EEE5),
    onSurface = Color(0xFFF2EEE5)
)

private val Typography = Typography(
    defaultFontFamily = FontFamily.Default,
    h1 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = 0.15.sp
    ),
    // Add other text styles as needed
)

private val Shapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun getTypography(): Typography {

    val nunitoRegular = FontFamily(
        Font(Res.font.nunito_regular, FontWeight.Normal, FontStyle.Normal)
    )

    val nunitoSemiBold = FontFamily(
        Font(Res.font.nunito_semibold, FontWeight.Normal, FontStyle.Normal)
    )
    val nunitoBold = FontFamily(
        Font(Res.font.nunito_bold, FontWeight.Normal, FontStyle.Normal)
    )
    val kalam = FontFamily(
        Font(Res.font.Kalam_Regular, FontWeight.Normal, FontStyle.Normal)
    )
    val courgette = FontFamily(
        Font(Res.font.Courgette_Regular, FontWeight.Normal, FontStyle.Normal)
    )
    return Typography(
        h1 = TextStyle(
            fontFamily = kalam,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            color = Color.Unspecified,
        ),
        h2 = TextStyle(
            fontFamily = courgette,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            color = Color.Unspecified
        ),
        h3 = TextStyle(
            fontFamily = nunitoBold,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.Unspecified
        ),
        h4 = TextStyle(
            fontFamily = nunitoBold,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.Unspecified
        ),
        h5 = TextStyle(
            fontFamily = nunitoBold,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Unspecified
        ),
        h6 = TextStyle(
            fontFamily = nunitoSemiBold,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Color.Unspecified
        ),
        subtitle1 = TextStyle(
            fontFamily = nunitoSemiBold,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color.Unspecified
        ),
        subtitle2 = TextStyle(
            fontFamily = nunitoRegular,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = Color.Unspecified
        ),
        body1 = TextStyle(
            fontFamily = nunitoRegular,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = Color.White
        ),
        body2 = TextStyle(fontFamily = nunitoRegular, fontSize = 12.sp, color = Color.Unspecified),
        button = TextStyle(
            fontFamily = nunitoRegular,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            color = Color.Unspecified
        ),
        caption = TextStyle(
            fontFamily = nunitoRegular,
            fontWeight = FontWeight.Normal,
            fontSize = 8.sp,
            color = Color.Unspecified
        ),
        overline = TextStyle(
            fontFamily = nunitoRegular,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Color.Unspecified
        )
    )

}
