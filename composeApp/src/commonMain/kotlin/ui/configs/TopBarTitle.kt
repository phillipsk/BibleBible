package ui.configs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun TopBarTitle(title: String, showHomePage: Boolean = true, onClick: () -> Unit = {}) {
    Row(Modifier.clickable(onClick = onClick)) {
        if (!showHomePage) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back to Home",
                tint = Color.White,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp)
            )
        }
        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontFamily.Cursive,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.1.sp,
                color = Color.White,
            ),
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}