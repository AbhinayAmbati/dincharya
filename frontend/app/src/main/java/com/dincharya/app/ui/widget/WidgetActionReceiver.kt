package com.dincharya.app.ui.widget

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
 * Handles the complete-circle taps on the home-screen widget — the same
 * contract as the notification Done button: complete idempotently, dismiss
 * any live notification for the task, then refresh every widget instance.
 */
class WidgetActionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE_TASK) return
        val taskId = intent.getLongExtra(KEY_TASK_ID, -1L)
        if (taskId == -1L) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val task = Graph.repository.taskById(taskId)
                if (task != null) {
                    Graph.repository.completeTask(task)
                    try {
                        NotificationManagerCompat.from(context).cancel(taskId.toInt())
                    } catch (_: SecurityException) {
                        // Nothing to cancel visually.
                    }
                }
                TodayWidgetProvider.pushUpdate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_TASK = "com.dincharya.app.action.widget.COMPLETE"
        const val KEY_TASK_ID = "taskId"
    }
}
