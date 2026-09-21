package com.dincharya.app.data

import android.content.Context
import com.dincharya.app.learning.EventLogger
import com.dincharya.app.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

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
    private val subtaskDao: SubtaskDao,
) {
    private val eventLogger = EventLogger(eventDao)

    /**
     * Application context, injected once from Graph — needed to book the
     * reminder for recurring tasks this repository spawns itself. Kept
     * nullable so the class stays constructible in unit tests.
     */
    @Volatile
    private var appContext: Context? = null

    fun attachContext(context: Context) {
        appContext = context
    }

    /** All pending tasks, live-updating. */
    val pendingTasks: Flow<List<TaskEntity>> = taskDao.observePending()

    /** All pending tasks, one snapshot (used by the morning brief worker). */
    suspend fun pendingTasksOnce(): List<TaskEntity> = taskDao.pendingOnce()

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
     * Add subtasks under a freshly created task.
     * Blank lines are filtered by the caller.
     */
    suspend fun addSubtasks(taskId: Long, titles: List<String>) {
        if (titles.isEmpty()) return
        subtaskDao.insertAll(titles.map { SubtaskEntity(taskId = taskId, title = it) })
    }

    /** Subtasks of one task, in creation order. */
    suspend fun subtasksFor(taskId: Long): List<SubtaskEntity> = subtaskDao.forTask(taskId)

    /** Toggle a subtask's done flag. */
    suspend fun toggleSubtask(subtask: SubtaskEntity) {
        subtaskDao.update(subtask.copy(isDone = !subtask.isDone))
    }

    /**
     * Mark a task complete and log the COMPLETED event — the reward signal
     * the learning layer trains on.
     *
     * Idempotent by design: a notification Done button, the Today check
     * circle and the Focus screen can all fire for the same task, and a
     * repeated completion must never be logged twice — duplicates would
     * pollute the learning signal and repeat rows in the evening review.
     *
     * Recurring tasks immediately spawn their next occurrence (with its
     * reminder booked), so habits never leave the pending list.
     */
    suspend fun completeTask(task: TaskEntity) {
        if (task.isCompleted) return
        val now = System.currentTimeMillis()
        taskDao.update(task.copy(isCompleted = true, completedAt = now))
        eventLogger.log(task, EventOutcome.COMPLETED)

        val rule = runCatching { RepeatRule.valueOf(task.repeatRule) }.getOrDefault(RepeatRule.NONE)
        if (rule.spawnsNext) {
            val next = nextOccurrence(task, rule)
            if (next != null) {
                spawnOccurrence(task, next)
            } else {
                // Anytime recurring task (no slot to roll forward): the
                // habit must stay on the list, so a plain pending copy is
                // spawned immediately.
                spawnOccurrence(
                    task,
                    task.copy(
                        id = 0L,
                        isCompleted = false,
                        completedAt = null,
                        createdAt = System.currentTimeMillis(),
                        snoozeCount = 0,
                        postponeCount = 0,
                        scheduledAt = null,
                    ),
                )
            }
        }
    }

    /**
     * Insert the next occurrence of a recurring task: book its reminder,
     * carry the subtask checklist over (fresh, unchecked) and record the
     * parent so undo can remove it again.
     */
    private suspend fun spawnOccurrence(parent: TaskEntity, next: TaskEntity) {
        val child = next.copy(spawnedBy = parent.id)
        val id = taskDao.insert(child)
        eventLogger.log(child.copy(id = id), EventOutcome.CREATED)
        subtaskDao.forTask(parent.id).forEach { sub ->
            subtaskDao.insertAll(listOf(sub.copy(id = 0L, taskId = id, isDone = false)))
        }
        child.scheduledAt?.let { at ->
            appContext?.let { ctx -> ReminderScheduler.schedule(ctx, id, at) }
        }
    }

    /**
     * The next occurrence of a recurring task: same time-of-day, one period
     * later (skipping weekends for WEEKDAYS). Anytime tasks return null —
     * the caller re-creates a plain pending copy instead (see completeTask).
     */
    private fun nextOccurrence(task: TaskEntity, rule: RepeatRule): TaskEntity? {
        val scheduled = task.scheduledAt ?: return null
        val cal = Calendar.getInstance().apply {
            timeInMillis = scheduled
            when (rule) {
                RepeatRule.DAILY -> add(Calendar.DAY_OF_YEAR, 1)
                RepeatRule.WEEKLY -> add(Calendar.DAY_OF_YEAR, 7)
                RepeatRule.WEEKDAYS -> {
                    do {
                        add(Calendar.DAY_OF_YEAR, 1)
                    } while (get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                        get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                }
                RepeatRule.NONE -> return null
            }
            // Never book a recurrence in the past (e.g. completing a very
            // overdue task rolls it to the same time tomorrow — which is
            // what the user meant anyway).
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return task.copy(
            id = 0L,
            isCompleted = false,
            completedAt = null,
            createdAt = System.currentTimeMillis(),
            snoozeCount = 0,
            postponeCount = 0,
            scheduledAt = cal.timeInMillis,
        )
    }

    /**
     * Undo a completion: the task returns to pending and the occurrence
     * this completion spawned (if any) is removed, so a habit rolls back
     * exactly one step. The COMPLETED event stays in the history on
     * purpose — events are an append-only record of what happened.
     *
     * @return the id of the removed spawned occurrence, so the caller can
     *         cancel that reminder too (null when there was none).
     */
    suspend fun undoCompletion(task: TaskEntity): Long? {
        if (!task.isCompleted) return null
        val child = taskDao.bySpawnedBy(task.id)
        if (child != null) {
            taskDao.delete(child)
        }
        taskDao.update(task.copy(isCompleted = false, completedAt = null))
        return child?.id
    }

    /**
     * Save edits made on the Add/Edit Task screen. A changed reminder time
     * is logged as RESCHEDULED so the learning layer keeps its signal.
     */
    suspend fun updateTask(original: TaskEntity, edited: TaskEntity) {
        taskDao.update(edited)
        if (original.scheduledAt != edited.scheduledAt) {
            eventLogger.log(edited, EventOutcome.RESCHEDULED)
        }
    }

    /**
     * Replace the subtask list of a task. Subtasks whose title is unchanged
     * keep their checked state, so editing a note never clears a checklist.
     */
    suspend fun replaceSubtasks(taskId: Long, titles: List<String>) {
        val existing = subtaskDao.forTask(taskId).associate { it.title to it.isDone }
        subtaskDao.deleteAllForTask(taskId)
        if (titles.isNotEmpty()) {
            subtaskDao.insertAll(titles.map { SubtaskEntity(taskId = taskId, title = it, isDone = existing[it] ?: false) })
        }
    }

    /**
     * Pending tasks relevant "today": anytime tasks plus everything
     * scheduled up to the end of today. Future occurrences of recurring
     * tasks stay hidden until the day they occur. Used by the widget.
     */
    suspend fun pendingTodayOnce(): List<TaskEntity> {
        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        return taskDao.pendingOnce().filter { it.scheduledAt == null || it.scheduledAt < endOfToday }
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
