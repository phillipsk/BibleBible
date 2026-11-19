package data.auth

import platform.Foundation.NSUserDefaults

actual class SecureTokenStore actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual suspend fun saveRefreshToken(token: String) {
        defaults.setObject(token, forKey = KEY_REFRESH_TOKEN)
        defaults.synchronize()
    }

    actual suspend fun getRefreshToken(): String? {
        return defaults.stringForKey(KEY_REFRESH_TOKEN)?.takeIf { it.isNotBlank() }
    }

    actual suspend fun clearRefreshToken() {
        defaults.removeObjectForKey(KEY_REFRESH_TOKEN)
        defaults.synchronize()
    }

    companion object {
        private const val KEY_REFRESH_TOKEN = "email.kevinphillips.biblebible.refresh_token"
    }
}
