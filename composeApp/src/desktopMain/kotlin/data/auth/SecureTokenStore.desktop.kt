package data.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.prefs.Preferences

actual class SecureTokenStore actual constructor() {
    private val prefs: Preferences by lazy {
        Preferences.userNodeForPackage(SecureTokenStore::class.java)
    }

    actual suspend fun saveRefreshToken(token: String): Unit = withContext(Dispatchers.IO) {
        prefs.put(KEY_REFRESH_TOKEN, token)
        prefs.flush()
    }

    actual suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) {
        prefs.get(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    actual suspend fun clearRefreshToken(): Unit = withContext(Dispatchers.IO) {
        prefs.remove(KEY_REFRESH_TOKEN)
        prefs.flush()
    }

    companion object {
        private const val KEY_REFRESH_TOKEN = "bb_refresh_token"
    }
}
