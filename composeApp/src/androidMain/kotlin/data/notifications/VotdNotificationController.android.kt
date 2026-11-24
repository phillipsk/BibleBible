package data.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import email.kevinphillips.biblebible.VotdNotificationReceiver
import email.kevinphillips.biblebible.cache.DriverFactory
import java.lang.ref.WeakReference

internal const val ANDROID_NOTIF_PREFS = "biblebible_notifications_prefs"
internal const val KEY_NOTIF_ENABLED = "votd_notifications_enabled"
internal const val KEY_PROMPT_DISMISSED = "votd_prompt_dismissed"
internal const val KEY_OS_PERMISSION_ASKED = "votd_os_permission_asked"

internal object AndroidNotificationPermissionHost {
    private var activityRef: WeakReference<ComponentActivity>? = null
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var pendingCallback: ((NotificationPermissionStatus) -> Unit)? = null

    fun attach(activity: ComponentActivity, launcher: ActivityResultLauncher<String>) {
        activityRef = WeakReference(activity)
        permissionLauncher = launcher
    }

    fun detach(activity: ComponentActivity) {
        if (activityRef?.get() === activity) {
            activityRef = null
            permissionLauncher = null
        }
    }

    fun canRequestRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            activityRef?.get() != null &&
            permissionLauncher != null

    fun currentActivity(): ComponentActivity? = activityRef?.get()

    fun launchRuntimePermissionRequest(callback: (NotificationPermissionStatus) -> Unit) {
        val launcher = permissionLauncher
        if (launcher == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            callback(NotificationPermissionStatus.DENIED)
            return
        }
        pendingCallback = callback
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun onPermissionResult(granted: Boolean) {
        val callback = pendingCallback
        pendingCallback = null
        val activity = activityRef?.get()
        val appContext = activity?.applicationContext ?: runCatching { DriverFactory.context }.getOrNull()

        if (granted) {
            if (appContext != null) {
                VotdNotificationReceiver.setUserNotificationsEnabled(appContext, true)
                VotdNotificationReceiver.scheduleDailyVotd(appContext)
            }
            callback?.invoke(NotificationPermissionStatus.GRANTED)
        } else {
            if (appContext != null) {
                VotdNotificationReceiver.setUserNotificationsEnabled(appContext, false)
                VotdNotificationReceiver.cancelAllVotd(appContext)
            }
            val showRationale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            val status = if (showRationale) {
                NotificationPermissionStatus.NOT_DETERMINED
            } else {
                NotificationPermissionStatus.DENIED
            }
            callback?.invoke(status)
        }
    }
}

private class AndroidNotificationBridge : NotificationPlatformBridge {
    private val context: Context?
        get() = AndroidNotificationPermissionHost.currentActivity()?.applicationContext
            ?: runCatching { DriverFactory.context }.getOrNull()

    private val prefs
        get() = context?.getSharedPreferences(ANDROID_NOTIF_PREFS, Context.MODE_PRIVATE)

    override val isSupported: Boolean = true

    override fun isUserEnabled(): Boolean {
        val ctx = context ?: return false
        return VotdNotificationReceiver.isUserNotificationsEnabled(ctx)
    }

    override fun setUserEnabled(enabled: Boolean) {
        val ctx = context ?: return
        VotdNotificationReceiver.setUserNotificationsEnabled(ctx, enabled)
    }

    override fun isPromptDismissed(): Boolean =
        prefs?.getBoolean(KEY_PROMPT_DISMISSED, false) ?: false

    override fun setPromptDismissed(dismissed: Boolean) {
        prefs?.edit()?.putBoolean(KEY_PROMPT_DISMISSED, dismissed)?.apply()
    }

    override fun getReminderTime(): Pair<Int, Int> {
        val ctx = context ?: return 6 to 0
        return VotdNotificationReceiver.getReminderHour(ctx) to VotdNotificationReceiver.getReminderMinute(ctx)
    }

