package com.novaproject.novai

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.novaproject.novai.navigation.AppNavigation
import com.novaproject.novai.util.AppForegroundState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(NovAIApp.TAG, "MainActivity.onCreate()")
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            AppNavigation()
        }
    }

    override fun onStart() {
        super.onStart()
        AppForegroundState.isForeground = true
        Log.d(NovAIApp.TAG, "MainActivity.onStart()")
    }

    override fun onStop() {
        super.onStop()
        AppForegroundState.isForeground = false
        Log.d(NovAIApp.TAG, "MainActivity.onStop()")
    }

    override fun onResume() {
        super.onResume()
        Log.d(NovAIApp.TAG, "MainActivity.onResume()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(NovAIApp.TAG, "MainActivity.onDestroy()")
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }
}
