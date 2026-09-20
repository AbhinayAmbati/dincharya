package com.dincharya.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dincharya.app.MainActivity
import com.dincharya.app.R
import com.dincharya.app.data.TaskEntity

/**
 * Builds and shows reminder notifications.
 *
 * The notification carries two direct actions — Complete and Snooze — so
 * the user never has to open the app to act on a reminder. All taps are
 * routed to [ReminderActionReceiver].
 */
object Notifications {

    const val CHANNEL_ID = "dincharya_reminders"

    /** Create the notification channel (required on API 26+, which is our minSdk). */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_description)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Show the reminder notification for [task].
     *
     * Request codes are derived from the task id so different tasks (and the
     * two actions of the same task) never collide in PendingIntent lookup.
     */
    fun showReminder(context: Context, task: TaskEntity) {
        ensureChannel(context)

        // Tap -> open the app on the Today screen.
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context, task.id.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // "Done" button -> broadcast to mark the task complete.
        val completePi = PendingIntent.getBroadcast(
            context, (task.id * 2).toInt(),
            ReminderActionReceiver.intentFor(context, task.id, ReminderActionReceiver.ACTION_COMPLETE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // "Snooze 10 min" button -> broadcast to push the reminder later.
        val snoozePi = PendingIntent.getBroadcast(
            context, (task.id * 2 + 1).toInt(),
            ReminderActionReceiver.intentFor(context, task.id, ReminderActionReceiver.ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText(context.getString(R.string.notif_channel_description))
            .setContentIntent(openPi)
            .addAction(0, context.getString(R.string.notif_action_complete), completePi)
            .addAction(0, context.getString(R.string.notif_action_snooze), snoozePi)
            .setAutoCancel(true)
            .build()

        // On Android 13+ the POST_NOTIFICATIONS permission may be denied;
        // a missing permission must never crash the reminder worker.
        try {
            NotificationManagerCompat.from(context).notify(task.id.toInt(), notification)
        } catch (_: SecurityException) {
            // Permission denied — reminder silently skipped. The task stays
            // visible in the Today list either way.
        }
    }
}
