package ui.configs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun TopBarTitle(
    title: String,
    showHomePage: Boolean = true,
    titleColor: Color = Color.White,
    iconTint: Color = Color.White,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
    ) {
        if (!showHomePage) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        BoxWithConstraints(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
            val textMeasurer = rememberTextMeasurer()
            val style = TextStyle(
                fontFamily = MaterialTheme.typography.h1.fontFamily,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                color = titleColor,
            )
            val spacedTitle = buildAnnotatedString {
                append(title)
                val prefix = title.substringBefore(' ')
                val wordStart = if (prefix.isNotEmpty() && prefix.length < title.length && prefix.all(Char::isDigit)) {
                    prefix.length + 1
                } else {
                    0
                }
                if (wordStart < length) {
                    addStyle(SpanStyle(letterSpacing = 1.2.sp), wordStart, length)
                }
            }
            val tightTitle = AnnotatedString(title)
            val titleConstraints = Constraints(maxWidth = constraints.maxWidth)

            fun fits(text: AnnotatedString): Boolean =
                !textMeasurer.measure(
                    text,
                    style = style,
                    softWrap = false,
                    maxLines = 1,
                    constraints = titleConstraints,
                ).didOverflowWidth

            val useWiderSpacing = fits(spacedTitle)
            Text(
                text = if (useWiderSpacing) spacedTitle else tightTitle,
                style = if (useWiderSpacing || fits(tightTitle)) {
                    style
                } else {
                    style.copy(fontSize = 24.sp)
                },
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
