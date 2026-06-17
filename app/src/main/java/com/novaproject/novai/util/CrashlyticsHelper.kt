package com.novaproject.novai.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashlyticsHelper @Inject constructor() {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    fun setUserId(uid: String) {
        crashlytics.setUserId(uid)
    }

    fun clearUserId() {
        crashlytics.setUserId("")
    }

    fun setUserEmail(email: String) {
        crashlytics.setCustomKey("user_email", email)
    }

    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    fun recordException(tag: String, throwable: Throwable) {
        crashlytics.setCustomKey("error_tag", tag)
        crashlytics.recordException(throwable)
    }

    fun log(message: String) {
        crashlytics.log(message)
    }
}
