package com.novaproject.novai.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the OpenRouter API token in EncryptedSharedPreferences (AES-256-GCM)
 * instead of Firestore, so it never leaves the device in plaintext.
 *
 * Vuln-2 fix: sensitive user credentials must not be persisted in cloud storage.
 */
@Singleton
class TokenSecureStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_OPENROUTER_TOKEN, token).apply()
    }

    fun getToken(): String = prefs.getString(KEY_OPENROUTER_TOKEN, "") ?: ""

    fun clearToken() {
        prefs.edit().remove(KEY_OPENROUTER_TOKEN).apply()
    }

    companion object {
        private const val PREFS_FILE = "novai_secure_prefs"
        private const val KEY_OPENROUTER_TOKEN = "openrouter_token"
    }
}
