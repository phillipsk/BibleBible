package data.auth

import android.content.Context
import email.kevinphillips.biblebible.cache.DriverFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class SecureTokenStore actual constructor() {
    private val prefs by lazy {
        try {
            DriverFactory.context.getSharedPreferences("biblebible_secure_auth", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    actual suspend fun saveRefreshToken(token: String): Unit = withContext(Dispatchers.IO) {
        prefs?.edit()?.putString(KEY_REFRESH_TOKEN, token)?.apply()
    }

    actual suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) {
        prefs?.getString(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    actual suspend fun clearRefreshToken(): Unit = withContext(Dispatchers.IO) {
        prefs?.edit()?.remove(KEY_REFRESH_TOKEN)?.apply()
    }

    companion object {
        private const val KEY_REFRESH_TOKEN = "bb_refresh_token"
    }
}
