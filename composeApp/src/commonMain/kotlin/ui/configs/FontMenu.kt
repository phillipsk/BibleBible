package ui.configs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.bibleIQ.BibleIQDataModel

@Composable
internal fun FontSizeSlider(fontSizes: List<Int>, selectedFontSize: Int) {
    var userSelectedFontSize by remember { mutableStateOf(selectedFontSize) }
    var sliderPosition by remember { mutableFloatStateOf(selectedFontSize.toFloat()) }

    Row(
        modifier = Modifier.padding(horizontal = 4.dp).wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Font Size ",
            fontFamily = MaterialTheme.typography.h1.fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colors.onSurface,
        )
        Slider(
            modifier = Modifier.width(180.dp),
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                userSelectedFontSize = it.toInt()
                BibleIQDataModel.selectedFontSize = userSelectedFontSize
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.surface,
                activeTrackColor = Color.Gray,
                inactiveTrackColor = Color.LightGray
            ),
            valueRange = fontSizes.first().toFloat()..fontSizes.last().toFloat(),
        )
    }
}