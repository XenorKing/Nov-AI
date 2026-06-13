package com.novaproject.novai.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.novaproject.novai.MainActivity
import com.novaproject.novai.R

object AppForegroundState {
    @Volatile var isForeground: Boolean = true
}

object NotificationHelper {
    const val CHANNEL_ID = "ai_replies"
    private var notifId = 3000

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ответы ИИ",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Уведомления когда ИИ ответил на ваш вопрос" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun showAiReply(context: Context, aiName: String, preview: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.novai_logo)
                .setContentTitle("$aiName ответил")
                .setContentText(preview.take(120))
                .setStyle(android.app.Notification.BigTextStyle().bigText(preview.take(300)))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
        } else return

        context.getSystemService(NotificationManager::class.java).notify(notifId++, notification)
    }
}
