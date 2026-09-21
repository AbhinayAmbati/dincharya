package com.dincharya.app.learning

import com.dincharya.app.data.EventOutcome
import com.dincharya.app.data.TaskEntity
import com.dincharya.app.data.TaskEventEntity
import java.util.Calendar

/**
 * A suggested change to a task's schedule, always paired with a human-readable
 * [reason]. The app NEVER silently reschedules — every adaptation is shown to
 * the user with its reason and can be accepted or declined.
 */
data class Adaptation(
    val taskId: Long,
    val newScheduledAt: Long,
    val reason: String,
)

/**
 * Stage-1 learning: the rule-based adaptation engine.
 *
 * Pure Kotlin, no Android dependencies, no database access — it takes the
 * task and its event history as plain values and returns suggestions. This
 * keeps it unit-testable (see AdaptationEngineTest) and lets the on-device
 * ML model of a later phase slot into the same interface.
 *
 * Rules implemented:
 *  1. A task snoozed [SNOOZE_THRESHOLD] times is suggested 2 hours earlier
 *     (never before [EARLIEST_HOUR] in the morning).
 *  2. A task scheduled into a historically weak time window is suggested to
 *     move to the user's best completion hour (needs [MIN_SAMPLES_PER_HOUR]
 *     observations before it dares to suggest anything).
 */
object AdaptationEngine {

    /** Snoozes before Rule 1 kicks in. */
    const val SNOOZE_THRESHOLD = 3

    /** How far Rule 1 shifts a task earlier. */
    const val SHIFT_EARLIER_HOURS = 2

    /** Adaptation never suggests a reminder before this hour. */
    const val EARLIEST_HOUR = 7

    /** Minimum observations per hour before statistics are trusted (Rule 2). */
    const val MIN_SAMPLES_PER_HOUR = 3

    /** Completion rate below which a window counts as "weak". */
    const val LOW_COMPLETION_RATE = 0.4f

    /**
     * Snapshot of how the user behaves in one hour of the day.
     * [total] counts completed + snoozed + ignored events — the three
     * outcomes that carry scheduling information.
     */
    data class HourStat(val completed: Int, val total: Int) {
        val rate: Float get() = if (total == 0) 0f else completed.toFloat() / total
    }

    /**
     * Rule 1: suggest reminding [SHIFT_EARLIER_HOURS] hours earlier for a task
     * the user keeps snoozing. If the new time would fall before
     * [EARLIEST_HOUR], it is floored to [EARLIEST_HOUR]:00.
     *
     * @return null when no suggestion applies (task unscheduled or snoozed
     *         fewer than [SNOOZE_THRESHOLD] times).
     */
    fun suggestShiftEarlier(task: TaskEntity): Adaptation? {
        val scheduled = task.scheduledAt ?: return null
        if (task.snoozeCount < SNOOZE_THRESHOLD) return null

        val target = Calendar.getInstance().apply {
            timeInMillis = scheduled
            add(Calendar.HOUR_OF_DAY, -SHIFT_EARLIER_HOURS)
            if (get(Calendar.HOUR_OF_DAY) < EARLIEST_HOUR) {
                // Would land too early in the morning — clamp instead.
                set(Calendar.HOUR_OF_DAY, EARLIEST_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        return Adaptation(
            taskId = task.id,
            newScheduledAt = target.timeInMillis,
            reason = "Snoozed ${task.snoozeCount} times. Dincharya suggests reminding " +
                "$SHIFT_EARLIER_HOURS hours earlier.",
        )
    }

    /**
     * Completion statistics per hour of day across [events].
     * Only COMPLETED / SNOOZED / IGNORED events count as samples.
     */
    fun completionRateByHour(events: List<TaskEventEntity>): Map<Int, HourStat> {
        val completed = mutableMapOf<Int, Int>()
        val totals = mutableMapOf<Int, Int>()
        for (event in events) {
            when (event.outcome) {
                EventOutcome.COMPLETED.name -> completed.merge(event.hourOfDay, 1, Int::plus)
                EventOutcome.SNOOZED.name, EventOutcome.IGNORED.name -> Unit
                else -> continue // CREATED / RESCHEDULED carry no signal here
            }
            totals.merge(event.hourOfDay, 1, Int::plus)
        }
        return totals.mapValues { (hour, total) ->
            HourStat(completed = completed[hour] ?: 0, total = total)
        }
    }

    /**
     * The hour with the highest completion rate, requiring at least
     * [MIN_SAMPLES_PER_HOUR] observations. Ties are broken by earliest hour
     * (deterministic, and mornings deserve the benefit of the doubt).
     */
    fun bestHour(events: List<TaskEventEntity>): Int? {
        return completionRateByHour(events)
            .filterValues { it.total >= MIN_SAMPLES_PER_HOUR }
            .minWithOrNull(compareByDescending<Map.Entry<Int, HourStat>> { it.value.rate }.thenBy { it.key })
            ?.key
    }

    /**
     * Rule 2: if the task sits in a statistically weak window (rate below
     * [LOW_COMPLETION_RATE], enough samples) and a clearly better hour
     * exists, suggest moving there. Requires no minimum sample count on the
     * current hour beyond the global [MIN_SAMPLES_PER_HOUR] on the best hour.
     *
     * @return null when the data does not support a move.
     */
    fun suggestBetterWindow(task: TaskEntity, events: List<TaskEventEntity>): Adaptation? {
        val scheduled = task.scheduledAt ?: return null
        val stats = completionRateByHour(events)
        val currentHour = Calendar.getInstance().apply {
            timeInMillis = scheduled
        }.get(Calendar.HOUR_OF_DAY)

        val current = stats[currentHour] ?: return null
        if (current.rate >= LOW_COMPLETION_RATE) return null // current window is fine

        val best = bestHour(events) ?: return null
        if (best == currentHour) return null // nowhere better to move it

        val target = Calendar.getInstance().apply {
            timeInMillis = scheduled
            set(Calendar.HOUR_OF_DAY, best)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        return Adaptation(
            taskId = task.id,
            newScheduledAt = target.timeInMillis,
            reason = "You finish tasks around %02d:00 far more often than around %02d:00. Try moving it there.".format(best, currentHour),
        )
    }

    /**
     * Rule 3: "anytime" tasks (no scheduled time) are offered a slot at the
     * user's historically best completion hour — the next occurrence of
     * that hour, today if it is still ahead, tomorrow otherwise.
     *
     * @return null when the task has a time already or the history is too
     *         thin to trust any hour.
     */
    fun suggestScheduleForAnytime(task: TaskEntity, events: List<TaskEventEntity>): Adaptation? {
        if (task.scheduledAt != null) return null
        val best = bestHour(events) ?: return null

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, best)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return Adaptation(
            taskId = task.id,
            newScheduledAt = target.timeInMillis,
            reason = "You finish things around %02d:00 more often than in any other hour. " +
                "want a reminder then?".format(best),
        )
    }

    /**
     * Combined evaluation used by the Today screen: Rule 1 first (cheap and
     * specific), Rule 2 as a fallback, Rule 3 for anything still unscheduled.
     */
    fun evaluate(task: TaskEntity, events: List<TaskEventEntity>): Adaptation? =
        suggestShiftEarlier(task) ?: suggestBetterWindow(task, events)
            ?: suggestScheduleForAnytime(task, events)
}
