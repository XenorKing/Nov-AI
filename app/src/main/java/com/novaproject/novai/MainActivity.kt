package com.novaproject.novai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.novaproject.novai.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(NovAIApp.TAG, "MainActivity.onCreate()")
        enableEdgeToEdge()
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
}