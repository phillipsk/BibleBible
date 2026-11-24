package data.appPrefs

import data.bible.currentTimeMillis
import data.shorts.ShortsPrefsBacking
import data.shorts.platformShortsPrefsBacking

internal object ReaderTutorialPrefs {
    private const val KEY_READER_TUTORIAL_SEEN = "reader_gesture_tutorial_seen_v1"

    fun shouldShow(
        backing: ShortsPrefsBacking = platformShortsPrefsBacking(),
        enabled: Boolean = true,
        reminderDays: Int = DEFAULT_TUTORIAL_REMINDER_DAYS,
    ): Boolean = runCatching {
        TutorialReminderPrefs(backing, KEY_READER_TUTORIAL_SEEN, ::currentTimeMillis)
            .shouldShow(enabled, reminderDays)
    }.getOrDefault(false)

    fun markDismissed(backing: ShortsPrefsBacking = platformShortsPrefsBacking()) {
        runCatching {
            TutorialReminderPrefs(backing, KEY_READER_TUTORIAL_SEEN, ::currentTimeMillis).markDismissed()
        }
    }
}
