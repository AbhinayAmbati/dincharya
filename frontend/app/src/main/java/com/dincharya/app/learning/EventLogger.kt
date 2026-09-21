package com.dincharya.app.learning

import android.util.Log
import com.dincharya.app.data.EventOutcome
import com.dincharya.app.data.TaskEntity
import com.dincharya.app.data.TaskEventDao
import com.dincharya.app.data.TaskEventEntity
import java.util.Calendar

/**
 * Writes one [TaskEventEntity] whenever a task interaction happens.
 *
 * This is the app's sensory system: nothing is learned from data that was
 * never logged, so EVERY user action on a task must pass through here.
 */
class EventLogger(private val eventDao: TaskEventDao) {

    /**
     * Record a completed focus session: actual minutes spent vs the task's
     * estimate. Feeds the estimate calibration ("you take 2x longer than
     * you think for Learning").
     */
    suspend fun logFocused(task: TaskEntity, estimatedMinutes: Int, actualMinutes: Int) {
        try {
            val now = System.currentTimeMillis()
            val hourOfDay = Calendar.getInstance().apply { timeInMillis = now }
                .get(Calendar.HOUR_OF_DAY)
            eventDao.insert(
                TaskEventEntity(
                    taskId = task.id,
                    outcome = EventOutcome.FOCUSED.name,
                    scheduledAt = task.scheduledAt ?: now,
                    occurredAt = now,
                    category = task.category,
                    hourOfDay = hourOfDay,
                    detail = "$estimatedMinutes/$actualMinutes",
                )
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to log focus event", t)
        }
    }

    /**
     * Record an outcome for [task].
     *
     * @param outcome what the user did (created / completed / snoozed / ...).
     */
    suspend fun log(task: TaskEntity, outcome: EventOutcome) {
        val now = System.currentTimeMillis()
        // The hour of day is the single most important learning feature —
        // "does this user complete things at 14:00 or 20:00?" — so we
        // snapshot it with the event.
        val hourOfDay = Calendar.getInstance().apply {
            timeInMillis = now
        }.get(Calendar.HOUR_OF_DAY)

        try {
            eventDao.insert(
                TaskEventEntity(
                    taskId = task.id,
                    outcome = outcome.name,
                    scheduledAt = task.scheduledAt ?: now,
                    occurredAt = now,
                    category = task.category,
                    hourOfDay = hourOfDay,
                )
            )
        } catch (t: Throwable) {
            // Logging must never break the user's action; degrade quietly.
            Log.w(TAG, "Failed to log task event", t)
        }
    }

    private companion object {
        const val TAG = "EventLogger"
    }
}
