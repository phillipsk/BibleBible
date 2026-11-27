package email.kevinphillips.biblebible

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import data.bible.BibleState
import data.notifications.AndroidPushTokenStore
import data.notifications.PushRegistrationRepository
import data.notifications.syncAndroidPushTopics
import java.util.TimeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BibleFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val appContext = applicationContext
        AndroidPushTokenStore.saveToken(appContext, token)
        val enabled = VotdNotificationReceiver.remindersAvailable(appContext) &&
            VotdNotificationReceiver.isUserNotificationsEnabled(appContext)
        val isDebug = VotdNotificationReceiver.isDebugBuild(appContext)
        if (enabled) {
            syncAndroidPushTopics(isDebug = isDebug, subscribe = true)
        }
        serviceScope.launch {
            PushRegistrationRepository.registerDeviceToken(
                token = token,
                platform = "android",
                isDebugBuild = isDebug,
                timezone = TimeZone.getDefault().id,
                reminderHour = VotdNotificationReceiver.getReminderHour(appContext),
                reminderMinute = VotdNotificationReceiver.getReminderMinute(appContext),
                remindersEnabled = enabled,
                versionId = BibleState.selectedVersion,
            )
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val appContext = applicationContext
        val data = message.data
        val type = data["type"]?.trim()?.lowercase() ?: "votd"
        val channelId = VotdNotificationReceiver.resolveChannelId(type, data["channelId"])
        val title = message.notification?.title?.takeIf { it.isNotBlank() }
            ?: data["title"]?.takeIf { it.isNotBlank() }
            ?: "BibleBible"
        val body = message.notification?.body?.takeIf { it.isNotBlank() }
            ?: data["body"]?.takeIf { it.isNotBlank() }
            ?: "Tap to open today's scripture in BibleBible."

        if (type == "votd") {
            VotdNotificationReceiver.markVotdDeliveredToday(appContext)
        }

        val notificationId = ((System.currentTimeMillis() % 100_000L).toInt() + 3000)
        VotdNotificationReceiver.showRemoteNotification(
            context = appContext,
            title = title,
            body = body,
            channelId = channelId,
            dataExtras = data,
            notificationId = notificationId,
        )
    }
}
