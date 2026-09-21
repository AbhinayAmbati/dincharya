package com.dincharya.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dincharya.app.MainActivity
import com.dincharya.app.R
import com.dincharya.app.data.TaskEntity

/**
 * Builds and shows reminder notifications.
 *
 * The reminder carries three direct actions — Done, Snooze 10 min and
 * Snooze 1 hour — so the user never has to open the app to act on one.
 * All taps are routed to [ReminderActionReceiver]; the snooze duration
 * travels as an intent extra so one receiver serves both buttons.
 */
object Notifications {

    const val CHANNEL_ID = "dincharya_reminders"

    /** Fixed notification ids for the daily briefs (task ids start at 1). */
    const val MORNING_BRIEF_ID = 100_001
    const val EVENING_BRIEF_ID = 100_002

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
     * three actions of the same task) never collide in PendingIntent lookup.
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
            context, (task.id * 3).toInt(),
            ReminderActionReceiver.intentFor(context, task.id, ReminderActionReceiver.ACTION_COMPLETE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // "10 min" snooze button.
        val snooze10Pi = PendingIntent.getBroadcast(
            context, (task.id * 3 + 1).toInt(),
            ReminderActionReceiver.snoozeIntentFor(context, task.id, SNOOZE_SHORT_MINUTES),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // "1 hour" snooze button.
        val snooze60Pi = PendingIntent.getBroadcast(
            context, (task.id * 3 + 2).toInt(),
            ReminderActionReceiver.snoozeIntentFor(context, task.id, SNOOZE_LONG_MINUTES),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText(context.getString(R.string.notif_reminder_text))
            .setContentIntent(openPi)
            .addAction(0, context.getString(R.string.notif_action_complete), completePi)
            .addAction(0, context.getString(R.string.notif_action_snooze_10), snooze10Pi)
            .addAction(0, context.getString(R.string.notif_action_snooze_60), snooze60Pi)
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

    /** Show a plain, actionless text notification (used by the daily briefs). */
    fun showBrief(context: Context, id: Int, title: String, text: String) {
        ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context, id, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied — nothing more we can do.
        }
    }

    /** Snooze durations offered directly on the notification. */
    const val SNOOZE_SHORT_MINUTES = 10
    const val SNOOZE_LONG_MINUTES = 60
}
