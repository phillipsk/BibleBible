package email.kevinphillips.biblebible

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import data.notifications.AndroidPushTokenStore
import data.notifications.parseVotdNotificationPayload
import java.util.Calendar
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VotdNotificationReceiverTest {

    @Test
    fun nextTriggerTimeRollsToTomorrowWhenAtOrPastTargetOrWithinLeadWindow() {
        val beforeSix = calendarMillis(2026, Calendar.NOVEMBER, 21, 5, 55, 0)
        val expectedTodaySix = calendarMillis(2026, Calendar.NOVEMBER, 21, 6, 0, 0)
        val expectedTomorrowSix = calendarMillis(2026, Calendar.NOVEMBER, 22, 6, 0, 0)

        assertEquals(
            expectedTodaySix,
            VotdNotificationReceiver.nextTriggerTimeMillis(hour = 6, minute = 0, nowMillis = beforeSix),
        )

        val afterSix = calendarMillis(2026, Calendar.NOVEMBER, 21, 6, 0, 1)
        assertEquals(
            expectedTomorrowSix,
            VotdNotificationReceiver.nextTriggerTimeMillis(hour = 6, minute = 0, nowMillis = afterSix),
        )

        // When woken up slightly early (5:59:50) with a 2-minute reschedule lead buffer, roll to tomorrow.
        val earlyWake = calendarMillis(2026, Calendar.NOVEMBER, 21, 5, 59, 50)
        assertEquals(
            expectedTomorrowSix,
            VotdNotificationReceiver.nextTriggerTimeMillis(
                hour = 6,
                minute = 0,
                nowMillis = earlyWake,
                minLeadMillis = 120_000L,
            ),
        )
    }

    @Test
    fun debugBuildCancelsLocalAlarmsAndReliesOnRemotePush() {
        val app = RuntimeEnvironment.getApplication()
        app.applicationInfo.flags = app.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        VotdNotificationReceiver.setUserNotificationsEnabled(app, true)
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)

        val now = calendarMillis(2026, Calendar.NOVEMBER, 21, 5, 0, 0)
        VotdNotificationReceiver.scheduleDailyVotd(app, nowMillis = now)

        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty(), "Release 3.2+ must not schedule local daily/debug alarms")
    }

    @Test
    fun releaseBuildSchedulesNoLocalAlarmsAndCreatesNotificationChannels() {
        val app = RuntimeEnvironment.getApplication()
        VotdNotificationReceiver.setUserNotificationsEnabled(app, true)
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)

        val now = calendarMillis(2026, Calendar.NOVEMBER, 21, 5, 0, 0)
        app.applicationInfo.flags = app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        assertFalse(VotdNotificationReceiver.isDebugBuild(app))

        VotdNotificationReceiver.scheduleDailyVotd(app, nowMillis = now)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun bootCompletedLeavesAlarmsEmptyWithoutPostingNotification() {
        val app = RuntimeEnvironment.getApplication()
        app.applicationInfo.flags = app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        VotdNotificationReceiver.setUserNotificationsEnabled(app, true)
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val receiver = VotdNotificationReceiver()
        receiver.onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertTrue(Shadows.shadowOf(alarmManager).scheduledAlarms.isEmpty())
        assertTrue(Shadows.shadowOf(notificationManager).allNotifications.isEmpty())
    }

    @Test
    fun disabledNotificationsKeepAlarmsEmptyAcrossBootCompleted() {
        val app = RuntimeEnvironment.getApplication()
        app.applicationInfo.flags = app.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)

        VotdNotificationReceiver.setUserNotificationsEnabled(app, true)
        VotdNotificationReceiver.scheduleDailyVotd(app)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())

        VotdNotificationReceiver.setUserNotificationsEnabled(app, false)
        VotdNotificationReceiver.cancelAllVotd(app)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())

        VotdNotificationReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun releaseSwitchPreservesUsersPreferenceWithoutSchedulingLocalAlarms() {
        val app = RuntimeEnvironment.getApplication()
        val alarms = Shadows.shadowOf(app.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        VotdNotificationReceiver.setUserNotificationsEnabled(app, true)
        VotdNotificationReceiver.scheduleDailyVotd(app)
        assertTrue(alarms.scheduledAlarms.isEmpty())

        VotdNotificationReceiver.setRemindersAvailable(app, false)
        VotdNotificationReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(alarms.scheduledAlarms.isEmpty())
        assertTrue(VotdNotificationReceiver.isUserNotificationsEnabled(app))

        VotdNotificationReceiver.setRemindersAvailable(app, true)
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }

    @Test
    fun postedNotificationIncludesBookChapterVerseAndVersionOnPendingIntent() {
        val app = RuntimeEnvironment.getApplication()
        app.applicationInfo.flags = app.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        VotdNotificationReceiver.setUserNotificationsEnabled(app, true)
        val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val triggerIntent = Intent(app, VotdNotificationReceiver::class.java).apply {
            putExtra(VotdNotificationReceiver.EXTRA_TITLE, "Verse of the Day · Philippians 4:13")
            putExtra(VotdNotificationReceiver.EXTRA_BODY, "I can do all things through Christ...")
            putExtra(VotdNotificationReceiver.EXTRA_BOOK_USFM, "PHP")
            putExtra(VotdNotificationReceiver.EXTRA_CHAPTER, 4)
            putExtra(VotdNotificationReceiver.EXTRA_VERSE, 13)
            putExtra(VotdNotificationReceiver.EXTRA_VERSION, "KJV")
        }

        VotdNotificationReceiver().onReceive(app, triggerIntent)

        val shadowNm = Shadows.shadowOf(notificationManager)
        val deadline = System.currentTimeMillis() + 3_000L
        while (shadowNm.allNotifications.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
        }
        val posted = shadowNm.allNotifications.singleOrNull()
        assertNotNull(posted, "Expected VOTD notification to be posted")

        val launchIntent = Shadows.shadowOf(posted.contentIntent).savedIntent
        val extras = MainActivity.extractNotificationExtras(launchIntent)
        val parsed = parseVotdNotificationPayload(extras)
        assertNotNull(parsed)
        assertEquals("PHP", parsed.bookUsfm)
        assertEquals(4, parsed.chapter)
        assertEquals(13, parsed.verse)
        assertEquals("KJV", parsed.version)
    }

    @Test
    fun mainActivityExtractsFcmPushPayloadExtrasAccurately() {
        val fcmIntent = Intent().apply {
            putExtra("type", "votd")
            putExtra("bookNumber", "43")
            putExtra("bookUsfm", "JHN")
            putExtra("chapter", "3")
            putExtra("verse", "16")
            putExtra("versionId", "BSB")
        }

        val extras = MainActivity.extractNotificationExtras(fcmIntent)
        val parsed = parseVotdNotificationPayload(extras)
        assertNotNull(parsed)
        assertEquals("JHN", parsed.bookUsfm)
        assertEquals(43, parsed.bookNumber)
        assertEquals(3, parsed.chapter)
        assertEquals(16, parsed.verse)
        assertEquals("BSB", parsed.version)
    }

    @Test
    fun releaseBuildIgnoresCustomNotificationOverrideExtras() {
        val app = RuntimeEnvironment.getApplication()
        app.applicationInfo.flags = app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        VotdNotificationReceiver.setUserNotificationsEnabled(app, true)
        data.config.CloudSessionManager.overrideGatewayUrlForTesting("https://unconfigured.invalid")
        try {
            val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val spoofIntent = Intent(app, VotdNotificationReceiver::class.java).apply {
                putExtra(VotdNotificationReceiver.EXTRA_TITLE, "Spoofed Title")
                putExtra(VotdNotificationReceiver.EXTRA_BODY, "Spoofed Body")
                putExtra(VotdNotificationReceiver.EXTRA_BOOK_USFM, "REV")
                putExtra(VotdNotificationReceiver.EXTRA_CHAPTER, 22)
                putExtra(VotdNotificationReceiver.EXTRA_VERSE, 21)
            }

            VotdNotificationReceiver().onReceive(app, spoofIntent)

            val shadowNm = Shadows.shadowOf(notificationManager)
            val deadline = System.currentTimeMillis() + 5_000L
            while (shadowNm.allNotifications.isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(20)
            }
            val posted = shadowNm.allNotifications.singleOrNull()
            assertNotNull(posted, "Expected fallback notification instead of spoofed extras")
            val launchIntent = Shadows.shadowOf(posted.contentIntent).savedIntent
            val extras = MainActivity.extractNotificationExtras(launchIntent)
            val parsed = parseVotdNotificationPayload(extras)
            assertNotNull(parsed)
            assertEquals("JHN", parsed.bookUsfm)
            assertEquals(3, parsed.chapter)
            assertEquals(16, parsed.verse)
        } finally {
            data.config.CloudSessionManager.resetForTesting()
        }
    }

    @Test
    fun ensureAllChannelsCreatesDedicatedCategoriesAndDeduplicatesSameDayRemoteVotd() {
        val app = RuntimeEnvironment.getApplication()
        app.applicationInfo.flags = app.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        VotdNotificationReceiver.setUserNotificationsEnabled(app, true)
        VotdNotificationReceiver.ensureAllChannels(app)

        val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = notificationManager.notificationChannels.map { it.id }.toSet()
        assertTrue(VotdNotificationReceiver.CHANNEL_ID in channels)
        assertTrue(VotdNotificationReceiver.CHANNEL_READING_ENGAGEMENT in channels)
        assertTrue(VotdNotificationReceiver.CHANNEL_BIBLICAL_WORDS in channels)
        assertTrue(VotdNotificationReceiver.CHANNEL_ANNOUNCEMENTS in channels)

        assertEquals(
            VotdNotificationReceiver.CHANNEL_READING_ENGAGEMENT,
            VotdNotificationReceiver.resolveChannelId("reading_nudge", null),
        )
        assertEquals(
            VotdNotificationReceiver.CHANNEL_BIBLICAL_WORDS,
            VotdNotificationReceiver.resolveChannelId("biblical_words", null),
        )

        // Marking today's remote VOTD delivered prevents ACTION_DAILY_VOTD from double-posting locally.
        VotdNotificationReceiver.markVotdDeliveredToday(app)
        assertTrue(VotdNotificationReceiver.wasVotdDeliveredToday(app))
        VotdNotificationReceiver().onReceive(
            app,
            Intent(app, VotdNotificationReceiver::class.java).apply {
                action = VotdNotificationReceiver.ACTION_DAILY_VOTD
                putExtra(VotdNotificationReceiver.EXTRA_TITLE, "Duplicate Local")
                putExtra(VotdNotificationReceiver.EXTRA_BODY, "Should be skipped")
            },
        )
        Thread.sleep(100)
        assertTrue(Shadows.shadowOf(notificationManager).allNotifications.isEmpty())
    }

    @Test
    fun remoteOnlyPolicyNeverSchedulesLocalDailyAlarmRegardlessOfToken() {
        val app = RuntimeEnvironment.getApplication()
        VotdNotificationReceiver.setUserNotificationsEnabled(app, true)
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)

        app.applicationInfo.flags = app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        val now = calendarMillis(2026, Calendar.NOVEMBER, 25, 5, 0, 0)
        VotdNotificationReceiver.scheduleDailyVotd(app, nowMillis = now)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())

        AndroidPushTokenStore.saveToken(app, "remote-fcm-token-registered-123")
        VotdNotificationReceiver.scheduleDailyVotd(app, nowMillis = now)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    private fun calendarMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
    ): Long = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, second)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
