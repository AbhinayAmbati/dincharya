package com.dincharya.app.learning

import com.dincharya.app.data.EventOutcome
import com.dincharya.app.data.TaskCategory
import com.dincharya.app.data.TaskEntity
import com.dincharya.app.data.TaskEventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for the Stage-1 rule engine.
 *
 * These tests ARE the spec for the adaptation behaviour — if a change here
 * is intentional, update the rule AND its test in the same commit.
 */
class AdaptationEngineTest {

    // ---- Test helpers ----

    /** Build a calendar for today at [hour]:[minute], returned as epoch millis. */
    private fun todayAt(hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun task(
        id: Long = 1L,
        snoozeCount: Int = 0,
        scheduledAt: Long? = null,
        category: TaskCategory = TaskCategory.CHORES,
    ) = TaskEntity(
        id = id,
        title = "test task",
        category = category.name,
        scheduledAt = scheduledAt,
        snoozeCount = snoozeCount,
    )

    private fun event(
        hourOfDay: Int,
        outcome: EventOutcome,
        taskId: Long = 1L,
    ) = TaskEventEntity(
        taskId = taskId,
        outcome = outcome.name,
        scheduledAt = todayAt(hourOfDay),
        occurredAt = todayAt(hourOfDay),
        category = TaskCategory.CHORES.name,
        hourOfDay = hourOfDay,
    )

    // ---- Rule 1: suggestShiftEarlier ----

    @Test
    fun `shift earlier is suggested after three snoozes`() {
        val scheduled = todayAt(18, 0)
        val suggestion = AdaptationEngine.suggestShiftEarlier(
            task(snoozeCount = 3, scheduledAt = scheduled)
        )
        assertNotNull("3 snoozes must trigger a suggestion", suggestion)
        // 18:00 minus 2 hours = 16:00, comfortably after the 7am floor.
        val expected = Calendar.getInstance().apply {
            timeInMillis = scheduled
            add(Calendar.HOUR_OF_DAY, -2)
        }
        assertEquals(expected.timeInMillis, suggestion!!.newScheduledAt)
    }

    @Test
    fun `shift earlier is NOT suggested below the snooze threshold`() {
        assertNull(
            "2 snoozes must stay quiet — nagging is the failure mode",
            AdaptationEngine.suggestShiftEarlier(task(snoozeCount = 2, scheduledAt = todayAt(18)))
        )
    }

    @Test
    fun `shift earlier is NOT suggested for anytime tasks`() {
        assertNull(
            AdaptationEngine.suggestShiftEarlier(task(snoozeCount = 5, scheduledAt = null))
        )
    }

    @Test
    fun `shift earlier never lands before seven in the morning`() {
        // 08:00 - 2h = 06:00, which must be floored to 07:00.
        val suggestion = AdaptationEngine.suggestShiftEarlier(
            task(snoozeCount = 4, scheduledAt = todayAt(8))
        )!!
        val resultHour = Calendar.getInstance().apply { timeInMillis = suggestion.newScheduledAt }
        assertEquals(AdaptationEngine.EARLIEST_HOUR, resultHour.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultHour.get(Calendar.MINUTE))
    }

    // ---- completionRateByHour / bestHour ----

    @Test
    fun `completion rate by hour counts only scheduling outcomes`() {
        val events = listOf(
            event(9, EventOutcome.COMPLETED),
            event(9, EventOutcome.SNOOZED),
            event(20, EventOutcome.COMPLETED),
            event(20, EventOutcome.SNOOZED),
            event(20, EventOutcome.IGNORED),
            // CREATED events must not pollute the statistics.
            event(9, EventOutcome.CREATED),
        )
        val stats = AdaptationEngine.completionRateByHour(events)

        assertEquals(2, stats.size)
        assertEquals(1, stats.getValue(9).completed)
        assertEquals(2, stats.getValue(9).total)
        assertEquals(1, stats.getValue(20).completed)
        assertEquals(3, stats.getValue(20).total)
    }

    @Test
    fun `best hour requires the minimum sample count`() {
        // 14:00 has a perfect rate but only 2 samples (< MIN_SAMPLES_PER_HOUR=3).
        // 10:00 has 4 samples with a 50% rate — it must win.
        val events = listOf(
            event(14, EventOutcome.COMPLETED),
            event(14, EventOutcome.COMPLETED),
            event(10, EventOutcome.COMPLETED),
            event(10, EventOutcome.COMPLETED),
            event(10, EventOutcome.SNOOZED),
            event(10, EventOutcome.IGNORED),
        )
        assertEquals(10, AdaptationEngine.bestHour(events))
    }

    @Test
    fun `best hour is null when nothing has enough samples`() {
        val events = listOf(
            event(9, EventOutcome.COMPLETED),
            event(20, EventOutcome.SNOOZED),
        )
        assertNull(AdaptationEngine.bestHour(events))
    }

    // ---- Rule 2: suggestBetterWindow ----

    @Test
    fun `task in a weak window is moved to the best hour`() {
        // History: evenings are weak (0/3), mornings are strong (3/3).
        val events = listOf(
            event(9, EventOutcome.COMPLETED),
            event(9, EventOutcome.COMPLETED),
            event(9, EventOutcome.COMPLETED),
            event(20, EventOutcome.SNOOZED),
            event(20, EventOutcome.IGNORED),
            event(20, EventOutcome.SNOOZED),
        )
        val task = task(scheduledAt = todayAt(20), category = TaskCategory.CHORES)
        val suggestion = AdaptationEngine.suggestBetterWindow(task, events)

        assertNotNull(suggestion)
        val resultHour = Calendar.getInstance().apply {
            timeInMillis = suggestion!!.newScheduledAt
        }
        assertEquals(9, resultHour.get(Calendar.HOUR_OF_DAY))
        // The reason must explain itself — the suggest-don't-dictate rule.
        assertTrue(suggestion!!.reason.isNotEmpty())
    }

    @Test
    fun `no window suggestion when the current window is already good`() {
        val events = listOf(
            event(9, EventOutcome.COMPLETED),
            event(9, EventOutcome.COMPLETED),
            event(9, EventOutcome.COMPLETED),
            event(9, EventOutcome.SNOOZED),
        )
        val task = task(scheduledAt = todayAt(9))
        assertNull(AdaptationEngine.suggestBetterWindow(task, events))
    }

    @Test
    fun `evaluate prefers the snooze rule over the window rule`() {
        // Both rules could apply; Rule 1 (task-specific) must win.
        val events = listOf(
            event(9, EventOutcome.COMPLETED),
            event(9, EventOutcome.COMPLETED),
            event(9, EventOutcome.COMPLETED),
            event(20, EventOutcome.SNOOZED),
            event(20, EventOutcome.IGNORED),
            event(20, EventOutcome.SNOOZED),
        )
        val task = task(snoozeCount = 5, scheduledAt = todayAt(20))
        val suggestion = AdaptationEngine.evaluate(task, events)!!

        // Rule 1 result: 20:00 - 2h = 18:00 (not the 09:00 of Rule 2).
        assertEquals(18, Calendar.getInstance().apply {
            timeInMillis = suggestion.newScheduledAt
        }.get(Calendar.HOUR_OF_DAY))
    }
}
