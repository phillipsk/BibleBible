import androidx.compose.ui.window.ComposeUIViewController
import data.ai.IosOnDeviceAiBridgeRegistry
import data.bible.BibleState
import data.bible.bibleBooksFull
import data.bible.dailyVerseRepository
import data.notifications.IosPushTokenStore
import data.notifications.NotificationDeliveryPolicy
import data.notifications.PushRegistrationRepository
import data.notifications.VotdNotificationController
import data.notifications.parseVotdNotificationPayload
import data.notifications.syncIosPushRegistration
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.Foundation.NSDateComponents
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIColor
import platform.UIKit.UIViewController
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

private const val LEGACY_DAILY_VOTD_ID = "biblebible_votd_daily_607am"
private const val DAILY_VOTD_ID = "biblebible_votd_daily_600am"
private const val DEBUG_EVENING_VOTD_ID = "biblebible_votd_debug_900pm"
private val iosNotificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private var iosLocalScheduleJob: Job? = null

internal fun notifyIosTopicSync() {
    NSNotificationCenter.defaultCenter.postNotificationName(
        aName = "BibleBiblePushTopicsChanged",
        `object` = null,
    )
}

fun iosPushTopics(isDebug: Boolean): List<String> =
    PushRegistrationRepository.defaultTopics(isDebug)

fun iosAllPushTopics(): List<String> =
    PushRegistrationRepository.allKnownTopics()

fun areIosNotificationsEnabled(): Boolean =
    IosPushTokenStore.isNotificationsEnabled()

fun handleIosPushTokenRefresh(token: String, isDebug: Boolean) {
    IosPushTokenStore.saveToken(token)
    if (IosPushTokenStore.isNotificationsEnabled()) {
        notifyIosTopicSync()
    }
    syncIosPushRegistration(isDebugOverride = isDebug)
}

fun handleIosNotificationTap(userInfo: Map<Any?, *>) {
    val extras = userInfo.entries.associate { (k, v) -> k?.toString().orEmpty() to v?.toString() }
    parseVotdNotificationPayload(extras)?.let { BibleState.enqueueDeepLink(it) }
}

fun registerIosOnDeviceAiBridge(
    isAvailable: () -> Boolean,
    extractTopics: (String, String, (String?) -> Unit) -> Unit,
    generateReflection: (String, String, (String?) -> Unit) -> Unit,
) {
    IosOnDeviceAiBridgeRegistry.availabilityCheck = isAvailable
    IosOnDeviceAiBridgeRegistry.topicExtractor = extractTopics
    IosOnDeviceAiBridgeRegistry.reflectionGenerator = generateReflection
}

fun MainViewController(): UIViewController {
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
    return ComposeUIViewController { App() }.apply {
        view.backgroundColor = UIColor(
            red = 9.0 / 255.0,
            green = 17.0 / 255.0,
            blue = 30.0 / 255.0,
            alpha = 1.0,
        )
    }
}

internal fun cancelIosLocalDailyVotdOnly() {
    iosLocalScheduleJob?.cancel()
    val center = UNUserNotificationCenter.currentNotificationCenter()
    val ids = listOf(LEGACY_DAILY_VOTD_ID, DAILY_VOTD_ID)
    center.removePendingNotificationRequestsWithIdentifiers(ids)
}

internal fun cancelIosDailyVotdNotifications() {
    iosLocalScheduleJob?.cancel()
    val center = UNUserNotificationCenter.currentNotificationCenter()
    val ids = listOf(LEGACY_DAILY_VOTD_ID, DAILY_VOTD_ID, DEBUG_EVENING_VOTD_ID)
    center.removePendingNotificationRequestsWithIdentifiers(ids)
    center.removeDeliveredNotificationsWithIdentifiers(ids)
}

@OptIn(ExperimentalNativeApi::class)
internal fun scheduleIosDailyVotdNotifications(
    hour: Int = 6,
    minute: Int = 0,
    hasRemotePushToken: Boolean = IosPushTokenStore.getToken() != null,
) {
    iosLocalScheduleJob?.cancel()
    // Release 3.2+ delivers notifications remotely via /v1/notifications/dispatch-due;
    // clear any legacy local schedules without creating new local triggers.
    val center = UNUserNotificationCenter.currentNotificationCenter()
    val idsToRemove = listOf(LEGACY_DAILY_VOTD_ID, DAILY_VOTD_ID, DEBUG_EVENING_VOTD_ID)
    center.removePendingNotificationRequestsWithIdentifiers(idsToRemove)
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
