package data.appPrefs

import data.shorts.ShortsPrefsBacking
import data.shorts.platformShortsPrefsBacking

internal object ReaderTutorialPrefs {
    private const val KEY_READER_TUTORIAL_SEEN = "reader_gesture_tutorial_seen_v1"

    fun shouldShow(backing: ShortsPrefsBacking = platformShortsPrefsBacking()): Boolean =
        runCatching { backing.getInt(KEY_READER_TUTORIAL_SEEN, 0) == 0 }.getOrDefault(false)

    fun markDismissed(backing: ShortsPrefsBacking = platformShortsPrefsBacking()) {
        runCatching { backing.putInt(KEY_READER_TUTORIAL_SEEN, 1) }
    }
}
