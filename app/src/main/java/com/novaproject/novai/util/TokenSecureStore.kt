package com.novaproject.novai.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the OpenRouter API token in EncryptedSharedPreferences (AES-256-GCM)
 * instead of Firestore, so it never leaves the device in plaintext.
 *
 * Falls back to regular SharedPreferences if the Android Keystore is
 * unavailable or corrupted (e.g. after backup-restore, firmware quirks on
 * Samsung/Xiaomi). The fallback is still MODE_PRIVATE — less secure than
 * encryption, but prevents a crash that would otherwise make the app unusable.
 */
@Singleton
class TokenSecureStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy { buildPrefs() }

    private fun buildPrefs(): SharedPreferences {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_FILE,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "EncryptedSharedPreferences init failed — using plain fallback", e)
            // Delete potentially corrupted encrypted file to avoid repeated failures.
            try { context.deleteSharedPreferences(PREFS_FILE) } catch (_: Exception) {}
            context.getSharedPreferences(FALLBACK_PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    fun saveToken(token: String) {
        try {
            prefs.edit().putString(KEY_TOKEN, token).apply()
        } catch (e: Exception) {
            Log.e(TAG, "saveToken failed", e)
        }
    }

    fun getToken(): String = try {
        prefs.getString(KEY_TOKEN, "") ?: ""
    } catch (e: Exception) {
        Log.e(TAG, "getToken failed", e)
        ""
    }

    fun clearToken() {
        try {
            prefs.edit().remove(KEY_TOKEN).apply()
        } catch (e: Exception) {
            Log.e(TAG, "clearToken failed", e)
        }
    }

    companion object {
        private const val TAG = "TokenSecureStore"
        private const val PREFS_FILE = "novai_secure_prefs"
        private const val FALLBACK_PREFS_FILE = "novai_prefs_fallback"
        private const val KEY_TOKEN = "openrouter_token"
    }
}
