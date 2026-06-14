package com.novaproject.novai.service

  import android.app.NotificationManager
  import android.app.PendingIntent
  import android.content.Context
  import android.content.Intent
  import android.util.Log
  import androidx.core.app.NotificationCompat
  import com.google.firebase.auth.FirebaseAuth
  import com.google.firebase.firestore.FieldValue
  import com.google.firebase.firestore.FirebaseFirestore
  import com.google.firebase.firestore.SetOptions
  import com.google.firebase.messaging.FirebaseMessagingService
  import com.google.firebase.messaging.RemoteMessage
  import com.novaproject.novai.MainActivity
  import com.novaproject.novai.NovAIApp
  import java.util.concurrent.atomic.AtomicInteger

  class NovAIMessagingService : FirebaseMessagingService() {

      private val notifIdCounter = AtomicInteger(1000)

      /**
       * Called when FCM assigns a new token to this device.
       * Save it to Firestore so the user can be targeted individually if needed.
       */
      override fun onNewToken(token: String) {
          Log.d(TAG, "FCM new token received")
          val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
          FirebaseFirestore.getInstance()
              .collection("users").document(uid)
              .set(
                  mapOf(
                      "fcmToken" to token,
                      "fcmUpdatedAt" to FieldValue.serverTimestamp()
                  ),
                  SetOptions.merge()
              )
      }

      /**
       * Called when a push message arrives.
       * Firebase shows notification-payload messages automatically when the app
       * is in the background; this handler catches data-only messages and any
       * foreground notification.
       */
      override fun onMessageReceived(remoteMessage: RemoteMessage) {
          val title = remoteMessage.notification?.title
              ?: remoteMessage.data["title"]
              ?: "NovAI"
          val body = remoteMessage.notification?.body
              ?: remoteMessage.data["body"]
              ?: return

          showNotification(title, body)
      }

      private fun showNotification(title: String, body: String) {
          val intent = Intent(this, MainActivity::class.java).apply {
              addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
          }
          val pendingIntent = PendingIntent.getActivity(
              this, 0, intent,
              PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
          )

          val notification = NotificationCompat.Builder(this, NovAIApp.CHANNEL_ID)
              .setSmallIcon(com.novaproject.novai.R.mipmap.ic_launcher)
              .setContentTitle(title)
              .setContentText(body)
              .setStyle(NotificationCompat.BigTextStyle().bigText(body))
              .setAutoCancel(true)
              .setPriority(NotificationCompat.PRIORITY_HIGH)
              .setContentIntent(pendingIntent)
              .build()

          val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
          manager.notify(notifIdCounter.getAndIncrement(), notification)
      }

      companion object {
          private const val TAG = "NovAIMessaging"
      }
  }
  