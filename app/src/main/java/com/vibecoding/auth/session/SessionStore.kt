package com.vibecoding.auth.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vibecoding.auth.util.nowUtcMillis
import com.vibecoding.auth.util.parseIso8601Utc

data class SessionData(
    val sessionToken: String,
    val sessionExpiresAt: String
)

class SessionStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREF_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(sessionToken: String, sessionExpiresAt: String) {
        prefs.edit()
            .putString(KEY_TOKEN, sessionToken)
            .putString(KEY_EXPIRES_AT, sessionExpiresAt)
            .apply()
    }

    fun getSessionOrNull(): SessionData? {
        val token = prefs.getString(KEY_TOKEN, null).orEmpty()
        val expiresAt = prefs.getString(KEY_EXPIRES_AT, null).orEmpty()
        if (token.isBlank() || expiresAt.isBlank()) return null
        return SessionData(sessionToken = token, sessionExpiresAt = expiresAt)
    }

    fun isSessionValidNow(): Boolean {
        val session = getSessionOrNull() ?: return false
        val expiresAtMillis = parseIso8601Utc(session.sessionExpiresAt) ?: return false
        if (expiresAtMillis <= nowUtcMillis()) {
            clear()
            return false
        }
        return true
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREF_NAME = "secure_auth_session"
        const val KEY_TOKEN = "session_token"
        const val KEY_EXPIRES_AT = "session_expires_at"
    }
}

