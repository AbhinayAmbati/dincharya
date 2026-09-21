package com.dincharya.app.ui.screens.focus

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dincharya.app.app.Graph
import com.dincharya.app.data.SubtaskEntity
import com.dincharya.app.data.TaskEntity
import com.dincharya.app.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Focus screen.
 *
 * Owns task selection and the countdown state; the screen itself drives the
 * 1-second tick (a UI concern) and reports it back via [tick] — this keeps
 * the timer accurate to what the user actually sees.
 */
class FocusViewModel(app: Application) : AndroidViewModel(app) {

    /** All pending tasks, live — the user picks one to focus on. */
    val pendingTasks: StateFlow<List<TaskEntity>> = Graph.repository.pendingTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** The task being focused on (null while picking). */
    var selectedTask by mutableStateOf<TaskEntity?>(null)
        private set

    /** Remaining seconds in the session. */
    var remainingSeconds by mutableIntStateOf(0)
        private set

    /** Total session length — used to draw the progress bar. */
    var totalSeconds by mutableIntStateOf(0)
        private set

    var running by mutableStateOf(false)
        private set

    /** True once the clock reaches zero. */
    var finished by mutableStateOf(false)
        private set

    /** Session-length presets (Pomodoro and friends) offered after picking a task. */
    val durationPresets = listOf(15, 25, 45, 50, 90)

    /** Subtasks of the selected task, checked off during the session. */
    var subtasks by mutableStateOf(emptyList<SubtaskEntity>())
        private set

    /** Start a focus session on [task]. */
    fun select(task: TaskEntity) {
        selectedTask = task
        totalSeconds = task.estimatedMinutes * 60
        remainingSeconds = totalSeconds
        running = false
        finished = false
        loadSubtasks(task.id)
    }

    private fun loadSubtasks(taskId: Long) {
        viewModelScope.launch {
            subtasks = Graph.repository.subtasksFor(taskId)
        }
    }

    /** Override the session length (a Pomodoro preset or the task estimate). */
    fun setDuration(minutes: Int) {
        totalSeconds = minutes * 60
        remainingSeconds = totalSeconds
        running = false
        finished = false
    }

    /** Toggle one subtask's done flag. */
    fun toggleSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            Graph.repository.toggleSubtask(subtask)
            subtasks = Graph.repository.subtasksFor(subtask.taskId)
        }
    }

    fun deselect() {
        selectedTask = null
        running = false
        finished = false
    }

    fun start() {
        if (!finished) running = true
    }

    fun pause() {
        running = false
    }

    /** Called by the screen once per second while [running]. */
    fun tick() {
        if (!running) return
        if (remainingSeconds > 0) {
            remainingSeconds -= 1
            if (remainingSeconds == 0) {
                running = false
                finished = true
            }
        }
    }

    /**
     * Mark the focused task complete; cancel its reminder. The time actually
     * spent in the session is recorded against the task's estimate — the
     * raw material for "you take 2x longer than you think" on Insights.
     */
    fun completeSelected(onDone: () -> Unit) {
        val task = selectedTask ?: return
        val focusedMinutes = (totalSeconds - remainingSeconds) / 60
        viewModelScope.launch {
            if (focusedMinutes >= 1) {
                Graph.repository.logFocused(task, task.estimatedMinutes, focusedMinutes)
            }
            Graph.repository.completeTask(task)
            ReminderScheduler.cancel(getApplication(), task.id)
            onDone()
        }
    }
}
