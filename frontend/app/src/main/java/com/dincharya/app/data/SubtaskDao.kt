package com.dincharya.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

/**
 * Data access for [SubtaskEntity].
 */
@Dao
interface SubtaskDao {

    @Insert
    suspend fun insertAll(subtasks: List<SubtaskEntity>)

    @Insert
    suspend fun insert(subtask: SubtaskEntity): Long

    @Update
    suspend fun update(subtask: SubtaskEntity)

    @Delete
    suspend fun delete(subtask: SubtaskEntity)

    /** All subtasks of one task, in creation order. */
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY id ASC")
    suspend fun forTask(taskId: Long): List<SubtaskEntity>

    /** How many subtasks of a task are done — quick progress signal. */
    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId AND isDone = 1")
    suspend fun doneCountForTask(taskId: Long): Int
}
