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

interface NotificationPlatformBridge {
    val isSupported: Boolean
    fun isUserEnabled(): Boolean
    fun setUserEnabled(enabled: Boolean)
    fun isPromptDismissed(): Boolean
    fun setPromptDismissed(dismissed: Boolean)
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
    }
}
