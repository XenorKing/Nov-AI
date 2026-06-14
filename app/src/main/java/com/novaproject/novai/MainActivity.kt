package com.novaproject.novai

  import android.Manifest
  import android.content.pm.PackageManager
  import android.os.Build
  import android.os.Bundle
  import android.util.Log
  import androidx.activity.ComponentActivity
  import androidx.activity.compose.setContent
  import androidx.activity.enableEdgeToEdge
  import androidx.activity.result.contract.ActivityResultContracts
  import androidx.core.content.ContextCompat
  import com.novaproject.novai.navigation.AppNavigation
  import dagger.hilt.android.AndroidEntryPoint

  @AndroidEntryPoint
  class MainActivity : ComponentActivity() {

      /**
       * Runtime permission launcher for POST_NOTIFICATIONS (Android 13+).
       * On older versions the system grants this automatically via the manifest.
       */
      private val notificationPermissionLauncher =
          registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
              Log.d(NovAIApp.TAG, "POST_NOTIFICATIONS permission granted=$granted")
          }

      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          Log.d(NovAIApp.TAG, "MainActivity.onCreate()")
          enableEdgeToEdge()
          requestNotificationPermissionIfNeeded()
          setContent {
              // NovAITheme is applied inside AppNavigation so the accent colour
              // updates live whenever settings change — no restart required.
              AppNavigation()
          }
      }

      override fun onStart() {
          super.onStart()
          Log.d(NovAIApp.TAG, "MainActivity.onStart()")
      }

      override fun onResume() {
          super.onResume()
          Log.d(NovAIApp.TAG, "MainActivity.onResume()")
      }

      override fun onDestroy() {
          super.onDestroy()
          Log.d(NovAIApp.TAG, "MainActivity.onDestroy()")
      }

      // ── Private helpers ────────────────────────────────────────────────────────

      /**
       * On Android 13 (API 33) and above, POST_NOTIFICATIONS is a runtime
       * permission. Without it the system silently drops every notification —
       * both local ones and FCM messages — no matter what the manifest says.
       */
      private fun requestNotificationPermissionIfNeeded() {
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
          if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
              == PackageManager.PERMISSION_GRANTED
          ) return
          notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
  }
  