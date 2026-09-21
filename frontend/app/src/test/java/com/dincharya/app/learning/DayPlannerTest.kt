package com.dincharya.app.learning

import com.dincharya.app.data.EventOutcome
import com.dincharya.app.data.TaskCategory
import com.dincharya.app.data.TaskEntity
import com.dincharya.app.data.TaskEventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for "Plan my day". The planner is deterministic given
 * (now, events, tasks), so the whole feature is spec'd here: anchored
 * tasks stay, flexible ones land in model-preferred hours, durations
 * honour the estimate calibration, and what does not fit is said so.
 */
class DayPlannerTest {

    private fun todayAt(hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun task(
        id: Long,
        title: String = "task $id",
        category: TaskCategory = TaskCategory.WORK,
        scheduledAt: Long? = null,
        estimatedMinutes: Int = 30,
    ) = TaskEntity(
        id = id,
        title = title,
        category = category.name,
        scheduledAt = scheduledAt,
        estimatedMinutes = estimatedMinutes,
    )

    private fun event(outcome: EventOutcome, hour: Int, category: String = "WORK") =
        TaskEventEntity(
            taskId = 1L,
            outcome = outcome.name,
            scheduledAt = todayAt(hour),
            occurredAt = todayAt(hour),
            category = category,
            hourOfDay = hour,
        )

    private fun hourOf(time: Long): Int =
        Calendar.getInstance().apply { timeInMillis = time }.get(Calendar.HOUR_OF_DAY)

    @Test
    fun `overdue task gets a realistic slot for the rest of the day`() {
        val now = todayAt(14)
        val plan = DayPlanner.plan(
            now = now,
            events = emptyList(),
            overdue = listOf(task(1, scheduledAt = now - 86_400_000)),
            upcoming = emptyList(),
            anytime = emptyList(),
        )
        assertEquals(1, plan.items.size)
        assertEquals(0, plan.unplaced.size)
        val slot = plan.items.first()
        assertTrue("slot must be at or after now", slot.startAt >= now)
        assertTrue("slot must be before 22:00", hourOf(slot.startAt) < DayPlanner.LATEST_HOUR)
        assertTrue("slot must be on the 15-minute grid", slot.startAt % (15 * 60_000L) == 0L)
    }

    @Test
    fun `already scheduled tasks are anchors, never moved`() {
        val now = todayAt(10)
        val anchored = task(1, scheduledAt = todayAt(12), estimatedMinutes = 60)
        // A big rock: 120 minutes cannot finish before the noon anchor,
        // so it must land after it.
        val rock = task(2, title = "big rock", estimatedMinutes = 120)
        val plan = DayPlanner.plan(
            now = now,
            events = emptyList(),
            overdue = emptyList(),
            upcoming = listOf(anchored),
            anytime = listOf(rock),
        )
        assertEquals(1, plan.items.size)
        val placed = plan.items.first()
        // Either before the anchor window [12:00, 13:10) or after it.
        assertTrue(
            "must not overlap the noon anchor",
            placed.startAt + placed.durationMinutes * 60_000L <= todayAt(12) ||
                placed.startAt >= todayAt(12) + (60 + DayPlanner.BUFFER_MINUTES) * 60_000L,
        )
        assertTrue(placed.startAt >= now)
    }

    @Test
    fun `calibration makes Learning blocks honest`() {
        val events = listOf(
            TaskEventEntity(
                taskId = 9L,
                outcome = EventOutcome.FOCUSED.name,
                scheduledAt = todayAt(10),
                occurredAt = todayAt(10),
                category = "LEARNING",
                hourOfDay = 10,
                detail = "30/60",
            ),
        )
        val task = task(1, category = TaskCategory.LEARNING, estimatedMinutes = 30)
        assertEquals(60, DayPlanner.durationOf(task, RhythmProfile.estimateCalibration(events)))
    }

    @Test
    fun `the model's best hour wins when the day allows it`() {
        // This user finishes things at 21:00 and snoozes at 08:00.
        val events = buildList {
            repeat(20) { add(event(EventOutcome.COMPLETED, 21)) }
            repeat(20) { add(event(EventOutcome.SNOOZED, 8)) }
        }
        val plan = DayPlanner.plan(
            now = todayAt(9),
            events = events,
            overdue = emptyList(),
            upcoming = emptyList(),
            anytime = listOf(task(1, estimatedMinutes = 30)),
        )
        assertEquals(1, plan.items.size)
        assertEquals(21, hourOf(plan.items.first().startAt))
    }

    @Test
    fun `nothing flexible makes an empty plan`() {
        val plan = DayPlanner.plan(
            now = todayAt(9),
            events = emptyList(),
            overdue = emptyList(),
            upcoming = listOf(task(1, scheduledAt = todayAt(12))),
            anytime = emptyList(),
        )
        assertTrue(plan.items.isEmpty())
        assertTrue(plan.unplaced.isEmpty())
    }

    @Test
    fun `a task that cannot fit before 22 is reported, not forced`() {
        val plan = DayPlanner.plan(
            now = todayAt(21, 30),
            events = emptyList(),
            overdue = emptyList(),
            upcoming = emptyList(),
            anytime = listOf(task(1, estimatedMinutes = 90)),
        )
        assertTrue(plan.items.isEmpty())
        assertEquals(1, plan.unplaced.size)
    }
}
