package com.dincharya.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dincharya.app.app.Graph

/**
 * Fired by WorkManager when a task's reminder time arrives.
 *
 * Reads the task from the local DB (it may have been completed or deleted
 * since scheduling — in which case we exit silently) and shows the
 * notification with Complete / Snooze actions.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId == -1L) return Result.failure()

        val task = Graph.repository.taskById(taskId) ?: return Result.success()
        if (task.isCompleted) return Result.success()

        // Respect the user's global notification toggle.
        if (!Graph.settings.notificationsEnabled) return Result.success()

        Notifications.showReminder(applicationContext, task)
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "taskId"
    }
}
