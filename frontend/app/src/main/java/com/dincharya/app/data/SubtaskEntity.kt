package com.dincharya.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One checkable step inside a task (added on the Add Task screen, ticked
 * off on the Focus screen).
 *
 * Belongs to exactly one [TaskEntity]; kept deliberately dumb — no dates,
 * no learning signal of its own (the parent task's completion is the event
 * that matters).
 */
@Entity(
    tableName = "subtasks",
    indices = [Index("taskId")],
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** The parent task. */
    val taskId: Long,

    /** What to do. */
    val title: String,

    /** Checked off? */
    val isDone: Boolean = false,
)
