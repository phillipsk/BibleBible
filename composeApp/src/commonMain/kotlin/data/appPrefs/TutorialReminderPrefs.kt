package data.appPrefs

import data.shorts.ShortsPrefsBacking

internal const val DEFAULT_TUTORIAL_REMINDER_DAYS = 30
private const val MILLIS_PER_DAY = 86_400_000L

internal class TutorialReminderPrefs(
    private val backing: ShortsPrefsBacking,
    private val legacySeenKey: String,
    private val now: () -> Long,
) {
    private val dismissalKey = "${legacySeenKey}_dismissal"

    // Zero is unseen, a positive value is the first dismissal, and -1 is complete.
    private fun dismissal(): Long {
        val saved = backing.getLong(dismissalKey, 0L)
        if (saved != 0L || backing.getInt(legacySeenKey, 0) == 0) return saved

        // Older versions saved no date. Start their reminder interval at migration.
        val migrated = now().coerceAtLeast(1L)
        backing.putLong(dismissalKey, migrated)
        return migrated
    }

    fun shouldShow(enabled: Boolean, reminderDays: Int): Boolean {
        val dismissedAt = dismissal()
        return enabled && when {
            dismissedAt == 0L -> true
            dismissedAt < 0L || reminderDays <= 0 -> false
            else -> now() - dismissedAt >= reminderDays * MILLIS_PER_DAY
        }
    }

    fun markDismissed() {
        val saved = dismissal()
        backing.putLong(dismissalKey, if (saved == 0L) now().coerceAtLeast(1L) else -1L)
        backing.putInt(legacySeenKey, 1)
    }
}