    override fun setReminderTime(hour: Int, minute: Int) {
        val ctx = context ?: return
        VotdNotificationReceiver.setReminderTime(ctx, hour, minute)
    }

    override fun remindersAvailable(): Boolean =
        context?.let { VotdNotificationReceiver.remindersAvailable(it) } ?: true

    override fun setRemindersAvailable(available: Boolean) {
        val ctx = context ?: return
        VotdNotificationReceiver.setRemindersAvailable(ctx, available)
    }

    override fun rescheduleIfEnabled() {
        val ctx = context ?: return
        if (isUserEnabled()) {
            VotdNotificationReceiver.scheduleDailyVotd(ctx)
        }
    }

    override fun queryOsPermissionStatus(onResult: (NotificationPermissionStatus) -> Unit) {
        val ctx = context
        if (ctx == null) {
            onResult(NotificationPermissionStatus.NOT_DETERMINED)
            return
        }
        val status = computeOsStatus(ctx)
        // Preserve enabled state for upgrading users who already granted POST_NOTIFICATIONS before this toggle existed.
        val sp = prefs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            status == NotificationPermissionStatus.GRANTED &&
            sp != null &&
            !sp.contains(KEY_NOTIF_ENABLED)
        ) {
            VotdNotificationReceiver.setUserNotificationsEnabled(ctx, true)
            setPromptDismissed(true)
        }
        if (status == NotificationPermissionStatus.GRANTED && isUserEnabled()) {
            VotdNotificationReceiver.scheduleDailyVotd(ctx)
        }
        onResult(status)
    }

    override fun requestEnable(onResult: (NotificationPermissionStatus) -> Unit) {
        val ctx = context
        if (ctx == null) {
            onResult(NotificationPermissionStatus.DENIED)
            return
        }
        val currentStatus = computeOsStatus(ctx)
        if (currentStatus == NotificationPermissionStatus.GRANTED) {
            VotdNotificationReceiver.setUserNotificationsEnabled(ctx, true)
            VotdNotificationReceiver.scheduleDailyVotd(ctx)
            onResult(NotificationPermissionStatus.GRANTED)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            AndroidNotificationPermissionHost.canRequestRuntimePermission()
        ) {
            prefs?.edit()?.putBoolean(KEY_OS_PERMISSION_ASKED, true)?.apply()
            AndroidNotificationPermissionHost.launchRuntimePermissionRequest(onResult)
            return
        }
        openSystemNotificationSettings()
        onResult(currentStatus)
    }

    override fun disableAndCancel() {
        val ctx = context ?: return
        VotdNotificationReceiver.setUserNotificationsEnabled(ctx, false)
        VotdNotificationReceiver.cancelAllVotd(ctx)
    }

    override fun openSystemNotificationSettings() {
        val ctx = AndroidNotificationPermissionHost.currentActivity() ?: context ?: return
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", ctx.packageName, null)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(intent) }
    }

    private fun computeOsStatus(ctx: Context): NotificationPermissionStatus {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val channelNotificationsEnabled = nm?.areNotificationsEnabled() ?: true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return if (channelNotificationsEnabled) {
                NotificationPermissionStatus.GRANTED
            } else {
                NotificationPermissionStatus.DENIED
            }
        }
        val granted = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            return if (channelNotificationsEnabled) {
                NotificationPermissionStatus.GRANTED
            } else {
                NotificationPermissionStatus.DENIED
            }
        }
        val activity = AndroidNotificationPermissionHost.currentActivity()
        val askedBefore = prefs?.getBoolean(KEY_OS_PERMISSION_ASKED, false) ?: false
        val showRationale = activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        return if (!askedBefore || showRationale) {
            NotificationPermissionStatus.NOT_DETERMINED
        } else {
            NotificationPermissionStatus.DENIED
        }
    }
}

internal actual fun platformNotificationBridge(): NotificationPlatformBridge = AndroidNotificationBridge()
