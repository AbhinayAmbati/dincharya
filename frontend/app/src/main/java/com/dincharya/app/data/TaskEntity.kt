package com.dincharya.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single task the user wants to get done.
 *
 * Notes on the design:
 *  - [scheduledAt] is `null` for "anytime" tasks that live on the list but
 *    have no reminder. The learning layer treats null as "no signal".
 *  - [snoozeCount] / [postponeCount] are denormalised counters kept on the
 *    task so the rule engine can react cheaply without re-querying all
 *    events (the full history still lives in [TaskEventEntity]).
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Short user-written description of the task. */
    val title: String,

    /** [TaskCategory] name — kept as a plain string so the DB is enum-safe. */
    val category: String = TaskCategory.PERSONAL.name,

    /** [TaskPriority] name. */
    val priority: String = TaskPriority.MEDIUM.name,

    /** The user's own estimate, in minutes. Compared against reality in Insights. */
    val estimatedMinutes: Int = 30,

    /** Epoch millis when the task should be done; null = anytime. */
    val scheduledAt: Long? = null,

    val isCompleted: Boolean = false,

    /** Epoch millis when the task was completed (null while pending). */
    val completedAt: Long? = null,

    val createdAt: Long = System.currentTimeMillis(),

    /** How many times the reminder for this task was snoozed. */
    val snoozeCount: Int = 0,

    /** How many times the task was moved to another day/time by the user. */
    val postponeCount: Int = 0,
)
