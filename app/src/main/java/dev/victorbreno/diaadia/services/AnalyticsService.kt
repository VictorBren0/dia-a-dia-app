package dev.victorbreno.diaadia.services

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsService {

    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (analytics == null) {
            analytics = FirebaseAnalytics.getInstance(context)
        }
    }

    fun logLogin(method: String) {
        analytics?.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        })
    }

    fun logSignUp(method: String) {
        analytics?.logEvent(FirebaseAnalytics.Event.SIGN_UP, Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        })
    }

    fun logScreenView(screenName: String) {
        analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        })
    }

    fun logReflectionCreated() {
        analytics?.logEvent("reflection_created", null)
    }

    fun logReflectionEdited() {
        analytics?.logEvent("reflection_edited", null)
    }

    fun logProfileUpdated() {
        analytics?.logEvent("profile_updated", null)
    }

    fun logProfilePhotoChanged() {
        analytics?.logEvent("profile_photo_changed", null)
    }
}
