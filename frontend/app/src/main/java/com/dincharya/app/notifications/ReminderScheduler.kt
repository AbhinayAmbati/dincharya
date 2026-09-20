package com.dincharya.app.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dincharya.app.notifications.ReminderWorker.Companion.KEY_TASK_ID
import java.util.concurrent.TimeUnit

/**
 * Schedules, reschedules and cancels task reminders via WorkManager.
 *
 * Why WorkManager and not exact alarms: reminders here are not
 * minute-critical, and WorkManager survives reboots and app kills without a
 * BOOT_COMPLETED receiver, and behaves well with battery saver. If a later
 * phase needs minute-exact reminders, switch to AlarmManager behind this
 * same object — the rest of the app will not notice.
 */
object ReminderScheduler {

    private const val WORK_TAG = "dincharya_reminder"

    /** Unique work name so a re-schedule replaces the older reminder. */
    private fun uniqueName(taskId: Long) = "reminder_$taskId"

    /**
     * Schedule (or replace) the reminder for [taskId] to fire at
     * [triggerAtMillis]. A time already in the past fires immediately.
     */
    fun schedule(context: Context, taskId: Long, triggerAtMillis: Long) {
        val delayMs = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_TASK_ID to taskId))
            .addTag(WORK_TAG)
            .build()
        // REPLACE: editing/rescheduling a task must not leave a stale reminder.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueName(taskId), ExistingWorkPolicy.REPLACE, request)
    }

    /** Cancel a pending reminder (task deleted or completed). */
    fun cancel(context: Context, taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(taskId))
    }
}
