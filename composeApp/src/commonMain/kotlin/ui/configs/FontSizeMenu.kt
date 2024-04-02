package ui.configs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.bibleIQ.BibleIQDataModel

@Composable
fun FontSizeMenu() {
    var expanded by remember { mutableStateOf(false) }
    var selectedFontSize =
        remember(BibleIQDataModel.selectedFontSize) { BibleIQDataModel.selectedFontSize }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(300)
    )
    Row(
        modifier = Modifier
            .offset(x = 10.dp, y = 0.dp) // Adjust these values as needed
            .wrapContentSize(Alignment.TopStart),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClickableText(
            text = AnnotatedString("$selectedFontSize pt"),
            style = MaterialTheme.typography.subtitle1.copy(fontSize = 14.sp, color = Color.White),
            onClick = { expanded = !expanded }
        )
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "Dropdown Menu",
                modifier = Modifier.graphicsLayer(rotationZ = rotationAngle)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BibleIQDataModel.fontSizeOptions.forEach {
                DropdownMenuItem(onClick = {
                    expanded = false
                    BibleIQDataModel.selectedFontSize = it
                    selectedFontSize = it
                }) {
                    Text("$it pt")
                }
            }
        }
    }
}