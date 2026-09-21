package com.dincharya.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dincharya.app.R
import com.dincharya.app.app.Graph
import com.dincharya.app.data.EventOutcome
import com.dincharya.app.data.TaskEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * The two daily check-ins that keep the Dincharya habit alive:
 *  - a morning brief ("you have 4 tasks today, first at 10:30")
 *  - an evening nudge to do the 30-second review.
 *
 * Scheduled as two unique periodic works (24h period, first fire at the
 * next 08:00 / 21:00). The evening line is generated from the day's events,
 * so it can honestly say "you finished 3 of 5".
 */
class DailyBriefWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!Graph.settings.notificationsEnabled) return Result.success()
        val isMorning = inputData.getString(KEY_KIND) == KIND_MORNING

        if (isMorning) {
            // New day, clean slate: missed recurring habits are rolled
            // forward to a realistic slot today (see repository docs)
            // before the brief is composed.
            runCatching { Graph.repository.rollMissedRecurringTasks() }
            val tasks = Graph.repository.pendingTasksOnce()
            val today = tasks.filter { it.scheduledAt != null && it.startsToday() }
            val text = if (today.isEmpty()) {
                applicationContext.getString(R.string.brief_morning_empty)
            } else {
                val first = today.first()
                val cal = Calendar.getInstance().apply { timeInMillis = first.scheduledAt!! }
                applicationContext.getString(
                    R.string.brief_morning_body,
                    today.size,
                    "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)),
                )
            }
            Notifications.showBrief(
                applicationContext,
                Notifications.MORNING_BRIEF_ID,
                applicationContext.getString(R.string.brief_morning_title),
                text,
            )
        } else {
            val events = Graph.repository.allEvents()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val dayStart = cal.timeInMillis
            val doneToday = events.count {
                it.outcome == EventOutcome.COMPLETED.name && it.occurredAt >= dayStart
            }
            val text = applicationContext.getString(R.string.brief_evening_body, doneToday)
            Notifications.showBrief(
                applicationContext,
                Notifications.EVENING_BRIEF_ID,
                applicationContext.getString(R.string.brief_evening_title),
                text,
            )
        }
        return Result.success()
    }

    companion object {
        const val KEY_KIND = "kind"
        const val KIND_MORNING = "morning"
        const val KIND_EVENING = "evening"

        /** Book (idempotently) both daily briefs on their fixed times. */
        fun scheduleDailyBriefs(context: Context) {
            val work = WorkManager.getInstance(context)

            work.enqueueUniquePeriodicWork(
                "morning_brief",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DailyBriefWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(delayUntil(8, 0), TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(KEY_KIND to KIND_MORNING))
                    .build(),
            )

            work.enqueueUniquePeriodicWork(
                "evening_brief",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DailyBriefWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(delayUntil(21, 0), TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(KEY_KIND to KIND_EVENING))
                    .build(),
            )
        }

        /** Milliseconds from now until the next [hour]:[minute]. */
        private fun delayUntil(hour: Int, minute: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }

        private fun TaskEntity.startsToday(): Boolean {
            val cal = Calendar.getInstance()
            val dayStart = cal.clone() as Calendar
            dayStart.set(Calendar.HOUR_OF_DAY, 0); dayStart.set(Calendar.MINUTE, 0)
            dayStart.set(Calendar.SECOND, 0); dayStart.set(Calendar.MILLISECOND, 0)
            val dayEnd = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val at = scheduledAt ?: return false
            return at in dayStart.timeInMillis until dayEnd.timeInMillis
        }
    }
}
