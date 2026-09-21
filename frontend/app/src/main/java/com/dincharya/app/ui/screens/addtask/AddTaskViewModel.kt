package com.dincharya.app.ui.screens.addtask

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dincharya.app.app.Graph
import com.dincharya.app.data.RepeatRule
import com.dincharya.app.data.TaskCategory
import com.dincharya.app.data.TaskEntity
import com.dincharya.app.data.TaskPriority
import com.dincharya.app.notifications.ReminderScheduler
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Form state + save logic for the Add Task screen.
 *
 * Design intent: capture must be FAST — a task takes under 15 seconds to
 * type. All state is Compose state so the screen updates instantly while
 * the user types.
 */
class AddTaskViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    /**
     * Task being edited, or null when this screen is a plain "add". The id
     * arrives as the {taskId} path argument of the edit route.
     */
    private val editTaskId: Long = savedStateHandle.get<String>("taskId")?.toLongOrNull() ?: 0L
    val isEdit: Boolean get() = editTaskId != 0L

    /** The original row, once loaded — save() updates it in place. */
    private var original: TaskEntity? = null

    /** True once the edit form has been populated. */
    var loaded by mutableStateOf(false)
        private set

    /** In edit mode: whether the task is currently completed. */
    var editingCompleted by mutableStateOf(false)
        private set

    var title by mutableStateOf("")
    var category by mutableStateOf(TaskCategory.PERSONAL)
    var priority by mutableStateOf(TaskPriority.MEDIUM)
    var durationMinutes by mutableStateOf(30)

    /** Optional free-form note shown on the Focus screen. */
    var note by mutableStateOf("")

    /** How the task repeats (once by default). */
    var repeatRule by mutableStateOf(RepeatRule.NONE)

    /** Subtask titles the user typed; blank lines are dropped on save. */
    val subtaskTitles = mutableStateListOf("")

    /** Null = anytime task (kept on the list, no reminder booked). */
    var useTime by mutableStateOf(true)
    var hour by mutableStateOf(9)
    var minute by mutableStateOf(0)

    /** Duration presets shown as chips on the screen. */
    val durationOptions = listOf(15, 30, 45, 60, 90, 120)

    init {
        if (editTaskId != 0L) {
            // Edit mode: populate the form from the stored task.
            viewModelScope.launch {
                val task = Graph.repository.taskById(editTaskId) ?: return@launch
                original = task
                title = task.title
                category = runCatching { TaskCategory.valueOf(task.category) }.getOrDefault(TaskCategory.PERSONAL)
                priority = runCatching { TaskPriority.valueOf(task.priority) }.getOrDefault(TaskPriority.MEDIUM)
                durationMinutes = task.estimatedMinutes
                note = task.note ?: ""
                repeatRule = runCatching { RepeatRule.valueOf(task.repeatRule) }.getOrDefault(RepeatRule.NONE)
                val subs = Graph.repository.subtasksFor(editTaskId)
                subtaskTitles.clear()
                if (subs.isEmpty()) {
                    subtaskTitles.add("")
                } else {
                    subs.forEach { subtaskTitles.add(it.title) }
                }
                task.scheduledAt?.let { at ->
                    useTime = true
                    val cal = Calendar.getInstance().apply { timeInMillis = at }
                    hour = cal.get(Calendar.HOUR_OF_DAY)
                    minute = cal.get(Calendar.MINUTE)
                } ?: run { useTime = false }
                editingCompleted = task.isCompleted
                loaded = true
            }
        } else {
            // A title shared from another app ("share to Dincharya") seeds the form.
            app.getSharedTaskTitle()?.let {
                title = it
            }
            loaded = true
        }
    }

    /**
     * True while a save is in flight. Guards the save button against
     * double-taps: without it, a few impatient taps on "Add task" would
     * create several identical tasks (and several identical reminders).
     */
    private var saving = false

    /**
     * Compute the scheduled time for today at [hour]:[minute]; if that slot
     * has already passed today, roll to tomorrow. (Reminder scheduling in
     * the past would fire immediately, which is never what the user meant.)
     *
     * Edits keep one exception: a task already scheduled on a future day
     * (e.g. a recurring occurrence due next Tuesday) keeps that day — only
     * the time-of-day changes. Without this, editing a future occurrence
     * would silently pull it into today.
     */
    private fun computeScheduledAt(): Long? {
        if (!useTime) return null
        original?.scheduledAt?.let { at ->
            if (at > System.currentTimeMillis()) {
                return Calendar.getInstance().apply {
                    timeInMillis = at
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
        }
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    /** True when the form is complete enough to save. */
    val canSave: Boolean get() = title.isNotBlank()

    /**
     * Undo the completion of the task being edited: back to pending, the
     * spawned next occurrence (if any) removed, reminder re-booked when its
     * slot is still ahead. Used from the edit screen of a completed task.
     */
    fun markPending(onDone: () -> Unit) {
        val existing = original ?: return
        viewModelScope.launch {
            val childId = Graph.repository.undoCompletion(existing)
            childId?.let { ReminderScheduler.cancel(getApplication(), it) }
            existing.scheduledAt?.let { at ->
                if (at > System.currentTimeMillis()) {
                    ReminderScheduler.schedule(getApplication(), existing.id, at)
                }
            }
            editingCompleted = false
            onDone()
        }
    }

    /** Adds one more (empty) subtask input row. */
    fun addSubtaskRow() {
        subtaskTitles.add("")
    }

    /** Edits the subtask at [index]; removing a row's text makes it vanish on save. */
    fun setSubtask(index: Int, value: String) {
        subtaskTitles[index] = value
    }

    /**
     * Persist the task, log CREATED, save its subtasks, and book the reminder.
     * In edit mode the existing row is updated in place (subtasks included)
     * and the reminder is re-booked — or cancelled if the task became anytime.
     */
    fun save(onSaved: () -> Unit) {
        if (!canSave || saving || !loaded) return
        saving = true
        val scheduledAt = computeScheduledAt()
        viewModelScope.launch {
            val subtasks = subtaskTitles.map { it.trim() }.filter { it.isNotEmpty() }
            val existing = original
            if (existing == null) {
                val id = Graph.repository.createTask(
                    TaskEntity(
                        title = title.trim(),
                        category = category.name,
                        priority = priority.name,
                        estimatedMinutes = durationMinutes,
                        scheduledAt = scheduledAt,
                        note = note.trim().ifBlank { null },
                        repeatRule = repeatRule.name,
                    )
                )
                Graph.repository.addSubtasks(id, subtasks)
                scheduledAt?.let {
                    ReminderScheduler.schedule(getApplication(), id, it)
                }
            } else {
                Graph.repository.updateTask(
                    existing,
                    existing.copy(
                        title = title.trim(),
                        category = category.name,
                        priority = priority.name,
                        estimatedMinutes = durationMinutes,
                        scheduledAt = scheduledAt,
                        note = note.trim().ifBlank { null },
                        repeatRule = repeatRule.name,
                    ),
                )
                Graph.repository.replaceSubtasks(existing.id, subtasks)
                // Re-book: cancel first so a moved slot cannot fire twice,
                // and only book future slots (past ones would fire at once).
                ReminderScheduler.cancel(getApplication(), existing.id)
                scheduledAt?.let {
                    if (it > System.currentTimeMillis()) {
                        ReminderScheduler.schedule(getApplication(), existing.id, it)
                    }
                }
            }
            onSaved()
        }
    }
}

/** One-shot inbox for text shared into the app from elsewhere. */
private fun android.app.Application.getSharedTaskTitle(): String? =
    com.dincharya.app.app.SharedInbox.consumePendingTitle()
