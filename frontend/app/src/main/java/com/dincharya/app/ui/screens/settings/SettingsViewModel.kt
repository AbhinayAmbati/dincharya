package com.dincharya.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dincharya.app.app.Graph
import com.dincharya.app.app.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thin ViewModel over [SettingsStore] — the store already holds Compose
 * state for theme, so this class mostly exposes type-safe actions — plus
 * the local data export.
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

    /**
     * Write the entire local database (tasks + behavioural events) to a
     * dated JSON file in the app's cache dir, ready to be handed to the
     * system share sheet. Local-first means the user OWNS this data — the
     * app must be able to hand it over in a readable, portable form.
     *
     * @return the file to share.
     */
    suspend fun exportData(): File = withContext(Dispatchers.IO) {
        val repo = Graph.repository

        val tasksJson = JSONArray()
        // Full snapshot: every task, done or not.
        val epoch = java.util.Calendar.getInstance().apply {
            set(1970, java.util.Calendar.JANUARY, 1)
        }.timeInMillis
        val allTasks = repo.pendingTasksOnce() +
            repo.completedBetween(epoch, Long.MAX_VALUE).first()
        for (task in allTasks) {
            tasksJson.put(
                JSONObject()
                    .put("id", task.id)
                    .put("title", task.title)
                    .put("category", task.category)
                    .put("priority", task.priority)
                    .put("estimatedMinutes", task.estimatedMinutes)
                    .put("scheduledAt", task.scheduledAt ?: JSONObject.NULL)
                    .put("completed", task.isCompleted)
                    .put("completedAt", task.completedAt ?: JSONObject.NULL)
                    .put("createdAt", task.createdAt)
                    .put("note", task.note ?: JSONObject.NULL)
                    .put("repeatRule", task.repeatRule)
            )
        }

        val eventsJson = JSONArray()
        for (event in repo.allEvents()) {
            eventsJson.put(
                JSONObject()
                    .put("taskId", event.taskId)
                    .put("outcome", event.outcome)
                    .put("scheduledAt", event.scheduledAt)
                    .put("occurredAt", event.occurredAt)
                    .put("category", event.category)
                    .put("hourOfDay", event.hourOfDay)
            )
        }

        val export = JSONObject()
            .put("app", "Dincharya")
            .put("exportedAt", System.currentTimeMillis())
            .put("tasks", tasksJson)
            .put("events", eventsJson)

        val dir = File(getApplication<Application>().cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        File(dir, "dincharya-export-$stamp.json").apply {
            writeText(export.toString(2))
        }
    }
}
