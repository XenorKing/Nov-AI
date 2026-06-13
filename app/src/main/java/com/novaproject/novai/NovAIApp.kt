package com.novaproject.novai

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NovAIApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupCrashLogger()
        Log.d(TAG, "NovAIApp.onCreate — app started")
    }

    private fun setupCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "━━━ FATAL CRASH ━━━ thread=${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        const val TAG = "NovAI"
    }
}
