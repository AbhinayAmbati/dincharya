package com.dincharya.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [TaskEntity].
 *
 * Read methods return [Flow] so the UI updates live; write methods are
 * suspend and run on a background dispatcher via Room.
 */
@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun byId(id: Long): TaskEntity?

    /**
     * All uncompleted tasks. Order: scheduled ones first (earliest first),
     * then anytime tasks. SQLite evaluates `scheduledAt IS NULL` as 0/1,
     * which is why this sorts as intended.
     */
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY scheduledAt IS NULL, scheduledAt ASC")
    fun observePending(): Flow<List<TaskEntity>>

    /** One-shot snapshot of [observePending] for non-UI callers (the widget, the briefs). */
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY scheduledAt IS NULL, scheduledAt ASC")
    suspend fun pendingOnce(): List<TaskEntity>

    /** Tasks completed within [from, to] — used for "done today" and the evening review. */
    @Query(
        "SELECT * FROM tasks WHERE isCompleted = 1 AND completedAt >= :from AND completedAt < :to " +
            "ORDER BY completedAt ASC"
    )
    fun observeCompletedBetween(from: Long, to: Long): Flow<List<TaskEntity>>
}
