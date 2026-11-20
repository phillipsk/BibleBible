package email.kevinphillips.biblebible

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import data.config.CloudSessionManager
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.concurrent.thread

class VotdNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        thread(name = "BibleBibleVotdNotify") {
            try {
                ensureChannel(context)
                val customTitle = intent?.getStringExtra(EXTRA_TITLE)
                val customBody = intent?.getStringExtra(EXTRA_BODY)
                val customBookUsfm = intent?.getStringExtra(EXTRA_BOOK_USFM)
                val customChapter = intent?.getIntExtra(EXTRA_CHAPTER, -1) ?: -1

                val payload = if (!customTitle.isNullOrBlank() && !customBody.isNullOrBlank()) {
                    VotdPayload(
                        title = customTitle,
                        body = customBody,
                        bookUsfm = customBookUsfm.orEmpty(),
                        chapter = customChapter.coerceAtLeast(1),
                    )
                } else {
                    fetchLiveVotdPayload() ?: VotdPayload(
                        title = "Verse of the Day · John 3:16",
                        body = "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
                        bookUsfm = "JHN",
                        chapter = 3,
                    )
                }

                showNotification(context, payload)
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
    )

    private fun fetchLiveVotdPayload(): VotdPayload? {
        val gateway = CloudSessionManager.configuredGatewayUrl
            .ifBlank { "https://biblebible-api-5jdy3d7zua-uc.a.run.app" }
            .trimEnd('/')
        return runCatching {
            val conn = (URL("$gateway/v1/notifications/preview?versionId=KJV").openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            val code = conn.responseCode
            val raw = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                // Fallback to /GetVerseOfTheDay if gateway hasn't deployed /v1/notifications/preview yet
                val fallbackConn = (URL("$gateway/GetVerseOfTheDay?versionId=KJV").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                val fallbackJson = fallbackConn.inputStream.bufferedReader().use { it.readText() }
                val root = Json.parseToJsonElement(fallbackJson).jsonObject
                val dataNode = root["data"]?.jsonObject ?: root
                val bookUsfm = dataNode["book_usfm"]?.jsonPrimitive?.content ?: "PSA"
                val ch = dataNode["chapter_number"]?.jsonPrimitive?.intOrNull ?: 23
                val v = dataNode["verse_number"]?.jsonPrimitive?.intOrNull ?: 1
                val text = dataNode["text"]?.jsonPrimitive?.content.orEmpty()
                return VotdPayload(
                    title = "Verse of the Day · $bookUsfm $ch:$v",
                    body = text,
                    bookUsfm = bookUsfm,
                    chapter = ch,
                )
            }
            val obj = Json.parseToJsonElement(raw).jsonObject
            val dataObj = obj["data"]?.jsonObject
            VotdPayload(
                title = obj["title"]?.jsonPrimitive?.content ?: "Verse of the Day",
                body = obj["body"]?.jsonPrimitive?.content ?: "",
                bookUsfm = dataObj?.get("bookUsfm")?.jsonPrimitive?.content.orEmpty(),
                chapter = dataObj?.get("chapter")?.jsonPrimitive?.intOrNull ?: 1,
            )
        }.getOrNull()
    }

    private fun showNotification(context: Context, payload: VotdPayload) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_BOOK_USFM, payload.bookUsfm)
            putExtra(EXTRA_CHAPTER, payload.chapter)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
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
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "verse_of_the_day"
        const val ACTION_DAILY_VOTD = "email.kevinphillips.biblebible.ACTION_DAILY_VOTD"
        const val EXTRA_TITLE = "votd_title"
        const val EXTRA_BODY = "votd_body"
        const val EXTRA_BOOK_USFM = "votd_book_usfm"
        const val EXTRA_CHAPTER = "votd_chapter"
        private const val NOTIFICATION_ID = 607
        private const val ALARM_REQUEST_CODE = 6070

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Verse of the Day",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = "Daily morning Verse of the Day scripture notification (6:07 AM)"
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }

        fun scheduleDaily607Am(context: Context) {
            ensureChannel(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, VotdNotificationReceiver::class.java).apply {
                action = ACTION_DAILY_VOTD
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val nextTrigger = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 7)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                nextTrigger.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent,
            )
        }
    }
}
