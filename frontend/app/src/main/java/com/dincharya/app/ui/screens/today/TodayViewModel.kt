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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Immutable snapshot of the Today screen.
 *
 * Tasks are split by urgency: missed (scheduled before now — including
 * leftovers from earlier days), upcoming (later today) and anytime (no
 * scheduled time). Future-dated tasks are deliberately NOT shown: a task
 * appears on the day it occurs, never before.
 * Each suggestion pairs the task with the [Adaptation] proposed for it.
 */
data class TodayUiState(
    val dateTitle: String = "",
    val overdue: List<TaskEntity> = emptyList(),
    val upcoming: List<TaskEntity> = emptyList(),
    val anytime: List<TaskEntity> = emptyList(),
    val completedToday: List<TaskEntity> = emptyList(),
    val suggestions: Map<Long, Adaptation> = emptyMap(),
)

/**
 * ViewModel for the Today screen: streams the live task list, splits it into
 * sections, and surfaces adaptation suggestions from the rule engine.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class TodayViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = Graph.repository

    /** Today's window [startOfDay, startOfTomorrow) — a state so a refresh
     *  (pull-to-refresh, or the app sitting open past midnight) can roll it. */
    private data class DayWindow(val start: Long, val end: Long)

    private val dayWindow = MutableStateFlow(currentWindow())

    /** Dismissed suggestion ids so "Keep" is sticky for the session. */
    private val dismissedSuggestions = MutableStateFlow<Set<Long>>(emptySet())

    /** Event log snapshot, refreshed whenever the pending list changes. */
    private val eventLog = MutableStateFlow<List<TaskEventEntity>>(emptyList())

    val uiState: StateFlow<TodayUiState>

    init {
        // Refresh the event log whenever tasks change — most task mutations
        // also write an event, so pending-list churn is a good trigger.
        viewModelScope.launch {
            repository.pendingTasks.collect { eventLog.value = repository.allEvents() }
        }

        uiState = combine(
            repository.pendingTasks,
            dayWindow,
            dayWindow.flatMapLatest { window ->
                repository.completedBetween(window.start, window.end)
            },
            eventLog,
            dismissedSuggestions,
        ) { pending, window, completed, events, dismissed ->
            buildState(pending, completed, events, dismissed, window)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(),
        )
    }

    /** Midnight-to-midnight window for "today". */
    private fun currentWindow(): DayWindow {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return DayWindow(start, cal.timeInMillis)
    }

    /**
     * Pull-to-refresh: roll the day window (covers the app staying open
     * past midnight) — everything else is live already, so this is instant.
     */
    fun refreshDay() {
        dayWindow.value = currentWindow()
    }

    private fun buildState(
        pending: List<TaskEntity>,
        completed: List<TaskEntity>,
        events: List<TaskEventEntity>,
        dismissed: Set<Long>,
        window: DayWindow,
    ): TodayUiState {
        val now = System.currentTimeMillis()
        val dayStart = window.start
        val dayEnd = window.end

        val overdue = pending.filter { it.scheduledAt != null && it.scheduledAt < dayStart }
        val upcoming = pending.filter { it.scheduledAt != null && it.scheduledAt in dayStart until dayEnd && it.scheduledAt >= now }
        // Scheduled today but its slot has already passed without completion.
        val missedToday = pending.filter { it.scheduledAt != null && it.scheduledAt in dayStart until now }
        val anytime = pending.filter { it.scheduledAt == null }

        // Rule-engine suggestions, one per *visible* task, minus dismissed
        // ones — future occurrences stay out of sight and out of the way.
        val visible = overdue + missedToday + upcoming + anytime
        val suggestions = visible
            .filter { it.id !in dismissed }
            .mapNotNull { task -> AdaptationEngine.evaluate(task, events)?.let { task.id to it } }
            .toMap()

        return TodayUiState(
            dateTitle = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
            overdue = (overdue + missedToday).distinctBy { it.id },
            upcoming = upcoming,
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

    /**
     * Undo a completion: the task returns to pending, the spawned next
     * occurrence (recurring tasks) is removed with its reminder, and this
     * task's own reminder comes back if its slot is still ahead of us.
     */
    fun undoTask(task: TaskEntity) {
        viewModelScope.launch {
            val childId = repository.undoCompletion(task)
            childId?.let { ReminderScheduler.cancel(getApplication(), it) }
            task.scheduledAt?.let { at ->
                if (at > System.currentTimeMillis()) {
                    ReminderScheduler.schedule(getApplication(), task.id, at)
                }
            }
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
