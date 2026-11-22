import androidx.compose.ui.window.ComposeUIViewController
import data.bible.BibleState
import data.bible.bibleBooksFull
import data.bible.fetchDailyVerse
import data.notifications.VotdNotificationController
import data.notifications.parseVotdNotificationPayload
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.Foundation.NSDateComponents
import platform.Foundation.NSProcessInfo
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

private const val LEGACY_DAILY_VOTD_ID = "biblebible_votd_daily_607am"
private const val DAILY_VOTD_ID = "biblebible_votd_daily_600am"
private const val DEBUG_EVENING_VOTD_ID = "biblebible_votd_debug_900pm"
private val iosNotificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

fun handleIosNotificationTap(userInfo: Map<Any?, *>) {
    val extras = userInfo.entries.associate { (k, v) -> k?.toString().orEmpty() to v?.toString() }
    parseVotdNotificationPayload(extras)?.let { BibleState.enqueueDeepLink(it) }
}

fun MainViewController() = run {
    val processInfo = NSProcessInfo.processInfo
    val hasDemoArg = processInfo.arguments.any { it.toString() == "--auto-demo" }
    val hasDemoEnv = processInfo.environment["BIBLEBIBLE_AUTO_DEMO"]?.toString() == "1"
    if (hasDemoArg || hasDemoEnv) {
        BibleState.showBiblicalShorts = false
        BibleState.showHomePage = true
        BibleState.autoDemoTourEnabled = true
    } else {
        VotdNotificationController.refresh()
    }
    ComposeUIViewController { App() }
}

internal fun cancelIosDailyVotdNotifications() {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    val ids = listOf(LEGACY_DAILY_VOTD_ID, DAILY_VOTD_ID, DEBUG_EVENING_VOTD_ID)
    center.removePendingNotificationRequestsWithIdentifiers(ids)
    center.removeDeliveredNotificationsWithIdentifiers(ids)
}

@OptIn(ExperimentalNativeApi::class)
internal fun scheduleIosDailyVotdNotifications() {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    val isDebug = Platform.isDebugBinary

    // Always clean up legacy 6:07 AM schedule, and clear 9:00 PM test schedule on non-debug builds.
    val idsToRemove = buildList {
        add(LEGACY_DAILY_VOTD_ID)
        if (!isDebug) add(DEBUG_EVENING_VOTD_ID)
    }
    center.removePendingNotificationRequestsWithIdentifiers(idsToRemove)

    val fallbackUserInfo: Map<Any?, Any> = mapOf("type" to "votd")
    scheduleDailyLocalNotification(
        center = center,
        identifier = DAILY_VOTD_ID,
        hour = 6,
        minute = 0,
        title = "Verse of the Day",
        body = "Start your morning with today’s scripture in BibleBible.",
        userInfo = fallbackUserInfo,
    )

    if (isDebug) {
        scheduleDailyLocalNotification(
            center = center,
            identifier = DEBUG_EVENING_VOTD_ID,
            hour = 21,
            minute = 0,
            title = "Verse of the Day (Debug 9 PM)",
            body = "Evening debug test notification · Open today’s scripture in BibleBible.",
            userInfo = fallbackUserInfo,
        )
    }

    iosNotificationScope.launch {
        val daily = runCatching { fetchDailyVerse(BibleState.selectedVersion) }.getOrNull() ?: return@launch
        val data = daily.data
        val bookName = bibleBooksFull.getOrNull(data.bookNumber - 1) ?: data.bookUsfm ?: "Scripture"
        val liveUserInfo: Map<Any?, Any> = buildMap {
            put("type", "votd")
            put("bookNumber", data.bookNumber.toString())
            data.bookUsfm?.takeIf { it.isNotBlank() }?.let { put("bookUsfm", it) }
            put("chapter", data.chapter.toString())
            put("verse", data.verse.toString())
            put("versionId", daily.version)
        }
        val liveTitle = "Verse of the Day · $bookName ${data.chapter}:${data.verse}"
        scheduleDailyLocalNotification(
            center = center,
            identifier = DAILY_VOTD_ID,
            hour = 6,
            minute = 0,
            title = liveTitle,
            body = data.text,
            userInfo = liveUserInfo,
        )
        if (isDebug) {
            scheduleDailyLocalNotification(
                center = center,
                identifier = DEBUG_EVENING_VOTD_ID,
                hour = 21,
                minute = 0,
                title = "$liveTitle (Debug 9 PM)",
                body = data.text,
                userInfo = liveUserInfo,
            )
        }
    }
}

private fun scheduleDailyLocalNotification(
    center: UNUserNotificationCenter,
    identifier: String,
    hour: Long,
    minute: Long,
    title: String,
    body: String,
    userInfo: Map<Any?, Any> = mapOf("type" to "votd"),
) {
    val content = UNMutableNotificationContent().apply {
        setTitle(title)
        setBody(body)
        setSound(UNNotificationSound.defaultSound())
        setUserInfo(userInfo)
    }
    val dateComponents = NSDateComponents().apply {
        this.hour = hour
        this.minute = minute
    }
    val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
        dateComponents = dateComponents,
        repeats = true,
    )
    val request = UNNotificationRequest.requestWithIdentifier(
        identifier = identifier,
        content = content,
        trigger = trigger,
    )
    center.addNotificationRequest(request, withCompletionHandler = null)
}
