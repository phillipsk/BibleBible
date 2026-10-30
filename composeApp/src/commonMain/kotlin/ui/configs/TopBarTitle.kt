package ui.configs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun TopBarTitle(title: String, showHomePage: Boolean = true, onClick: () -> Unit = {}) {
    Row(Modifier.clickable(onClick = onClick)) {
        if (!showHomePage) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Home",
                tint = MaterialTheme.colors.onPrimary,
                modifier = Modifier.padding(end = 8.dp).size(22.dp)
            )
        }
        Text(
            text = title,
            style = TextStyle(
                fontFamily = MaterialTheme.typography.h1.fontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                color = MaterialTheme.colors.onPrimary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
