package data.shorts

internal interface ShortsPrefsBacking {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun putLong(key: String, value: Long)
}

internal expect fun platformShortsPrefsBacking(): ShortsPrefsBacking

internal class InMemoryShortsPrefsBacking : ShortsPrefsBacking {
    private val strings = mutableMapOf<String, String>()
    private val ints = mutableMapOf<String, Int>()
    private val longs = mutableMapOf<String, Long>()

    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String) { strings[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = ints[key] ?: defaultValue
    override fun putInt(key: String, value: Int) { ints[key] = value }
    override fun getLong(key: String, defaultValue: Long): Long = longs[key] ?: defaultValue
    override fun putLong(key: String, value: Long) { longs[key] = value }
}
