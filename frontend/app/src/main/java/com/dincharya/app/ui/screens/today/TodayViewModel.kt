package com.dincharya.app.ui.screens.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dincharya.app.app.Graph
import com.dincharya.app.data.TaskEntity
import com.dincharya.app.data.TaskEventEntity
import com.dincharya.app.learning.Adaptation
import com.dincharya.app.learning.AdaptationEngine
import com.dincharya.app.notifications.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Immutable snapshot of the Today screen.
 *
 * Tasks are split by urgency: overdue (scheduled before today), upcoming
 * (later today), later (after today) and anytime (no scheduled time).
 * Each suggestion pairs the task with the [Adaptation] proposed for it.
 */
data class TodayUiState(
    val dateTitle: String = "",
    val overdue: List<TaskEntity> = emptyList(),
    val upcoming: List<TaskEntity> = emptyList(),
    val later: List<TaskEntity> = emptyList(),
    val anytime: List<TaskEntity> = emptyList(),
    val completedToday: List<TaskEntity> = emptyList(),
    val suggestions: Map<Long, Adaptation> = emptyMap(),
)

/**
 * ViewModel for the Today screen: streams the live task list, splits it into
 * sections, and surfaces adaptation suggestions from the rule engine.
 */
class TodayViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = Graph.repository

    /** Today's window [startOfDay, startOfTomorrow), fixed at VM creation. */
    private val dayStart: Long
    private val dayEnd: Long

    /** Dismissed suggestion ids so "Keep" is sticky for the session. */
    private val dismissedSuggestions = MutableStateFlow<Set<Long>>(emptySet())

    /** Event log snapshot, refreshed whenever the pending list changes. */
    private val eventLog = MutableStateFlow<List<TaskEventEntity>>(emptyList())

    val uiState: StateFlow<TodayUiState>

    init {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        dayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        dayEnd = cal.timeInMillis

        // Refresh the event log whenever tasks change — most task mutations
        // also write an event, so pending-list churn is a good trigger.
        viewModelScope.launch {
            repository.pendingTasks.collect { eventLog.value = repository.allEvents() }
        }

        uiState = combine(
            repository.pendingTasks,
            repository.completedBetween(dayStart, dayEnd),
            eventLog,
            dismissedSuggestions,
        ) { pending, completed, events, dismissed ->
            buildState(pending, completed, events, dismissed)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(),
        )
    }

    private fun buildState(
        pending: List<TaskEntity>,
        completed: List<TaskEntity>,
        events: List<TaskEventEntity>,
        dismissed: Set<Long>,
    ): TodayUiState {
        val now = System.currentTimeMillis()

        val overdue = pending.filter { it.scheduledAt != null && it.scheduledAt < dayStart }
        val upcoming = pending.filter { it.scheduledAt != null && it.scheduledAt in dayStart until dayEnd && it.scheduledAt >= now }
        // Scheduled today but its slot has already passed without completion.
        val missedToday = pending.filter { it.scheduledAt != null && it.scheduledAt in dayStart until now }
        val later = pending.filter { it.scheduledAt != null && it.scheduledAt >= dayEnd }
        val anytime = pending.filter { it.scheduledAt == null }

        // Rule-engine suggestions, one per task, minus dismissed ones.
        val suggestions = pending
            .filter { it.id !in dismissed }
            .mapNotNull { task -> AdaptationEngine.evaluate(task, events)?.let { task.id to it } }
            .toMap()

        return TodayUiState(
            dateTitle = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
            overdue = (overdue + missedToday).distinctBy { it.id },
            upcoming = upcoming,
            later = later,
            anytime = anytime,
            completedToday = completed,
            suggestions = suggestions,
        )
    }

    /** Mark a pending task complete (also cancels its pending reminder). */
    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.completeTask(task)
            ReminderScheduler.cancel(getApplication(), task.id)
        }
    }

    /** Delete a task and cancel its reminder. */
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            ReminderScheduler.cancel(getApplication(), task.id)
        }
    }

    /** Accept an adaptation: reschedule the task and re-book the reminder. */
    fun applySuggestion(task: TaskEntity, adaptation: Adaptation) {
        viewModelScope.launch {
            repository.rescheduleTask(task, adaptation.newScheduledAt)
            ReminderScheduler.schedule(getApplication(), task.id, adaptation.newScheduledAt)
        }
    }

    /** Dismiss ("Keep") a suggestion for this session. */
    fun dismissSuggestion(taskId: Long) {
        dismissedSuggestions.value = dismissedSuggestions.value + taskId
    }
}
