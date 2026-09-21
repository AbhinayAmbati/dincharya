package com.dincharya.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Data access for [TaskEventEntity] — the behavioural event log.
 *
 * The full log is what the adaptation engine learns from; it is also the
 * data that will be mirrored (encrypted) to the backend in a later phase.
 */
@Dao
interface TaskEventDao {

    @Insert
    suspend fun insert(event: TaskEventEntity)

    @Query("SELECT * FROM task_events WHERE taskId = :taskId ORDER BY occurredAt DESC")
    suspend fun forTask(taskId: Long): List<TaskEventEntity>

    /** Complete event history, oldest first — input for the adaptation engine and Insights. */
    @Query("SELECT * FROM task_events ORDER BY occurredAt ASC")
    suspend fun all(): List<TaskEventEntity>

    /**
     * Remove the COMPLETED event(s) a completion wrote, in the instant after
     * [completedAt]. Undoing a completion must roll the learning signal back
     * too, or a complete-undo-complete cycle would count twice in Insights.
     */
    @Query(
        "DELETE FROM task_events WHERE taskId = :taskId AND outcome = 'COMPLETED' " +
            "AND occurredAt >= :completedAt AND occurredAt <= :until"
    )
    suspend fun deleteCompletionBetween(taskId: Long, completedAt: Long, until: Long)

    /** Remove every event of a task (used when a spawned occurrence is deleted on undo). */
    @Query("DELETE FROM task_events WHERE taskId = :taskId")
    suspend fun deleteAllForTask(taskId: Long)
}
