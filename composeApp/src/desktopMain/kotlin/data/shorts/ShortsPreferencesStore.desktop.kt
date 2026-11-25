package data.shorts

import email.kevinphillips.biblebible.cache.desktopAppDataDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties

private class DesktopShortsPrefsBacking : ShortsPrefsBacking {
    private val prefsFile: Path by lazy {
        desktopAppDataDir().toAbsolutePath().resolve("preferences.properties")
    }

    private val properties: Properties by lazy {
        Properties().apply {
            runCatching {
                if (Files.isRegularFile(prefsFile)) {
                    Files.newInputStream(prefsFile).use { load(it) }
                }
            }
        }
    }

    @Synchronized
    private fun persist() {
        runCatching {
            val parent = prefsFile.parent
            if (parent != null) {
                Files.createDirectories(parent)
            }
            val tmpFile = parent?.resolve("preferences.properties.tmp")
            if (tmpFile != null) {
                Files.newOutputStream(tmpFile).use { properties.store(it, null) }
                runCatching {
                    Files.move(tmpFile, prefsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                }.getOrElse {
                    Files.move(tmpFile, prefsFile, StandardCopyOption.REPLACE_EXISTING)
                }
            } else {
                Files.newOutputStream(prefsFile).use { properties.store(it, null) }
            }
        }
    }

    @Synchronized
    override fun getString(key: String): String? = properties.getProperty(key)

    @Synchronized
    override fun putString(key: String, value: String) {
        properties.setProperty(key, value)
        persist()
    }

    @Synchronized
    override fun getInt(key: String, defaultValue: Int): Int =
        properties.getProperty(key)?.toIntOrNull() ?: defaultValue

    @Synchronized
    override fun putInt(key: String, value: Int) {
        properties.setProperty(key, value.toString())
        persist()
    }

    @Synchronized
    override fun getLong(key: String, defaultValue: Long): Long =
        properties.getProperty(key)?.toLongOrNull() ?: defaultValue

    @Synchronized
    override fun putLong(key: String, value: Long) {
        properties.setProperty(key, value.toString())
        persist()
    }
}

internal actual fun platformShortsPrefsBacking(): ShortsPrefsBacking = DesktopShortsPrefsBacking()
