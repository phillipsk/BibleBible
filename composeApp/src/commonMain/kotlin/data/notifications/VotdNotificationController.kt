package data.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.native.concurrent.ThreadLocal

enum class NotificationPermissionStatus {
    NOT_DETERMINED,
    GRANTED,
    DENIED,
    UNSUPPORTED,
}

data class DailyReminderTimeOption(
    val hour: Int,
    val minute: Int,
    val label: String,
)

val dailyReminderTimeOptions: List<DailyReminderTimeOption> = listOf(
    DailyReminderTimeOption(5, 0, "5:00 AM"),
    DailyReminderTimeOption(5, 30, "5:30 AM"),
    DailyReminderTimeOption(6, 0, "6:00 AM"),
    DailyReminderTimeOption(6, 30, "6:30 AM"),
    DailyReminderTimeOption(7, 0, "7:00 AM"),
    DailyReminderTimeOption(7, 30, "7:30 AM"),
    DailyReminderTimeOption(8, 0, "8:00 AM"),
    DailyReminderTimeOption(9, 0, "9:00 AM"),
    DailyReminderTimeOption(12, 0, "12:00 PM"),
    DailyReminderTimeOption(18, 0, "6:00 PM"),
    DailyReminderTimeOption(20, 0, "8:00 PM"),
    DailyReminderTimeOption(21, 0, "9:00 PM"),
)

fun formatReminderTime12Hour(hour: Int, minute: Int): String {
    val cleanHour = hour.coerceIn(0, 23)
    val cleanMinute = minute.coerceIn(0, 59)
    val period = if (cleanHour < 12) "AM" else "PM"
    val displayHour = when (val h12 = cleanHour % 12) {
        0 -> 12
        else -> h12
    }
    val minStr = if (cleanMinute < 10) "0$cleanMinute" else "$cleanMinute"
    return "$displayHour:$minStr $period"
}

interface NotificationPlatformBridge {
    val isSupported: Boolean
    fun isUserEnabled(): Boolean
    fun setUserEnabled(enabled: Boolean)
    fun isPromptDismissed(): Boolean
    fun setPromptDismissed(dismissed: Boolean)
    fun getReminderTime(): Pair<Int, Int> = 6 to 0
    fun setReminderTime(hour: Int, minute: Int) {}
    fun rescheduleIfEnabled() {}
    fun queryOsPermissionStatus(onResult: (NotificationPermissionStatus) -> Unit)
    fun requestEnable(onResult: (NotificationPermissionStatus) -> Unit)
    fun disableAndCancel()
    fun openSystemNotificationSettings()
}

expect fun platformNotificationBridge(): NotificationPlatformBridge

@ThreadLocal
object VotdNotificationController {
    private var overrideBridge: NotificationPlatformBridge? = null
    private val bridge: NotificationPlatformBridge
        get() = overrideBridge ?: defaultBridge

    private val defaultBridge: NotificationPlatformBridge by lazy {
        platformNotificationBridge()
    }

    var userEnabled by mutableStateOf(false)
        private set

    var permissionStatus by mutableStateOf(NotificationPermissionStatus.NOT_DETERMINED)
        private set

    var promptDismissed by mutableStateOf(false)
        private set

    var reminderHour by mutableStateOf(6)
        private set

    var reminderMinute by mutableStateOf(0)
        private set

    val formattedReminderTime: String
        get() = formatReminderTime12Hour(reminderHour, reminderMinute)

    val isSupported: Boolean
        get() = bridge.isSupported

    val effectivelyEnabled: Boolean
        get() = isSupported && userEnabled && permissionStatus == NotificationPermissionStatus.GRANTED

    val shouldShowOptInPrompt: Boolean
        get() = isSupported &&
            !promptDismissed &&
            !userEnabled &&
            permissionStatus != NotificationPermissionStatus.DENIED &&
            permissionStatus != NotificationPermissionStatus.UNSUPPORTED

    fun refresh() {
        val currentBridge = bridge
        userEnabled = currentBridge.isUserEnabled()
        promptDismissed = currentBridge.isPromptDismissed()
        val (savedHour, savedMinute) = currentBridge.getReminderTime()
        reminderHour = savedHour.coerceIn(0, 23)
        reminderMinute = savedMinute.coerceIn(0, 59)
        if (!currentBridge.isSupported) {
            permissionStatus = NotificationPermissionStatus.UNSUPPORTED
            return
        }
        currentBridge.queryOsPermissionStatus { status ->
            permissionStatus = status
            userEnabled = currentBridge.isUserEnabled()
            promptDismissed = currentBridge.isPromptDismissed()
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        val cleanHour = hour.coerceIn(0, 23)
        val cleanMinute = minute.coerceIn(0, 59)
        reminderHour = cleanHour
        reminderMinute = cleanMinute
        bridge.setReminderTime(cleanHour, cleanMinute)
        if (effectivelyEnabled) {
            bridge.rescheduleIfEnabled()
        }
    }

    fun onEnableRequested() {
        val currentBridge = bridge
        if (!currentBridge.isSupported) return
        promptDismissed = true
        currentBridge.setPromptDismissed(true)

        if (permissionStatus == NotificationPermissionStatus.DENIED) {
            currentBridge.openSystemNotificationSettings()
            return
        }

        currentBridge.requestEnable { status ->
            permissionStatus = status
            val enabledNow = status == NotificationPermissionStatus.GRANTED && currentBridge.isUserEnabled()
            userEnabled = enabledNow
        }
    }

    fun onDisableRequested() {
        val currentBridge = bridge
        if (!currentBridge.isSupported) return
        userEnabled = false
        promptDismissed = true
        currentBridge.setUserEnabled(false)
        currentBridge.setPromptDismissed(true)
        currentBridge.disableAndCancel()
    }

    fun dismissOptInPrompt() {
        promptDismissed = true
        bridge.setPromptDismissed(true)
    }

    internal fun setBridgeForTesting(testBridge: NotificationPlatformBridge) {
        overrideBridge = testBridge
        refresh()
    }

    internal fun resetForTesting() {
        overrideBridge = null
        userEnabled = false
        permissionStatus = NotificationPermissionStatus.NOT_DETERMINED
        promptDismissed = false
        reminderHour = 6
        reminderMinute = 0
    }
}
