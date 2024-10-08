package ui.configs.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biblebible.composeapp.generated.resources.BibleBible_ico_iv
import biblebible.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@Composable
@OptIn(ExperimentalResourceApi::class)
internal fun HomeTitle(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(Res.drawable.BibleBible_ico_iv),
            contentDescription = "BibleBible",
            modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(30.dp))
        )
        Text(
            modifier = Modifier.padding(start = 6.dp, top = 6.dp),
            text = "BibleBible",
            style = TextStyle(
                fontFamily = MaterialTheme.typography.h1.fontFamily,
                letterSpacing = 3.1.sp,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
            ),
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
    }
}