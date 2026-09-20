package com.dincharya.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One observed interaction with a task — the raw training data of the app.
 *
 * Every time a task is created, completed, snoozed, ignored or rescheduled,
 * one of these rows is written. The adaptation engine later aggregates these
 * rows to answer questions like "which hour of the day does this user
 * actually complete chores?".
 *
 * Snapshot fields ([category], [scheduledAt], [hourOfDay]) are copied from
 * the task at event time so history stays truthful even if the task is
 * later edited or deleted.
 */
@Entity(tableName = "task_events")
data class TaskEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** The task this event belongs to (-1 for system-level events). */
    val taskId: Long,

    /** [EventOutcome] name. */
    val outcome: String,

    /** Epoch millis of the task's scheduled time at the moment of the event. */
    val scheduledAt: Long,

    /** Epoch millis when the event happened. */
    val occurredAt: Long,

    /** Category of the task at event time. */
    val category: String,

    /** Hour of day (0-23) when the event happened — the key learning feature. */
    val hourOfDay: Int,
)
