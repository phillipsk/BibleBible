package ui.configs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import data.bibleIQ.BibleIQDataModel

@Composable
internal fun FontSizeSlider(fontSizes: List<Int>, selectedFontSize: Int) {
    var userSelectedFontSize by remember { mutableStateOf(selectedFontSize) }

    var sliderPosition by remember { mutableFloatStateOf(selectedFontSize.toFloat()) }
    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Font Size: ")
        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                userSelectedFontSize = it.toInt()
                BibleIQDataModel.selectedFontSize = userSelectedFontSize
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.DarkGray,
                activeTrackColor = Color.DarkGray,
                inactiveTrackColor = Color.LightGray
            ),
            steps = fontSizes.lastIndex,
            valueRange = fontSizes.first().toFloat()..fontSizes.last().toFloat(),
        )
    }
}