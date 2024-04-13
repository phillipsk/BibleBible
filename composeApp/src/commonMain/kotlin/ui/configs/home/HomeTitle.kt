package ui.configs.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun HomeTitle(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = "BibleBible",
            style = TextStyle(
                fontFamily = FontFamily.Cursive,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.1.sp,
                color = Color.White,
            ),
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
    }
}