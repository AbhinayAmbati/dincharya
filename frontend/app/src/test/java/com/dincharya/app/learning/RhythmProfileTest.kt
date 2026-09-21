package com.dincharya.app.learning

import com.dincharya.app.data.EventOutcome
import com.dincharya.app.data.RepeatRule
import com.dincharya.app.data.TaskEntity
import com.dincharya.app.data.TaskEventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for the personalised rhythm profile: the self-image check,
 * per-category hours, habit strength and estimate calibration.
 */
class RhythmProfileTest {

    private fun nowAt(hour: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun completed(hour: Int, category: String = "WORK") = TaskEventEntity(
        taskId = 1L,
        outcome = EventOutcome.COMPLETED.name,
        scheduledAt = nowAt(hour),
        occurredAt = nowAt(hour),
        category = category,
        hourOfDay = hour,
    )

    // ---- Self-image ----

    @Test
    fun `self image null on thin history`() {
        val events = (1..10).map { completed(20) }
        assertNull(RhythmProfile.selfImage("early", events))
    }

    @Test
    fun `declared morning person with evening record gets called out`() {
        val events = buildList {
            repeat(15) { add(completed(19, category = "PERSONAL")) } // 7 PM
            repeat(2) { add(completed(9, category = "PERSONAL")) }
        }
        val line = RhythmProfile.selfImage("early", events)
        assertNotNull(line)
        assertTrue(line!!.contains("after 5 PM"))
    }

    @Test
    fun `declared night owl with morning record gets called out`() {
        val events = buildList {
            repeat(15) { add(completed(7)) } // 7 AM
            repeat(2) { add(completed(22)) }
        }
        val line = RhythmProfile.selfImage("late", events)
        assertNotNull(line)
        assertTrue(line!!.contains("before noon"))
    }

    @Test
    fun `no callout when claim and record agree`() {
        val events = buildList {
            repeat(15) { add(completed(7)) }
        }
        assertNull(RhythmProfile.selfImage("early", events))
    }

    // ---- Per-category hours ----

    @Test
    fun `per category best hour separates categories`() {
        val events = buildList {
            repeat(6) { add(completed(9, category = "LEARNING")) }
            repeat(6) { add(completed(20, category = "CHORES")) }
        }
        val map = RhythmProfile.perCategoryBestHour(events)
        assertEquals(9, map["LEARNING"])
        assertEquals(20, map["CHORES"])
    }

    @Test
    fun `thin categories stay out of the map`() {
        val events = buildList {
            repeat(6) { add(completed(9, category = "LEARNING")) }
            repeat(1) { add(completed(20, category = "CHORES")) }
        }
        val map = RhythmProfile.perCategoryBestHour(events)
        assertEquals(9, map["LEARNING"])
        assertNull(map["CHORES"])
    }

    // ---- Habit strength ----

    @Test
    fun `daily habit scored over the 30 day window`() {
        val now = System.currentTimeMillis()
        val pendingHabit = TaskEntity(
            id = 1L,
            title = "Read",
            category = "LEARNING",
            repeatRule = RepeatRule.DAILY.name,
        )
        val completedRows = (1..17).map { i ->
            pendingHabit.copy(
                id = 100L + i,
                isCompleted = true,
                completedAt = now - i * 24L * 60 * 60 * 1000,
            )
        }
        val habits = RhythmProfile.habitStrength(listOf(pendingHabit) + completedRows)
        assertEquals(1, habits.size)
        assertEquals(17, habits.first().completions)
        assertEquals(22, habits.first().expected) // 30 minus 8 slack days
        assertEquals(17f / 22f, habits.first().strength, 0.001f)
    }

    // ---- Estimate calibration ----

    @Test
    fun `calibration reads the median est-act ratio`() {
        fun focused(est: Int, act: Int) = TaskEventEntity(
            taskId = 1L,
            outcome = EventOutcome.FOCUSED.name,
            scheduledAt = nowAt(10),
            occurredAt = nowAt(10),
            category = "LEARNING",
            hourOfDay = 10,
            detail = "$est/$act",
        )
        val events = listOf(focused(30, 60), focused(30, 30), focused(30, 90))
        // ratios 2.0, 1.0, 3.0 -> median 2.0
        val calibration = RhythmProfile.estimateCalibration(events)
        assertEquals(2.0f, calibration["LEARNING"]!!, 0.001f)
    }

    @Test
    fun `calibration ignores junk detail rows`() {
        val junk = TaskEventEntity(
            taskId = 1L,
            outcome = EventOutcome.FOCUSED.name,
            scheduledAt = nowAt(10),
            occurredAt = nowAt(10),
            category = "WORK",
            hourOfDay = 10,
            detail = null,
        )
        assertTrue(RhythmProfile.estimateCalibration(listOf(junk)).isEmpty())
    }
}
