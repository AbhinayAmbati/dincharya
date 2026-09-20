package com.dincharya.app.ui.screens.review

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dincharya.app.app.Graph
import com.dincharya.app.data.EventOutcome
import com.dincharya.app.data.TaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/** Immutable snapshot of the evening review. */
data class ReviewUiState(
    val completedToday: List<TaskEntity> = emptyList(),
    val movedToday: Int = 0,
    val stillPending: Int = 0,
    val alreadyReviewed: Boolean = false,
)

/**
 * ViewModel for the Evening Review — the 30-second close of the day.
 *
 * Besides showing the user their day, the review is the moment Dincharya's
 * own loop closes: it nudges reflection and keeps the habit of checking in
 * (the single strongest predictor of whether users keep the app).
 */
class ReviewViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState

    /** The one-sentence reflection the user types, kept in local settings. */
    var note by mutableStateOf(Graph.settings.reviewNote)

    private var dayStart: Long = 0L
    private var dayEnd: Long = 0L

    init {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        dayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        dayEnd = cal.timeInMillis
        refresh()
    }

    /** Recompute today's numbers from the DB. */
    fun refresh() {
        viewModelScope.launch {
            val repository = Graph.repository
            val events = repository.allEvents()

            val completed = events.filter {
                it.outcome == EventOutcome.COMPLETED.name && it.occurredAt in dayStart until dayEnd
            }
            val moved = events.count {
                it.occurredAt in dayStart until dayEnd &&
                    (it.outcome == EventOutcome.SNOOZED.name || it.outcome == EventOutcome.RESCHEDULED.name)
            }
            // Pending tasks as of right now — take the first emission of
            // the live flow (an infinite cold flow, so `first()` is required).
            val pending = repository.pendingTasks.first()

            _uiState.value = ReviewUiState(
                completedToday = completed.mapNotNull { repository.taskById(it.taskId) },
                movedToday = moved,
                stillPending = pending.size,
                alreadyReviewed = Graph.settings.lastReviewDayStart == dayStart,
            )
        }
    }

    /** Persist the reflection note. */
    fun saveNote() {
        Graph.settings.reviewNote = note
    }

    /** Mark today as reviewed. */
    fun markReviewed() {
        saveNote()
        Graph.settings.lastReviewDayStart = dayStart
        refresh()
    }
}
