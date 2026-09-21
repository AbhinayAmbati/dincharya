package com.dincharya.app.learning

import com.dincharya.app.data.EventOutcome
import com.dincharya.app.data.TaskEventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for the on-device logistic regression.
 *
 * Pure JVM, no Android — the predictor is plain Kotlin by design, so the
 * learning behaviour is spec'd here exactly like the rule engine.
 */
class CompletionPredictorTest {

    private fun event(
        outcome: EventOutcome,
        hour: Int,
        category: String = "WORK",
    ) = TaskEventEntity(
        taskId = 1L,
        outcome = outcome.name,
        scheduledAt = todayAt(hour),
        occurredAt = todayAt(hour),
        category = category,
        hourOfDay = hour,
    )

    private fun todayAt(hour: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `untrained when history is thin`() {
        val events = (1..10).map { event(EventOutcome.COMPLETED, 10) }
        val model = CompletionPredictor.train(events)
        assertFalse(model.isTrained)
    }

    @Test
    fun `learns a strong hour pattern`() {
        // 20 completions at 21:00 and 20 snoozes at 08:00 — the classic
        // "not a morning person" log.
        val events = buildList {
            repeat(20) { add(event(EventOutcome.COMPLETED, 21)) }
            repeat(20) { add(event(EventOutcome.SNOOZED, 8)) }
        }
        val model = CompletionPredictor.train(events)
        assertTrue(model.isTrained)
        assertTrue(
            "evening probability should beat morning",
            model.predict(21, Calendar.MONDAY, "WORK") >
                model.predict(8, Calendar.MONDAY, "WORK"),
        )
        assertEquals(21, model.bestHourFor("WORK", Calendar.MONDAY))
    }

    @Test
    fun `prediction stays inside 0 and 1`() {
        val events = buildList {
            repeat(15) { add(event(EventOutcome.COMPLETED, 14)) }
            repeat(15) { add(event(EventOutcome.IGNORED, 3)) }
        }
        val model = CompletionPredictor.train(events)
        for (hour in 0..23) {
            val p = model.predict(hour, Calendar.SUNDAY, "CHORES")
            assertTrue(p in 0f..1f)
        }
    }
}
