package ui.configs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetState
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.analytics.AnalyticsTracker
import data.bible.BibleState
import data.bible.BibleVersions
import data.bible.BookData
import data.bible.Chapter
import data.bible.ReadingHistoryUIState
import data.bible.bibleBooksFull
import data.bible.fetchReadingHistory
import data.notifications.NotificationPermissionStatus
import data.notifications.VotdNotificationController
import data.notifications.usfmForBookNumber
import email.kevinphillips.biblebible.isDesktopPlatform
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val DRAWER_AUTO_DISMISS_MS = 20_000L

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BottomSheetConfigView(
    bibleVersionsList: BibleVersions,
    showAISummary: Boolean,
    bottomSheetState: BottomSheetState,
    autoDismissDelayMs: Long = DRAWER_AUTO_DISMISS_MS,
    onInitialLoad: suspend () -> Unit = {
        fetchReadingHistory()
        VotdNotificationController.refresh()
    },
) {
    val scope = rememberCoroutineScope()
    var interactionNonce by remember { mutableIntStateOf(0) }
    var highlightedHistoryIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(true) {
        onInitialLoad()
    }

    LaunchedEffect(bottomSheetState.isExpanded) {
        if (bottomSheetState.isExpanded) {
            VotdNotificationController.refresh()
        } else {
            highlightedHistoryIndex = null
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
    val bottomSheetHeight = if (isDesktopPlatform()) 360.dp else 560.dp
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .height(bottomSheetHeight),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFBD38D)),
            )
        }
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
                labelColor = Color(0xFFFBD38D),
                onMenuOpened = { interactionNonce++ },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            Divider(
                color = Color(0x33FBD38D),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
        if (!readingHistory.isNullOrEmpty()) {
            Text(
                text = "Reading History",
                fontFamily = MaterialTheme.typography.h1.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFFFBD38D),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp, start = 4.dp, end = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("reading_history_list"),
            ) {
                itemsIndexed(readingHistory) { index, historyItem ->
                    val isHighlighted = highlightedHistoryIndex == index
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isHighlighted) {
                            Color(0x2EFBD38D)
                        } else {
                            Color(0x14FFFFFF)
                        },
                        border = BorderStroke(
                            width = if (isHighlighted) 1.5.dp else 1.dp,
                            color = if (isHighlighted) {
                                Color(0xFFFBD38D)
                            } else {
                                Color(0x33FBD38D)
                            },
                        ),
                        elevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reading_history_item_$index")
                            .clickable {
                                interactionNonce++
                                highlightedHistoryIndex = index
                                scope.launch {
                                    delay(180L)
                                    openHistoryChapter(historyItem)
                                    bottomSheetState.collapse()
                                    highlightedHistoryIndex = null
                                }
                            },
                    ) {
                        Text(
                            text = historyItem.toString(),
                            color = if (isHighlighted) {
                                Color(0xFFFBD38D)
                            } else {
                                MaterialTheme.colors.onSurface
                            },
                            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun openHistoryChapter(historyItem: ReadingHistoryUIState) {
    val books = BibleState.bibleBooks.data.orEmpty()
    val targetBook = historyItem.bookOrdinal?.let { ordinal ->
        books.getOrNull(ordinal - 1)
    } ?: books.find {
        !historyItem.bookName.isNullOrBlank() &&
            it.name.equals(historyItem.bookName, ignoreCase = true)
    } ?: run {
        val ordinal = historyItem.bookOrdinal
            ?: bibleBooksFull.indexOfFirst {
                it.equals(historyItem.bookName, ignoreCase = true)
            }.takeIf { it >= 0 }?.plus(1)
        val usfm = usfmForBookNumber(ordinal) ?: return
        val chapterCount = (historyItem.chapterId ?: 1).coerceAtLeast(1)
        BookData(
            id = usfm,
            name = historyItem.bookName ?: bibleBooksFull.getOrNull((ordinal ?: 1) - 1) ?: usfm,
            chapters = listOf(Chapter(bookId = "$usfm.intro")) +
                (1..chapterCount).map { ch ->
                    Chapter(
                        name = "$usfm.$ch",
                        bookId = usfm,
                        number = ch.toString(),
                        position = ch,
                    )
                },
        )
    }

    val chapterNum = (historyItem.chapterId ?: 1).coerceAtLeast(1)
    AnalyticsTracker.trackBookSelected(
        bookId = targetBook.bookId,
        bookName = targetBook.name ?: targetBook.bookId,
        source = "reading_history",
    )
    BibleState.returnToShortsOnBack = false
    BibleState.clearHighlightedVerse()
    BibleState.clearPendingTargetVerse()
    BibleState.restoreReadingLocation(targetBook, chapterNum)
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
                color = Color(0xFFFBD38D),
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
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFFBD38D),
                checkedTrackColor = Color(0x66FBD38D),
                uncheckedThumbColor = Color(0xFFCBD5E1),
                uncheckedTrackColor = Color(0x33FFFFFF),
            ),
        )
    }
}