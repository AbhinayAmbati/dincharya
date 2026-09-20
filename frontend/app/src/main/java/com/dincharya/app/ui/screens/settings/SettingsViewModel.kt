package com.dincharya.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.dincharya.app.app.Graph
import com.dincharya.app.app.SettingsStore

/**
 * Thin ViewModel over [SettingsStore] — the store already holds Compose
 * state for theme, so this class mostly exposes type-safe actions.
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    val themeMode: String get() = Graph.settings.themeMode
    val notificationsEnabled: Boolean get() = Graph.settings.notificationsEnabled
    val chronotype: String get() = Graph.settings.chronotype

    fun setTheme(mode: String) {
        Graph.settings.themeMode = mode
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        Graph.settings.notificationsEnabled = enabled
    }

    /** Replay onboarding: flips the flag; navigation reacts to it. */
    fun replayOnboarding() {
        Graph.settings.onboardingDone = false
    }
}
