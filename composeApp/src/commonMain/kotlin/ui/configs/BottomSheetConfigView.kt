package ui.configs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.BottomSheetState
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.bible.BibleState
import data.bible.BibleVersions
import data.bible.fetchReadingHistory
import data.notifications.NotificationPermissionStatus
import data.notifications.VotdNotificationController
import email.kevinphillips.biblebible.isDesktopPlatform
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay

internal const val DRAWER_AUTO_DISMISS_MS = 10_000L

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BottomSheetConfigView(
    bibleVersionsList: BibleVersions,
    showAISummary: Boolean,
    bottomSheetState: BottomSheetState,
    autoDismissDelayMs: Long = DRAWER_AUTO_DISMISS_MS,
) {
    var interactionNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(true) {
        fetchReadingHistory()
        VotdNotificationController.refresh()
    }

    LaunchedEffect(bottomSheetState.isExpanded) {
        if (bottomSheetState.isExpanded) {
            VotdNotificationController.refresh()
        }
    }

    LaunchedEffect(bottomSheetState.isExpanded, interactionNonce, autoDismissDelayMs) {
        if (bottomSheetState.isExpanded && autoDismissDelayMs > 0L) {
            delay(autoDismissDelayMs)
            if (bottomSheetState.isExpanded) {
                bottomSheetState.collapse()
            }
        }
    }

    val readingHistory = BibleState.readingHistory
    Napier.v("LaunchedEffect :: count :: ${readingHistory?.size}", tag = "RH1283")
    val bottomSheetHeight = if (isDesktopPlatform()) 150.dp else 550.dp
    Column(modifier = Modifier.padding(4.dp).height(bottomSheetHeight)) {
        if (!showAISummary) {
            BibleMenu(
                bibleVersionsList = bibleVersionsList,
                selectedVersion = BibleState.selectedVersion,
                bottomSheetState = bottomSheetState,
                onInteraction = { interactionNonce++ },
            )
        }
        FontSizeSlider(
            bottomSheetState = bottomSheetState,
            onInteraction = { interactionNonce++ },
        )
        if (VotdNotificationController.isSupported) {
            DailyVerseNotificationToggleRow(
                onInteraction = { interactionNonce++ },
            )
            DailyReminderTimePickerRow(
                selectedHour = VotdNotificationController.reminderHour,
                selectedMinute = VotdNotificationController.reminderMinute,
                onTimeSelected = { hour, minute ->
                    interactionNonce++
                    VotdNotificationController.updateReminderTime(hour, minute)
                },
                labelColor = MaterialTheme.colors.onSurface,
                onMenuOpened = { interactionNonce++ },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            Divider(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
        }
        if (!readingHistory.isNullOrEmpty()) {
            Text(
                text = "Reading History",
                fontFamily = MaterialTheme.typography.h1.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(readingHistory) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it.toString(),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyVerseNotificationToggleRow(
    onInteraction: () -> Unit,
) {
    val isEnabled = VotdNotificationController.effectivelyEnabled
    val status = VotdNotificationController.permissionStatus
    val timeLabel = VotdNotificationController.formattedReminderTime
    val subtitle = when {
        status == NotificationPermissionStatus.DENIED -> "Blocked in device settings. Tap to open"
        isEnabled -> "$timeLabel daily scripture reminder"
        else -> "Off. Tap to enable $timeLabel reminder"
    }
    val onToggle: (Boolean) -> Unit = { checked ->
        onInteraction()
        if (checked) {
            VotdNotificationController.onEnableRequested()
        } else {
            VotdNotificationController.onDisableRequested()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isEnabled) }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = "Daily Verse Notifications",
                fontFamily = MaterialTheme.typography.h1.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colors.onSurface,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
        )
    }
}