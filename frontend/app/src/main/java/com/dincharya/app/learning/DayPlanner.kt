package com.dincharya.app.learning

import com.dincharya.app.data.TaskEntity
import com.dincharya.app.data.TaskEventEntity
import java.util.Calendar
import kotlin.math.roundToInt

/** One proposed placement: task, slot and the human reason for it. */
data class PlannedTask(
    val taskId: Long,
    val title: String,
    val startAt: Long,
    val durationMinutes: Int,
    val reason: String,
)

/** A whole proposed day: the placements, plus what honestly did not fit. */
data class DayPlan(
    val items: List<PlannedTask>,
    val unplaced: List<TaskEntity>,
)

/**
 * "Plan my day": the learning engine's graduation from advice to action.
 *
 * Takes everything flexible on today's plate (overdue, missed-today and
 * anytime tasks), keeps already-scheduled tasks where they are, and lays
 * the rest across the free windows of the remaining day. Every placement
 * is scored by the on-device completion model, so tasks land in the hours
 * THIS user actually gets things done; durations come from the estimate
 * calibration, so "Learning takes you 2.3x" becomes an honest time block.
 *
 * Pure Kotlin, no Android dependencies, fully deterministic given
 * (now, events, tasks): the planner is unit-tested like everything else.
 */
object DayPlanner {

    /** Nothing is scheduled past 22:00; a day has to end. */
    const val LATEST_HOUR = 22

    /** Breathing room between two blocks, so plans stay plans. */
    const val BUFFER_MINUTES = 10

    /** Candidate slots are offered on a 15-minute grid. */
    const val STEP_MINUTES = 15

    /** The smallest block worth putting on a plan. */
    const val MIN_DURATION_MINUTES = 15

    /** Weak nudge so overdue debt tends to clear earlier in the day. */
    private const val EARLIER_IS_BETTER = 0.004f

    /** Half-open window [start, end) of free time. */
    private data class Window(val start: Long, val end: Long)

    fun plan(
        now: Long,
        events: List<TaskEventEntity>,
        overdue: List<TaskEntity>,
        upcoming: List<TaskEntity>,
        anytime: List<TaskEntity>,
    ): DayPlan {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

        val predictor = CompletionPredictor.train(events)
        val calibration = RhythmProfile.estimateCalibration(events)

        val endOfDay = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, LATEST_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val firstSlot = ceilToStep(now)
        if (firstSlot >= endOfDay) {
            // The day is spent; nothing honest can be planned.
            return DayPlan(emptyList(), overdue + anytime)
        }
        val windows = mutableListOf(Window(firstSlot, endOfDay))

        // Already-scheduled tasks are the skeleton: they stay put, and
        // their (calibrated) duration blocks the time around them.
        for (task in upcoming) {
            val at = task.scheduledAt ?: continue
            val duration = durationOf(task, calibration)
            block(windows, at, at + (duration + BUFFER_MINUTES) * MILLIS_PER_MINUTE)
        }

        // Overdue debt clears first (oldest first); anytime tasks follow,
        // big rocks before small ones.
        data class Flex(val task: TaskEntity, val overdue: Boolean)
        val flexible = buildList {
            addAll(overdue.sortedBy { it.scheduledAt ?: Long.MAX_VALUE }.map { Flex(it, true) })
            addAll(anytime.sortedByDescending { it.estimatedMinutes }.map { Flex(it, false) })
        }

        val items = mutableListOf<PlannedTask>()
        val unplaced = mutableListOf<TaskEntity>()

        for (flex in flexible) {
            val task = flex.task
            val duration = durationOf(task, calibration)
            val durationMillis = duration * MILLIS_PER_MINUTE

            var bestSlot = -1L
            var bestScore = -1f
            for (window in windows) {
                var slot = window.start
                while (slot + durationMillis <= window.end) {
                    var score = predictor.predict(hourOf(slot), dayOfWeek, task.category)
                    if (flex.overdue) {
                        // Slight, honest preference for clearing debt soon.
                        score -= ((slot - firstSlot) / 3_600_000f) * EARLIER_IS_BETTER
                    }
                    if (score > bestScore) {
                        bestScore = score
                        bestSlot = slot
                    }
                    slot += STEP_MINUTES * MILLIS_PER_MINUTE
                }
            }

            if (bestSlot < 0) {
                unplaced.add(task)
                continue
            }

            val bestHour = if (predictor.isTrained) predictor.bestHourFor(task.category, dayOfWeek) else -1
            val reason = when {
                flex.overdue && hourOf(bestSlot) == bestHour ->
                    "Missed earlier, now at your best hour for it"
                flex.overdue -> "Clears yesterday's backlog"
                hourOf(bestSlot) == bestHour -> "Your best hour for it"
                else -> "Fits the shape of your day"
            }

            items.add(
                PlannedTask(
                    taskId = task.id,
                    title = task.title,
                    startAt = bestSlot,
                    durationMinutes = duration,
                    reason = reason,
                )
            )
            block(windows, bestSlot, bestSlot + (duration + BUFFER_MINUTES) * MILLIS_PER_MINUTE)
        }

        return DayPlan(items, unplaced)
    }

    /**
     * Calibrated duration: the user's estimate scaled by how long this
     * category REALLY takes them (from focus sessions), floored at 15
     * minutes and rounded to 5, so blocks read like clock times.
     */
    fun durationOf(task: TaskEntity, calibration: Map<String, Float>): Int {
        val ratio = calibration[task.category] ?: 1f
        val minutes = (task.estimatedMinutes * ratio).roundToInt()
        return ((minutes + 4) / 5 * 5).coerceAtLeast(MIN_DURATION_MINUTES)
    }

    // ---- helpers ----

    private const val MILLIS_PER_MINUTE = 60_000L

    /** Round [time] up to the next 15-minute grid point. */
    private fun ceilToStep(time: Long): Long {
        val step = STEP_MINUTES * MILLIS_PER_MINUTE
        val cal = Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val aligned = cal.timeInMillis
        return if (aligned >= time) aligned else aligned + step
    }

    /** Local hour of day for a timestamp. */
    private fun hourOf(time: Long): Int =
        Calendar.getInstance().apply { timeInMillis = time }.get(Calendar.HOUR_OF_DAY)

    /** Remove [from, to) from every window, keeping the usable leftovers. */
    private fun block(windows: MutableList<Window>, from: Long, to: Long) {
        val result = mutableListOf<Window>()
        for (window in windows) {
            if (to <= window.start || from >= window.end) {
                result += window
                continue
            }
            if (from > window.start) result += Window(window.start, from)
            if (to < window.end) result += Window(to, window.end)
        }
        val minUseful = MIN_DURATION_MINUTES * MILLIS_PER_MINUTE
        windows.clear()
        windows.addAll(result.filter { it.end - it.start >= minUseful })
    }
}
