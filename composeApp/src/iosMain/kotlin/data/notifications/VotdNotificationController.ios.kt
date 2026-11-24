package data.notifications

import cancelIosDailyVotdNotifications
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import scheduleIosDailyVotdNotifications

private const val KEY_IOS_ENABLED = "email.kevinphillips.biblebible.votd_notifications_enabled"
private const val KEY_IOS_PROMPT_DISMISSED = "email.kevinphillips.biblebible.votd_prompt_dismissed"
private const val KEY_IOS_REMINDER_HOUR = "email.kevinphillips.biblebible.votd_reminder_hour"
private const val KEY_IOS_REMINDER_MINUTE = "email.kevinphillips.biblebible.votd_reminder_minute"

private const val KEY_IOS_AVAILABLE = "email.kevinphillips.biblebible.votd_reminders_available"

private class IosNotificationBridge : NotificationPlatformBridge {
    private val defaults = NSUserDefaults.standardUserDefaults

    override val isSupported: Boolean = true

    override fun isUserEnabled(): Boolean =
        defaults.objectForKey(KEY_IOS_ENABLED) != null && defaults.boolForKey(KEY_IOS_ENABLED)

    override fun setUserEnabled(enabled: Boolean) {
        defaults.setBool(enabled, forKey = KEY_IOS_ENABLED)
    }

    override fun isPromptDismissed(): Boolean =
        defaults.objectForKey(KEY_IOS_PROMPT_DISMISSED) != null && defaults.boolForKey(KEY_IOS_PROMPT_DISMISSED)

    override fun setPromptDismissed(dismissed: Boolean) {
        defaults.setBool(dismissed, forKey = KEY_IOS_PROMPT_DISMISSED)
    }

    override fun getReminderTime(): Pair<Int, Int> {
        val hour = if (defaults.objectForKey(KEY_IOS_REMINDER_HOUR) != null) {
            defaults.integerForKey(KEY_IOS_REMINDER_HOUR).toInt().coerceIn(0, 23)
        } else {
            6
        }
        val minute = if (defaults.objectForKey(KEY_IOS_REMINDER_MINUTE) != null) {
            defaults.integerForKey(KEY_IOS_REMINDER_MINUTE).toInt().coerceIn(0, 59)
        } else {
            0
        }
        return hour to minute
    }

    override fun setReminderTime(hour: Int, minute: Int) {
        defaults.setInteger(hour.coerceIn(0, 23).toLong(), forKey = KEY_IOS_REMINDER_HOUR)
        defaults.setInteger(minute.coerceIn(0, 59).toLong(), forKey = KEY_IOS_REMINDER_MINUTE)
    }

    override fun remindersAvailable(): Boolean =
        defaults.objectForKey(KEY_IOS_AVAILABLE) == null || defaults.boolForKey(KEY_IOS_AVAILABLE)

    override fun setRemindersAvailable(available: Boolean) {
        if (remindersAvailable() == available) return
        defaults.setBool(available, forKey = KEY_IOS_AVAILABLE)
        if (available) rescheduleIfEnabled() else cancelIosDailyVotdNotifications()
    }

    override fun rescheduleIfEnabled() {
        if (remindersAvailable() && isUserEnabled()) {
            val (h, m) = getReminderTime()
            scheduleIosDailyVotdNotifications(h, m)
        }
    }

    override fun queryOsPermissionStatus(onResult: (NotificationPermissionStatus) -> Unit) {
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val mapped = when (settings?.authorizationStatus) {
                UNAuthorizationStatusAuthorized,
                UNAuthorizationStatusProvisional,
                UNAuthorizationStatusEphemeral -> NotificationPermissionStatus.GRANTED
                UNAuthorizationStatusDenied -> NotificationPermissionStatus.DENIED
                UNAuthorizationStatusNotDetermined,
                null -> NotificationPermissionStatus.NOT_DETERMINED
                else -> NotificationPermissionStatus.NOT_DETERMINED
            }
            dispatch_async(dispatch_get_main_queue()) {
                // Preserve enabled state for upgrading users who already authorized notifications on an earlier release.
                if (mapped == NotificationPermissionStatus.GRANTED &&
                    defaults.objectForKey(KEY_IOS_ENABLED) == null
                ) {
                    setUserEnabled(true)
                    setPromptDismissed(true)
                }
                if (mapped == NotificationPermissionStatus.GRANTED && isUserEnabled()) {
                    rescheduleIfEnabled()
                }
                onResult(mapped)
            }
        }
    }

    override fun requestEnable(onResult: (NotificationPermissionStatus) -> Unit) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { granted, _ ->
            dispatch_async(dispatch_get_main_queue()) {
                if (granted) {
                    setUserEnabled(true)
                    setPromptDismissed(true)
                    rescheduleIfEnabled()
                    onResult(NotificationPermissionStatus.GRANTED)
                } else {
                    setUserEnabled(false)
                    onResult(NotificationPermissionStatus.DENIED)
                }
            }
        }
    }

    override fun disableAndCancel() {
        setUserEnabled(false)
        cancelIosDailyVotdNotifications()
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun openSystemNotificationSettings() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(
            settingsUrl,
            options = emptyMap<Any?, Any>(),
            completionHandler = null,
        )
    }
}

internal actual fun platformNotificationBridge(): NotificationPlatformBridge = IosNotificationBridge()
