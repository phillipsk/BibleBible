package data.shorts

import platform.Foundation.NSUserDefaults

private class IosShortsPrefsBacking : ShortsPrefsBacking {
    private val defaults = NSUserDefaults.standardUserDefaults
    private fun namespaced(key: String) = "email.kevinphillips.biblebible.shorts.$key"

    override fun getString(key: String): String? = defaults.stringForKey(namespaced(key))
    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = namespaced(key))
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        val k = namespaced(key)
        return if (defaults.objectForKey(k) != null) defaults.integerForKey(k).toInt() else defaultValue
    }

    override fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = namespaced(key))
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        val k = namespaced(key)
        return if (defaults.objectForKey(k) != null) defaults.integerForKey(k) else defaultValue
    }

    override fun putLong(key: String, value: Long) {
        defaults.setInteger(value, forKey = namespaced(key))
    }
}

internal actual fun platformShortsPrefsBacking(): ShortsPrefsBacking = IosShortsPrefsBacking()
