package data.shorts

import android.content.Context
import email.kevinphillips.biblebible.cache.DriverFactory

private class AndroidShortsPrefsBacking : ShortsPrefsBacking {
    private val prefs by lazy {
        runCatching {
            DriverFactory.context.getSharedPreferences("biblebible_shorts_prefs", Context.MODE_PRIVATE)
        }.getOrNull()
    }

    override fun getString(key: String): String? = prefs?.getString(key, null)
    override fun putString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int = prefs?.getInt(key, defaultValue) ?: defaultValue
    override fun putInt(key: String, value: Int) {
        prefs?.edit()?.putInt(key, value)?.apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long = prefs?.getLong(key, defaultValue) ?: defaultValue
    override fun putLong(key: String, value: Long) {
        prefs?.edit()?.putLong(key, value)?.apply()
    }
}

actual fun platformShortsPrefsBacking(): ShortsPrefsBacking = AndroidShortsPrefsBacking()
