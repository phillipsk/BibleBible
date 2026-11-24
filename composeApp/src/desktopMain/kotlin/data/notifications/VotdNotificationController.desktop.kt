package data.notifications

private class DesktopNotificationBridge : NotificationPlatformBridge {
    override val isSupported: Boolean = false
    override fun isUserEnabled(): Boolean = false
    override fun setUserEnabled(enabled: Boolean) {}
    override fun isPromptDismissed(): Boolean = true
    override fun setPromptDismissed(dismissed: Boolean) {}
    override fun queryOsPermissionStatus(onResult: (NotificationPermissionStatus) -> Unit) {
        onResult(NotificationPermissionStatus.UNSUPPORTED)
    }
    override fun requestEnable(onResult: (NotificationPermissionStatus) -> Unit) {
        onResult(NotificationPermissionStatus.UNSUPPORTED)
    }
    override fun disableAndCancel() {}
    override fun openSystemNotificationSettings() {}
}

actual fun platformNotificationBridge(): NotificationPlatformBridge = DesktopNotificationBridge()
