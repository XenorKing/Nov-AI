package com.novaproject.novai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.novaproject.novai.util.AppStateManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NovAIApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
        createNotificationChannel()
        subscribeToFcmTopics()
        registerActivityLifecycleCallbacks(AppStateManager)
        Log.d(TAG, "NovAIApp.onCreate — app started")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "NovAI Уведомления",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомления об ответах ИИ и новостях от Nova Project"
            enableLights(true)
            enableVibration(true)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun subscribeToFcmTopics() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) Log.d(TAG, "FCM subscribed to topic: all_users")
                else Log.w(TAG, "FCM topic subscription failed", task.exception)
            }
    }

    companion object {
        const val TAG = "NovAI"
        const val CHANNEL_ID = "novai_notifications"
    }
}
