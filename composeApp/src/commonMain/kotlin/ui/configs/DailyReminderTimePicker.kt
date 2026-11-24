package ui.configs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.notifications.dailyReminderTimeOptions
import data.notifications.formatReminderTime12Hour

@Composable
internal fun DailyReminderTimePickerRow(
    selectedHour: Int,
    selectedMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    labelColor: Color = Color(0xFFE2E8F0),
    onMenuOpened: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = formatReminderTime12Hour(selectedHour, selectedMinute)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Reminder Time",
            color = labelColor,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Box {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0E1B2E),
                border = BorderStroke(1.dp, Color(0x88FBD38D)),
                elevation = 0.dp,
                modifier = Modifier
                    .clickable {
                        onMenuOpened()
                        expanded = true
                    }
                    .testTag("votd_time_picker_button"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    Text(
                        text = selectedLabel,
                        color = Color(0xFFFBD38D),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Choose reminder time",
                        tint = Color(0xFFFBD38D),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF15243A)),
            ) {
                dailyReminderTimeOptions.forEach { option ->
                    val isSelected = option.hour == selectedHour && option.minute == selectedMinute
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onTimeSelected(option.hour, option.minute)
                        },
                    ) {
                        Text(
                            text = option.label,
                            color = if (isSelected) Color(0xFFFBD38D) else Color.White,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
