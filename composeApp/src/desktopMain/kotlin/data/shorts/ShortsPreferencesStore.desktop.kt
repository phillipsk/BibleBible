package data.shorts

import java.util.prefs.Preferences

private class DesktopShortsPrefsBacking : ShortsPrefsBacking {
    private val prefs: Preferences by lazy {
        Preferences.userNodeForPackage(DesktopShortsPrefsBacking::class.java)
    }

    override fun getString(key: String): String? = prefs.get(key, null)
    override fun putString(key: String, value: String) {
        prefs.put(key, value)
        runCatching { prefs.flush() }
    }

    override fun getInt(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)
    override fun putInt(key: String, value: Int) {
        prefs.putInt(key, value)
        runCatching { prefs.flush() }
    }

    override fun getLong(key: String, defaultValue: Long): Long = prefs.getLong(key, defaultValue)
    override fun putLong(key: String, value: Long) {
        prefs.putLong(key, value)
        runCatching { prefs.flush() }
    }
}

internal actual fun platformShortsPrefsBacking(): ShortsPrefsBacking = DesktopShortsPrefsBacking()
