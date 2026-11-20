import androidx.compose.ui.window.ComposeUIViewController
import data.bibleIQ.BibleIQDataModel
import platform.Foundation.NSDateComponents
import platform.Foundation.NSProcessInfo
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

fun MainViewController() = run {
    val processInfo = NSProcessInfo.processInfo
    val hasDemoArg = processInfo.arguments.any { it.toString() == "--auto-demo" }
    val hasDemoEnv = processInfo.environment["BIBLEBIBLE_AUTO_DEMO"]?.toString() == "1"
    if (hasDemoArg || hasDemoEnv) {
        BibleIQDataModel.showBiblicalShorts = false
        BibleIQDataModel.showHomePage = true
        BibleIQDataModel.autoDemoTourEnabled = true
    } else {
        scheduleIosDaily607AmVotdNotification()
    }
    ComposeUIViewController { App() }
}

private fun scheduleIosDaily607AmVotdNotification() {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.requestAuthorizationWithOptions(
        options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
    ) { granted, _ ->
        if (!granted) return@requestAuthorizationWithOptions
        val content = UNMutableNotificationContent().apply {
            setTitle("Verse of the Day")
            setBody("Start your morning with today’s scripture in BibleBible.")
            setSound(UNNotificationSound.defaultSound())
        }
        val dateComponents = NSDateComponents().apply {
            hour = 6
            minute = 7
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = dateComponents,
            repeats = true,
        )
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "biblebible_votd_daily_607am",
            content = content,
            trigger = trigger,
        )
        center.addNotificationRequest(request, withCompletionHandler = null)
    }
}
