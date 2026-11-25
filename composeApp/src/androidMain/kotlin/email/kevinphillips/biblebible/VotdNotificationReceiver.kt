package email.kevinphillips.biblebible

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import data.config.CloudSessionManager
import data.notifications.AndroidPushTokenStore
import data.notifications.NotificationDeliveryPolicy
import email.kevinphillips.biblebible.security.ProtectedEndpoints
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.concurrent.thread

class VotdNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (!remindersAvailable(context) || !isUserNotificationsEnabled(context.applicationContext)) {
            cancelAllVotd(context.applicationContext)
            return
        }
        if (action in RESCHEDULE_ONLY_ACTIONS) {
            scheduleDailyVotd(context.applicationContext)
            return
        }

        val isDebugEvening = action == ACTION_DEBUG_EVENING_VOTD
        if (isDebugEvening && !isDebugBuild(context)) {
            cancelDebugEveningVotd(context.applicationContext)
            return
        }

        // Re-arm one-shot idle-allowed alarms for the next day (with lead buffer to prevent double-firing on early wake).
        if (action == ACTION_DAILY_VOTD || isDebugEvening) {
            scheduleDailyVotd(
                context = context.applicationContext,
                dailyMinLeadMillis = if (action == ACTION_DAILY_VOTD) RESCHEDULE_LEAD_MILLIS else 0L,
                debugEveningMinLeadMillis = if (isDebugEvening) RESCHEDULE_LEAD_MILLIS else 0L,
            )
        }

        if (action == ACTION_DAILY_VOTD && wasVotdDeliveredToday(context.applicationContext)) {
            return
        }

        val pendingResult = goAsync()
        thread(name = "BibleBibleVotdNotify") {
            try {
                ensureChannel(context)
                val allowCustomExtras = isDebugBuild(context)
                val customTitle = if (allowCustomExtras) intent?.getStringExtra(EXTRA_TITLE) else null
                val customBody = if (allowCustomExtras) intent?.getStringExtra(EXTRA_BODY) else null
                val customBookUsfm = if (allowCustomExtras) intent?.getStringExtra(EXTRA_BOOK_USFM) else null
                val customChapter = if (allowCustomExtras) (intent?.getIntExtra(EXTRA_CHAPTER, -1) ?: -1) else -1
                val customVerse = if (allowCustomExtras) (intent?.getIntExtra(EXTRA_VERSE, 1) ?: 1) else 1
                val customVersion = if (allowCustomExtras) intent?.getStringExtra(EXTRA_VERSION) else null

                val basePayload = if (!customTitle.isNullOrBlank() && !customBody.isNullOrBlank()) {
                    VotdPayload(
                        title = customTitle,
                        body = customBody,
                        bookUsfm = customBookUsfm.orEmpty(),
                        chapter = customChapter.coerceAtLeast(1),
                        verse = customVerse.coerceAtLeast(1),
                        version = customVersion,
                    )
                } else {
                    fetchLiveVotdPayload() ?: VotdPayload(
                        title = "Verse of the Day · John 3:16",
                        body = "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
                        bookUsfm = "JHN",
                        chapter = 3,
                        verse = 16,
                    )
                }

                val payload = if (isDebugEvening) {
                    basePayload.copy(title = "${basePayload.title} (Debug 9 PM)")
                } else {
                    basePayload
                }

                val notificationId = if (isDebugEvening) DEBUG_NOTIFICATION_ID else NOTIFICATION_ID
                showNotification(context, payload, notificationId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private data class VotdPayload(
        val title: String,
        val body: String,
        val bookUsfm: String,
        val chapter: Int,
        val verse: Int = 1,
        val version: String? = null,
    )

    private fun fetchLiveVotdPayload(): VotdPayload? {
        val gateway = CloudSessionManager.configuredGatewayUrl
            .ifBlank { ProtectedEndpoints.gatewayUrl() }
            .trimEnd('/')
        return runCatching {
            val conn = (URL("$gateway${ProtectedEndpoints.votdPreviewPath()}?versionId=KJV").openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
            }
            val code = conn.responseCode
            val raw = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                // Fallback to daily verse endpoint if preview route is unavailable
                val fallbackConn = (URL("$gateway${ProtectedEndpoints.dailyVersePath()}?versionId=KJV").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                }
                val fallbackJson = fallbackConn.inputStream.bufferedReader().use { it.readText() }
                val root = Json.parseToJsonElement(fallbackJson).jsonObject
                val dataNode = root["data"]?.jsonObject ?: root
                val bookUsfm = dataNode["book_usfm"]?.jsonPrimitive?.content ?: "PSA"
                val ch = dataNode["chapter_number"]?.jsonPrimitive?.intOrNull ?: 23
                val v = dataNode["verse_number"]?.jsonPrimitive?.intOrNull ?: 1
                val text = dataNode["text"]?.jsonPrimitive?.content.orEmpty()
                val version = root["version"]?.jsonPrimitive?.content
                return VotdPayload(
                    title = "Verse of the Day · $bookUsfm $ch:$v",
                    body = text,
                    bookUsfm = bookUsfm,
                    chapter = ch,
                    verse = v,
                    version = version,
                )
            }
            val obj = Json.parseToJsonElement(raw).jsonObject
            val dataObj = obj["data"]?.jsonObject
            VotdPayload(
                title = obj["title"]?.jsonPrimitive?.content ?: "Verse of the Day",
                body = obj["body"]?.jsonPrimitive?.content ?: "",
                bookUsfm = dataObj?.get("bookUsfm")?.jsonPrimitive?.content.orEmpty(),
                chapter = dataObj?.get("chapter")?.jsonPrimitive?.intOrNull ?: 1,
                verse = dataObj?.get("verse")?.jsonPrimitive?.intOrNull ?: 1,
                version = dataObj?.get("versionId")?.jsonPrimitive?.content,
            )
        }.getOrNull()
    }

    private fun showNotification(context: Context, payload: VotdPayload, notificationId: Int) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_BOOK_USFM, payload.bookUsfm)
            putExtra(EXTRA_CHAPTER, payload.chapter)
            putExtra(EXTRA_VERSE, payload.verse)
            payload.version?.let { putExtra(EXTRA_VERSION, it) }
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val iconRes = context.applicationInfo.icon
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "verse_of_the_day"
        const val CHANNEL_READING_ENGAGEMENT = "reading_engagement"
        const val CHANNEL_BIBLICAL_WORDS = "biblical_words"
        const val CHANNEL_ANNOUNCEMENTS = "announcements"
        const val ACTION_DAILY_VOTD = "email.kevinphillips.biblebible.ACTION_DAILY_VOTD"
        const val ACTION_DEBUG_EVENING_VOTD = "email.kevinphillips.biblebible.ACTION_DEBUG_EVENING_VOTD"
        const val EXTRA_TITLE = "votd_title"
        const val EXTRA_BODY = "votd_body"
        const val EXTRA_BOOK_USFM = "votd_book_usfm"
        const val EXTRA_CHAPTER = "votd_chapter"
        const val EXTRA_VERSE = "votd_verse"
        const val EXTRA_VERSION = "votd_version"

        internal const val DAILY_HOUR = 6
        internal const val DAILY_MINUTE = 0
        internal const val DEBUG_EVENING_HOUR = 21
        internal const val DEBUG_EVENING_MINUTE = 0

        private const val NOTIFICATION_ID = 600
        private const val DEBUG_NOTIFICATION_ID = 2100
        // Reuse original 6070 request code so existing devices overwrite any legacy 6:07 AM PendingIntent.
        internal const val DAILY_ALARM_REQUEST_CODE = 6070
        internal const val DEBUG_EVENING_ALARM_REQUEST_CODE = 2100
        private const val RESCHEDULE_LEAD_MILLIS = 120_000L

        private const val PREFS_NAME = "biblebible_notifications_prefs"
        private const val KEY_USER_ENABLED = "votd_notifications_enabled"
        private const val KEY_REMINDER_HOUR = "votd_reminder_hour"
        private const val KEY_REMINDER_MINUTE = "votd_reminder_minute"
        private const val KEY_LAST_REMOTE_VOTD_DATE = "votd_last_remote_delivered_date"

        private val RESCHEDULE_ONLY_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )

        private fun localDateKey(nowMillis: Long = System.currentTimeMillis()): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))

        fun markVotdDeliveredToday(context: Context, nowMillis: Long = System.currentTimeMillis()) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_REMOTE_VOTD_DATE, localDateKey(nowMillis))
                .apply()
        }

        fun wasVotdDeliveredToday(context: Context, nowMillis: Long = System.currentTimeMillis()): Boolean {
            val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_REMOTE_VOTD_DATE, null)
            return saved == localDateKey(nowMillis)
        }

        fun resolveChannelId(type: String?, explicitChannelId: String? = null): String {
            val explicit = explicitChannelId?.trim().orEmpty()
            if (explicit in setOf(CHANNEL_ID, CHANNEL_READING_ENGAGEMENT, CHANNEL_BIBLICAL_WORDS, CHANNEL_ANNOUNCEMENTS)) {
                return explicit
            }
            return when (type?.trim()?.lowercase()) {
                "reading_plan", "reading_nudge" -> CHANNEL_READING_ENGAGEMENT
                "biblical_words", "inhouse_words" -> CHANNEL_BIBLICAL_WORDS
                "announcement" -> CHANNEL_ANNOUNCEMENTS
                else -> CHANNEL_ID
            }
        }

        fun showRemoteNotification(
            context: Context,
            title: String,
            body: String,
            channelId: String,
            dataExtras: Map<String, String>,
            notificationId: Int,
        ) {
            ensureAllChannels(context)
            val resolvedChannel = resolveChannelId(dataExtras["type"], channelId)
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                dataExtras.forEach { (k, v) -> putExtra(k, v) }
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val iconRes = context.applicationInfo.icon
            val notification = NotificationCompat.Builder(context, resolvedChannel)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(notificationId, notification)
        }

        internal fun remindersAvailable(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("votd_reminders_available", true)

        internal fun setRemindersAvailable(context: Context, available: Boolean) {
            if (remindersAvailable(context) == available) return
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean("votd_reminders_available", available).apply()
            if (available) scheduleDailyVotd(context) else cancelAllVotd(context)
        }

        fun isUserNotificationsEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_USER_ENABLED, false)
        }

        fun setUserNotificationsEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_USER_ENABLED, enabled)
                .apply()
        }

        fun getReminderHour(context: Context): Int =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_REMINDER_HOUR, DAILY_HOUR)
                .coerceIn(0, 23)

        fun getReminderMinute(context: Context): Int =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_REMINDER_MINUTE, DAILY_MINUTE)
                .coerceIn(0, 59)

        fun setReminderTime(context: Context, hour: Int, minute: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_REMINDER_HOUR, hour.coerceIn(0, 23))
                .putInt(KEY_REMINDER_MINUTE, minute.coerceIn(0, 59))
                .apply()
        }

        fun isDebugBuild(context: Context): Boolean =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        fun ensureChannel(context: Context) {
            ensureAllChannels(context)
        }

        fun ensureAllChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channels = listOf(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Verse of the Day",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = "Daily morning Verse of the Day scripture notification"
                    },
                    NotificationChannel(
                        CHANNEL_READING_ENGAGEMENT,
                        "Reading Nudges & Plans",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = "Daily scripture reading prompts and devotional nudges"
                    },
                    NotificationChannel(
                        CHANNEL_BIBLICAL_WORDS,
                        "Biblical Words",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply {
                        description = "New Biblical Words video reflections and teachings"
                    },
                    NotificationChannel(
                        CHANNEL_ANNOUNCEMENTS,
                        "BibleBible Highlights",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply {
                        description = "Featured study notes and app updates"
                    },
                )
                channels.forEach { manager.createNotificationChannel(it) }
            }
        }

        internal fun nextTriggerTimeMillis(
            hour: Int,
            minute: Int,
            nowMillis: Long = System.currentTimeMillis(),
            minLeadMillis: Long = 0L,
        ): Long {
            val nextTrigger = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= nowMillis + minLeadMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return nextTrigger.timeInMillis
        }

        fun scheduleDailyVotd(
            context: Context,
            nowMillis: Long = System.currentTimeMillis(),
            dailyMinLeadMillis: Long = 0L,
            debugEveningMinLeadMillis: Long = 0L,
        ) {
            if (!remindersAvailable(context) || !isUserNotificationsEnabled(context)) {
                cancelAllVotd(context)
                return
            }
            ensureChannel(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val hasRemotePush = AndroidPushTokenStore.getToken(context.applicationContext) != null
            val scheduleLocalDaily = NotificationDeliveryPolicy.shouldScheduleLocalDailyVotd(
                remindersEnabled = true,
                hasRemotePushToken = hasRemotePush,
            )

            if (scheduleLocalDaily) {
                val dailyTriggerAt = nextTriggerTimeMillis(
                    hour = getReminderHour(context),
                    minute = getReminderMinute(context),
                    nowMillis = nowMillis,
                    minLeadMillis = dailyMinLeadMillis,
                )
                scheduleIdleAllowedAlarm(
                    alarmManager = alarmManager,
                    triggerAtMillis = dailyTriggerAt,
                    pendingIntent = buildAlarmPendingIntent(context, ACTION_DAILY_VOTD, DAILY_ALARM_REQUEST_CODE),
                )
            } else {
                cancelDailyVotdOnly(context)
            }

            if (isDebugBuild(context)) {
                val debugTriggerAt = nextTriggerTimeMillis(
                    hour = DEBUG_EVENING_HOUR,
                    minute = DEBUG_EVENING_MINUTE,
                    nowMillis = nowMillis,
                    minLeadMillis = debugEveningMinLeadMillis,
                )
                scheduleIdleAllowedAlarm(
                    alarmManager = alarmManager,
                    triggerAtMillis = debugTriggerAt,
                    pendingIntent = buildAlarmPendingIntent(
                        context,
                        ACTION_DEBUG_EVENING_VOTD,
                        DEBUG_EVENING_ALARM_REQUEST_CODE,
                    ),
                )
            } else {
                cancelDebugEveningVotd(context)
            }
        }

        fun cancelDailyVotdOnly(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val existing = PendingIntent.getBroadcast(
                context,
                DAILY_ALARM_REQUEST_CODE,
                Intent(context, VotdNotificationReceiver::class.java).apply {
                    action = ACTION_DAILY_VOTD
                },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (existing != null) {
                alarmManager?.cancel(existing)
                existing.cancel()
            }
        }

        fun cancelAllVotd(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            listOf(
                ACTION_DAILY_VOTD to DAILY_ALARM_REQUEST_CODE,
                ACTION_DEBUG_EVENING_VOTD to DEBUG_EVENING_ALARM_REQUEST_CODE,
            ).forEach { (action, requestCode) ->
                val existing = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    Intent(context, VotdNotificationReceiver::class.java).apply {
                        this.action = action
                    },
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                ) ?: return@forEach
                alarmManager?.cancel(existing)
                existing.cancel()
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(NOTIFICATION_ID)
            manager?.cancel(DEBUG_NOTIFICATION_ID)
        }

        fun cancelDebugEveningVotd(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val existing = PendingIntent.getBroadcast(
                context,
                DEBUG_EVENING_ALARM_REQUEST_CODE,
                Intent(context, VotdNotificationReceiver::class.java).apply {
                    action = ACTION_DEBUG_EVENING_VOTD
                },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: return
            alarmManager.cancel(existing)
            existing.cancel()
        }

        private fun buildAlarmPendingIntent(
            context: Context,
            action: String,
            requestCode: Int,
        ): PendingIntent {
            val intent = Intent(context, VotdNotificationReceiver::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun scheduleIdleAllowedAlarm(
            alarmManager: AlarmManager,
            triggerAtMillis: Long,
            pendingIntent: PendingIntent,
        ) {
            // Use Doze-compatible inexact alarms so the app complies with Google Play Exact Alarm policy.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }
}
