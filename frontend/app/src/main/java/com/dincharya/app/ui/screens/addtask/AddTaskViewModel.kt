package com.dincharya.app.ui.screens.addtask

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dincharya.app.app.Graph
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
class AddTaskViewModel(app: Application) : AndroidViewModel(app) {

    var title by mutableStateOf("")
    var category by mutableStateOf(TaskCategory.PERSONAL)
    var priority by mutableStateOf(TaskPriority.MEDIUM)
    var durationMinutes by mutableStateOf(30)

    /** Null = anytime task (kept on the list, no reminder booked). */
    var useTime by mutableStateOf(true)
    var hour by mutableStateOf(9)
    var minute by mutableStateOf(0)

    /** Duration presets shown as chips on the screen. */
    val durationOptions = listOf(15, 30, 45, 60, 90, 120)

    /**
     * Compute the scheduled time for today at [hour]:[minute]; if that slot
     * has already passed today, roll to tomorrow. (Reminder scheduling in
     * the past would fire immediately, which is never what the user meant.)
     */
    private fun computeScheduledAt(): Long? {
        if (!useTime) return null
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
     * Persist the task, log CREATED, and book the reminder.
     *
     * @return true if the task was created.
     */
    fun save(onSaved: () -> Unit) {
        if (!canSave) return
        val scheduledAt = computeScheduledAt()
        viewModelScope.launch {
            val id = Graph.repository.createTask(
                TaskEntity(
                    title = title.trim(),
                    category = category.name,
                    priority = priority.name,
                    estimatedMinutes = durationMinutes,
                    scheduledAt = scheduledAt,
                )
            )
            scheduledAt?.let {
                ReminderScheduler.schedule(getApplication(), id, it)
            }
            onSaved()
        }
    }
}
