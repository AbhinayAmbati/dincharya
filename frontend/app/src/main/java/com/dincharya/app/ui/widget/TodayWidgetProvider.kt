package com.dincharya.app.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.dincharya.app.MainActivity
import com.dincharya.app.R
import com.dincharya.app.data.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The "Today" home-screen widget: the next three pending tasks, each with a
 * one-tap complete button, plus an add-task button in the corner.
 *
 * Monochrome by design — the app's state is carried by weight and shape,
 * never colour, so the widget reads on any wallpaper.
 */
class TodayWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                val tasks = com.dincharya.app.app.Graph.repository.pendingTodayOnce()
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, buildViews(context, tasks))
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Refresh every instance of the widget after a data change. */
        fun pushUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, TodayWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            // Called from IO coroutines (receivers), so blocking briefly is fine.
            kotlinx.coroutines.runBlocking {
                val tasks = com.dincharya.app.app.Graph.repository.pendingTodayOnce()
                ids.forEach { id -> manager.updateAppWidget(id, buildViews(context, tasks)) }
            }
        }

        private fun openAppIntent(context: Context, requestCode: Int, addTask: Boolean): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (addTask) putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_ADD_TASK)
            }
            return PendingIntent.getActivity(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun buildViews(context: Context, tasks: List<TaskEntity>): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_today)

            // "+" button — opens the app straight on the Add Task screen.
            views.setOnClickPendingIntent(R.id.widget_add, openAppIntent(context, 90_001, addTask = true))

            val visible = tasks.take(3)
            if (visible.isEmpty()) {
                views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_empty, View.GONE)
            }

            val rowIds = listOf(R.id.widget_row_1, R.id.widget_row_2, R.id.widget_row_3)
            val titleIds = listOf(R.id.widget_task_1_title, R.id.widget_task_2_title, R.id.widget_task_3_title)
            val checkIds = listOf(R.id.widget_task_1_check, R.id.widget_task_2_check, R.id.widget_task_3_check)

            for (index in 0 until 3) {
                val task = visible.getOrNull(index)
                if (task == null) {
                    views.setViewVisibility(rowIds[index], View.GONE)
                    continue
                }
                views.setViewVisibility(rowIds[index], View.VISIBLE)
                views.setTextViewText(titleIds[index], task.title)

                // Tapping the row opens the app.
                views.setOnClickPendingIntent(rowIds[index], openAppIntent(context, 90_010 + index, addTask = false))

                // Tapping the circle completes the task without opening anything.
                val complete = Intent(context, WidgetActionReceiver::class.java).apply {
                    action = WidgetActionReceiver.ACTION_COMPLETE_TASK
                    putExtra(WidgetActionReceiver.KEY_TASK_ID, task.id)
                }
                val completePi = PendingIntent.getBroadcast(
                    context, task.id.toInt(), complete,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(checkIds[index], completePi)
            }
            return views
        }
    }
}
