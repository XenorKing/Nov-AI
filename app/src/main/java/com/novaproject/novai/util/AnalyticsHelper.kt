package com.novaproject.novai.util

  import android.os.Bundle
  import com.google.firebase.analytics.FirebaseAnalytics
  import javax.inject.Inject
  import javax.inject.Singleton

  @Singleton
  class AnalyticsHelper @Inject constructor(
      private val analytics: FirebaseAnalytics
  ) {
      /** Called when a user successfully registers. */
      fun logSignUp() {
          analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, Bundle().apply {
              putString(FirebaseAnalytics.Param.METHOD, "email")
          })
      }

      /** Called when a user successfully signs in. */
      fun logLogin() {
          analytics.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle().apply {
              putString(FirebaseAnalytics.Param.METHOD, "email")
          })
      }

      /** Called when the user sends a chat message. */
      fun logMessageSent(modelId: String) {
          analytics.logEvent("message_sent", Bundle().apply {
              putString("model", modelId.ifBlank { "novai_default" })
          })
      }

      /** Called when the AI reply is saved successfully. */
      fun logAiResponse(modelId: String) {
          analytics.logEvent("ai_response_received", Bundle().apply {
              putString("model", modelId.ifBlank { "novai_default" })
          })
      }

      /** Called when a new conversation is created. */
      fun logConversationCreated() {
          analytics.logEvent("conversation_created", null)
      }

      /** Called when the user changes an AI setting. */
      fun logSettingsChanged(settingName: String) {
          analytics.logEvent("settings_changed", Bundle().apply {
              putString("setting", settingName)
          })
      }

      /** Called when the user deletes their account. */
      fun logAccountDeleted() {
          analytics.logEvent("account_deleted", null)
      }
  }
  