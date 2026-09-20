package com.dincharya.app.data

import com.dincharya.app.learning.EventLogger
import kotlinx.coroutines.flow.Flow

/**
 * The app's single source of truth for task data and the place where every
 * state change gets logged as a behavioural event.
 *
 * UI layer (ViewModels) talks only to this class — never to DAOs directly —
 * so the "mutate + log" pair can never be accidentally separated.
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val eventDao: TaskEventDao,
) {
    private val eventLogger = EventLogger(eventDao)

    /** All pending tasks, live-updating. */
    val pendingTasks: Flow<List<TaskEntity>> = taskDao.observePending()

    /** Tasks completed within [from, to), live-updating (day boundaries). */
    fun completedBetween(from: Long, to: Long): Flow<List<TaskEntity>> =
        taskDao.observeCompletedBetween(from, to)

    suspend fun taskById(id: Long): TaskEntity? = taskDao.byId(id)

    /**
     * Create a task and log the CREATED event.
     *
     * @return the new task's row id.
     */
    suspend fun createTask(task: TaskEntity): Long {
        val id = taskDao.insert(task)
        eventLogger.log(task.copy(id = id), EventOutcome.CREATED)
        return id
    }

    /**
     * Mark a task complete and log the COMPLETED event — the reward signal
     * the learning layer trains on.
     *
     * Idempotent by design: a notification Done button, the Today check
     * circle and the Focus screen can all fire for the same task, and a
     * repeated completion must never be logged twice — duplicates would
     * pollute the learning signal and repeat rows in the evening review.
     */
    suspend fun completeTask(task: TaskEntity) {
        if (task.isCompleted) return
        val now = System.currentTimeMillis()
        taskDao.update(task.copy(isCompleted = true, completedAt = now))
        eventLogger.log(task, EventOutcome.COMPLETED)
    }

    /**
     * Snooze a task by [minutes] and log the SNOOZED event.
     *
     * @return the new reminder time (epoch millis) so the caller can hand it
     *         to the reminder scheduler.
     */
    suspend fun snoozeTask(task: TaskEntity, minutes: Int): Long {
        val newTime = System.currentTimeMillis() + minutes * 60_000L
        taskDao.update(task.copy(scheduledAt = newTime, snoozeCount = task.snoozeCount + 1))
        eventLogger.log(task, EventOutcome.SNOOZED)
        return newTime
    }

    /**
     * Move a task to [newScheduledAt] (user action or accepted suggestion)
     * and log the RESCHEDULED event.
     */
    suspend fun rescheduleTask(task: TaskEntity, newScheduledAt: Long) {
        taskDao.update(
            task.copy(scheduledAt = newScheduledAt, postponeCount = task.postponeCount + 1)
        )
        eventLogger.log(task, EventOutcome.RESCHEDULED)
    }

    /** Delete a task. Its event history is kept on purpose — deleted tasks still teach. */
    suspend fun deleteTask(task: TaskEntity) {
        taskDao.delete(task)
    }

    /** Event history of one task, newest first. */
    suspend fun eventsForTask(taskId: Long): List<TaskEventEntity> =
        eventDao.forTask(taskId)

    /** Complete event log, oldest first. */
    suspend fun allEvents(): List<TaskEventEntity> = eventDao.all()
}
