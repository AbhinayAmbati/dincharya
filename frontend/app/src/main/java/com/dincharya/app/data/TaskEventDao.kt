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
}
