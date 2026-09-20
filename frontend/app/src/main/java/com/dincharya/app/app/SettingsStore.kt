package com.dincharya.app.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * User preferences, backed by SharedPreferences (v1 is local-only; the value
 * set is tiny, so a DataStore migration is not worth it yet).
 *
 * [themeModeState] is a Compose state so the UI re-themes instantly when the
 * user flips the dark-mode setting — no restart needed.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("dincharya_settings", Context.MODE_PRIVATE)

    // ---- Theme ----

    /** One of [THEME_SYSTEM], [THEME_LIGHT], [THEME_DARK]. */
    var themeMode: String
        get() = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) {
            prefs.edit().putString(KEY_THEME, value).apply()
            themeModeState = value
        }

    /** Observable mirror of [themeMode] for Compose. */
    var themeModeState by mutableStateOf(themeMode)
        private set

    // ---- Onboarding ----

    /** True once the user has seen the intro + chronotype quiz. */
    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    /**
     * Chronotype seed from the onboarding quiz: "early", "neutral" or "late".
     * Used as the prior for reminder timing until real behaviour takes over.
     */
    var chronotype: String
        get() = prefs.getString(KEY_CHRONOTYPE, "neutral") ?: "neutral"
        set(value) = prefs.edit().putString(KEY_CHRONOTYPE, value).apply()

    // ---- Notifications ----

    /** Master switch for all reminders. */
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    // ---- Evening review ----

    /** Epoch-millis "day stamp" of the last evening review the user completed. */
    var lastReviewDayStart: Long
        get() = prefs.getLong(KEY_LAST_REVIEW, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_REVIEW, value).apply()

    /** Free-text reflection saved in the evening review. */
    var reviewNote: String
        get() = prefs.getString(KEY_REVIEW_NOTE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_REVIEW_NOTE, value).apply()

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        private const val KEY_THEME = "theme_mode"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_CHRONOTYPE = "chronotype"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_LAST_REVIEW = "last_review_day_start"
        private const val KEY_REVIEW_NOTE = "review_note"
    }
}
