package com.novaproject.novai.util

  import android.content.Context
  import androidx.security.crypto.EncryptedSharedPreferences
  import androidx.security.crypto.MasterKeys
  import dagger.hilt.android.qualifiers.ApplicationContext
  import javax.inject.Inject
  import javax.inject.Singleton

  /**
   * Stores the OpenRouter API token in EncryptedSharedPreferences (AES-256-GCM)
   * instead of Firestore, so it never leaves the device in plaintext.
   *
   * Vuln-2 fix: sensitive user credentials must not be persisted in cloud storage.
   *
   * Uses security-crypto:1.0.0 stable API (MasterKeys, not MasterKey.Builder).
   */
  @Singleton
  class TokenSecureStore @Inject constructor(
      @ApplicationContext private val context: Context
  ) {
      private val masterKeyAlias by lazy {
          MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
      }

      private val prefs by lazy {
          EncryptedSharedPreferences.create(
              PREFS_FILE,
              masterKeyAlias,
              context,
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
  