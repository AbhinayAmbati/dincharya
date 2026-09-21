package com.dincharya.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.dincharya.app.app.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles the "Done", "Snooze 10 min" and "Snooze 1 hour" buttons on a
 * reminder notification — so the user can act without opening the app.
 *
 * The notification is dismissed synchronously in [onReceive], BEFORE any
 * database work starts: the visual response to the tap must never depend on
 * how long the DB takes — or on it failing. (BroadcastReceiver.onReceive
 * must not block, so the DB work is dispatched to a coroutine and [goAsync]
 * keeps the process alive until it finishes.)
 */
class ReminderActionReceiver : BroadcastReceiver() {

    // Dedicated scope; the receiver is short-lived so we never cancel it
    // explicitly — every launched job finishes in milliseconds.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getLongExtra(KEY_TASK_ID, -1L)
        if (taskId == -1L) return

        // 1. Dismiss the notification immediately — a stuck-looking
        //    notification makes users tap again, and every extra tap is a
        //    duplicate interaction the learning layer would have to digest.
        try {
            NotificationManagerCompat.from(context).cancel(taskId.toInt())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied — nothing to cancel visually.
        }

        // 2. Do the actual work asynchronously.
        val pendingResult = goAsync()
        scope.launch {
            try {
                val repository = Graph.repository
                val task = repository.taskById(taskId)
                if (task != null) {
                    when (action) {
                        ACTION_COMPLETE -> {
                            // completeTask() is idempotent: if the task was
                            // already completed (by another notification or
                            // in the app), this is a quiet no-op.
                            repository.completeTask(task)
                            // Defensive: drop any follow-up reminder work.
                            ReminderScheduler.cancel(context, taskId)
                        }
                        ACTION_SNOOZE -> {
                            val minutes = intent.getIntExtra(
                                KEY_SNOOZE_MINUTES, Notifications.SNOOZE_SHORT_MINUTES
                            )
                            val newTime = repository.snoozeTask(task, minutes)
                            // Book the follow-up reminder for the snoozed time.
                            ReminderScheduler.schedule(context, taskId, newTime)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.dincharya.app.action.COMPLETE"
        const val ACTION_SNOOZE = "com.dincharya.app.action.SNOOZE"
        const val KEY_TASK_ID = "taskId"
        const val KEY_SNOOZE_MINUTES = "snoozeMinutes"
        const val SNOOZE_MINUTES = 10

        /** Helper so callers build intents with matching extras. */
        fun intentFor(context: Context, taskId: Long, action: String): Intent =
            Intent(context, ReminderActionReceiver::class.java).apply {
                this.action = action
                putExtra(KEY_TASK_ID, taskId)
            }

        /** A snooze intent that carries its duration (10 or 60 minutes). */
        fun snoozeIntentFor(context: Context, taskId: Long, minutes: Int): Intent =
            intentFor(context, taskId, ACTION_SNOOZE).apply {
                putExtra(KEY_SNOOZE_MINUTES, minutes)
            }
    }
}
