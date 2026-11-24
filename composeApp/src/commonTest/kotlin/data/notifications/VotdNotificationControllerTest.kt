package data.notifications

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeNotificationPlatformBridge(
    override val isSupported: Boolean = true,
    var storedEnabled: Boolean = false,
    var storedPromptDismissed: Boolean = false,
    var osStatus: NotificationPermissionStatus = NotificationPermissionStatus.NOT_DETERMINED,
    var requestResult: NotificationPermissionStatus = NotificationPermissionStatus.GRANTED,
) : NotificationPlatformBridge {
    var requestCalls = 0
    var cancelCalls = 0
    var settingsCalls = 0

    override fun isUserEnabled(): Boolean = storedEnabled
    override fun setUserEnabled(enabled: Boolean) {
        storedEnabled = enabled
    }
    override fun isPromptDismissed(): Boolean = storedPromptDismissed
    override fun setPromptDismissed(dismissed: Boolean) {
        storedPromptDismissed = dismissed
    }
    override fun queryOsPermissionStatus(onResult: (NotificationPermissionStatus) -> Unit) {
        onResult(osStatus)
    }
    override fun requestEnable(onResult: (NotificationPermissionStatus) -> Unit) {
        requestCalls++
        osStatus = requestResult
        storedEnabled = requestResult == NotificationPermissionStatus.GRANTED
        onResult(requestResult)
    }
    override fun disableAndCancel() {
        cancelCalls++
    }
    override fun openSystemNotificationSettings() {
        settingsCalls++
    }
}

class VotdNotificationControllerTest {
    @AfterTest
    fun tearDown() {
        VotdNotificationController.resetForTesting()
    }

    @Test
    fun showsPromptOnceUntilUserDismissesOrEnables() {
        val bridge = FakeNotificationPlatformBridge()
        VotdNotificationController.setBridgeForTesting(bridge)

        assertTrue(VotdNotificationController.shouldShowOptInPrompt)
        assertFalse(VotdNotificationController.effectivelyEnabled)

        VotdNotificationController.dismissOptInPrompt()
        assertFalse(VotdNotificationController.shouldShowOptInPrompt)
        assertTrue(bridge.storedPromptDismissed)
    }

    @Test
    fun enableAndDisableToggleUpdatesStateAndCancelsSchedules() {
        val bridge = FakeNotificationPlatformBridge(
            osStatus = NotificationPermissionStatus.NOT_DETERMINED,
            requestResult = NotificationPermissionStatus.GRANTED,
        )
        VotdNotificationController.setBridgeForTesting(bridge)

        VotdNotificationController.onEnableRequested()
        assertEquals(1, bridge.requestCalls)
        assertTrue(VotdNotificationController.effectivelyEnabled)
        assertFalse(VotdNotificationController.shouldShowOptInPrompt)

        VotdNotificationController.onDisableRequested()
        assertFalse(VotdNotificationController.effectivelyEnabled)
        assertFalse(bridge.storedEnabled)
        assertEquals(1, bridge.cancelCalls)
    }

    @Test
    fun permanentlyDeniedEnableRequestOpensSystemSettings() {
        val bridge = FakeNotificationPlatformBridge(
            osStatus = NotificationPermissionStatus.DENIED,
        )
        VotdNotificationController.setBridgeForTesting(bridge)

        assertFalse(VotdNotificationController.shouldShowOptInPrompt)
        VotdNotificationController.onEnableRequested()
        assertEquals(1, bridge.settingsCalls)
        assertEquals(0, bridge.requestCalls)
    }

    @Test
    fun unsupportedDesktopPlatformNeverPromptsOrEnables() {
        val bridge = FakeNotificationPlatformBridge(
            isSupported = false,
            osStatus = NotificationPermissionStatus.UNSUPPORTED,
        )
        VotdNotificationController.setBridgeForTesting(bridge)

        assertFalse(VotdNotificationController.isSupported)
        assertFalse(VotdNotificationController.shouldShowOptInPrompt)
        assertFalse(VotdNotificationController.effectivelyEnabled)
    }
}
