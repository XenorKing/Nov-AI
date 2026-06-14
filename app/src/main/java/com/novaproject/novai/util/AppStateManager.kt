package com.novaproject.novai.util

  import android.app.Activity
  import android.app.Application
  import android.os.Bundle

  /**
   * Tracks whether the app is currently in the foreground.
   * Register as ActivityLifecycleCallbacks in Application.onCreate().
   *
   * Usage: AppStateManager.isAppInForeground
   */
  object AppStateManager : Application.ActivityLifecycleCallbacks {

      @Volatile
      var isAppInForeground: Boolean = false
          private set

      private var startedCount = 0

      override fun onActivityStarted(activity: Activity) {
          startedCount++
          isAppInForeground = true
      }

      override fun onActivityStopped(activity: Activity) {
          startedCount--
          if (startedCount == 0) isAppInForeground = false
      }

      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
      override fun onActivityResumed(activity: Activity) {}
      override fun onActivityPaused(activity: Activity) {}
      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
      override fun onActivityDestroyed(activity: Activity) {}
  }
  