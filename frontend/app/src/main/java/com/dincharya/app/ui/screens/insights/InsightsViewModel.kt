package com.dincharya.app.ui.screens.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dincharya.app.app.Graph
import com.dincharya.app.data.EventOutcome
import com.dincharya.app.data.TaskEventEntity
import com.dincharya.app.learning.AdaptationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Aggregated statistics shown on the Insights screen.
 * All numbers are derived from the raw event log only — no shortcuts.
 */
data class InsightsUiState(
    /** Total behavioural events analysed. */
    val totalEvents: Int = 0,

    /** Completed / (completed + snoozed + ignored), 0..1. */
    val completionRate: Float = 0f,

    /** Hour of day with the best completion rate (needs enough samples). */
    val bestHour: Int? = null,

    /** Completion stats for 4-hour windows of the day, in order. */
    val windowStats: List<WindowStat> = emptyList(),

    /** Completion stats per task category. */
    val categoryStats: List<CategoryStat> = emptyList(),

    /** Total snoozes — the procrastination signal. */
    val snoozeCount: Int = 0,

    /** Rate for morning window (06-12) — null when no data yet. */
    val morningRate: Float? = null,

    /** Rate for evening window (18-24) — null when no data yet. */
    val eveningRate: Float? = null,

    /** Consecutive days (ending today or yesterday) with at least one completion. */
    val streakDays: Int = 0,

    /** One heatmap cell per day, oldest first — last [HEATMAP_DAYS] days. */
    val heatmap: List<HeatCell> = emptyList(),

    /** Completions in the last 7 days. */
    val weeklyCompleted: Int = 0,

    /** Snoozes in the last 7 days. */
    val weeklySnoozed: Int = 0,

    /** Tasks pending right now. */
    val pendingNow: Int = 0,
)

/** One day of the completion heatmap. */
data class HeatCell(
    /** Midnight (epoch millis) of the day. */
    val dayStart: Long,
    /** Completions that day. */
    val count: Int,
)

/** Completion rate inside one labelled window of the day. */
data class WindowStat(val label: String, val rate: Float, val samples: Int)

/** Completion stats for one task category. */
data class CategoryStat(val category: String, val completed: Int, val total: Int) {
    val rate: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

/**
 * ViewModel for the Insights screen — the "honest mirror".
 *
 * Computes all statistics from [Graph.repository]'s event log on open. This
 * is deliberately snapshot-based (not a Flow): the log only changes through
 * task interactions, and a stale-but-correct mirror is fine between visits.
 */
class InsightsViewModel(app: Application) : AndroidViewModel(app) {

    /** Days shown in the heatmap — 13 weeks, Monday-first. */
    val heatmapDays: Int = 91

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState

    init {
        refresh()
    }

    /** Recompute everything from the current event log. */
    fun refresh() {
        viewModelScope.launch {
            val events = Graph.repository.allEvents()
            val pending = Graph.repository.pendingTasksOnce()
            _uiState.value = computeStats(events, pending.size)
        }
    }

    private fun computeStats(events: List<TaskEventEntity>, pendingNow: Int): InsightsUiState {
        if (events.isEmpty()) return InsightsUiState(pendingNow = pendingNow)

        val byHour = AdaptationEngine.completionRateByHour(events)
        val bestHour = AdaptationEngine.bestHour(events)

        // Sample-bearing outcomes only (see AdaptationEngine.HourStat).
        val samples = events.filter {
            it.outcome in listOf(
                EventOutcome.COMPLETED.name,
                EventOutcome.SNOOZED.name,
                EventOutcome.IGNORED.name,
            )
        }
        val completedCount = samples.count { it.outcome == EventOutcome.COMPLETED.name }

        // 4-hour windows from 06:00 to 22:00 plus an "early/late" bucket.
        val windows = listOf(
            "06–10" to (6..9), "10–14" to (10..13), "14–18" to (14..17), "18–22" to (18..21),
        ).map { (label, hours) ->
            val inWindow = samples.filter { it.hourOfDay in hours }
            val done = inWindow.count { it.outcome == EventOutcome.COMPLETED.name }
            WindowStat(
                label = label,
                rate = if (inWindow.isEmpty()) 0f else done.toFloat() / inWindow.size,
                samples = inWindow.size,
            )
        }

        // Category stats, sorted by activity (most-used first).
        val categoryStats = samples
            .groupBy { it.category }
            .map { (category, list) ->
                CategoryStat(
                    category = category,
                    completed = list.count { it.outcome == EventOutcome.COMPLETED.name },
                    total = list.size,
                )
            }
            .sortedByDescending { it.total }

        val morning = samples.filter { it.hourOfDay in 6..11 }
        val evening = samples.filter { it.hourOfDay in 18..23 }

        // ---- Streak + heatmap + weekly, all from completion days ----
        val completionDays = events
            .filter { it.outcome == EventOutcome.COMPLETED.name }
            .map { dayStartOf(it.occurredAt) }
            .toSortedSet()

        val streak = currentStreak(completionDays)
        val heatmap = heatmapCells(completionDays, heatmapDays)
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000

        return InsightsUiState(
            totalEvents = events.size,
            completionRate = if (samples.isEmpty()) 0f else completedCount.toFloat() / samples.size,
            bestHour = bestHour,
            windowStats = windows,
            categoryStats = categoryStats,
            snoozeCount = events.count { it.outcome == EventOutcome.SNOOZED.name },
            morningRate = morning.takeIf { it.isNotEmpty() }
                ?.let { it.count { e -> e.outcome == EventOutcome.COMPLETED.name }.toFloat() / it.size },
            eveningRate = evening.takeIf { it.isNotEmpty() }
                ?.let { it.count { e -> e.outcome == EventOutcome.COMPLETED.name }.toFloat() / it.size },
            streakDays = streak,
            heatmap = heatmap,
            weeklyCompleted = events.count {
                it.outcome == EventOutcome.COMPLETED.name && it.occurredAt >= weekAgo
            },
            weeklySnoozed = events.count {
                it.outcome == EventOutcome.SNOOZED.name && it.occurredAt >= weekAgo
            },
            pendingNow = pendingNow,
        )
    }

    /** Midnight of [at], in the device timezone. */
    private fun dayStartOf(at: Long): Long =
        java.util.Calendar.getInstance().apply {
            timeInMillis = at
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

    /**
     * Consecutive days with at least one completion, counting back from
     * today (or yesterday — a streak survives until the day is over).
     */
    private fun currentStreak(completionDays: Set<Long>): Int {
        if (completionDays.isEmpty()) return 0
        var day = dayStartOf(System.currentTimeMillis())
        // Today not done yet (or the day just started) — the streak counts
        // from yesterday as long as yesterday was a completion day.
        if (day !in completionDays) day -= 24L * 60 * 60 * 1000
        var streak = 0
        while (day in completionDays) {
            streak += 1
            day -= 24L * 60 * 60 * 1000
        }
        return streak
    }

    /** One cell per day for the last [days] days, oldest first. */
    private fun heatmapCells(completionDays: Set<Long>, days: Int): List<HeatCell> {
        val today = dayStartOf(System.currentTimeMillis())
        val firstDay = today - (days - 1) * 24L * 60 * 60 * 1000
        return (0 until days).map { offset ->
            val day = firstDay + offset * 24L * 60 * 60 * 1000
            HeatCell(dayStart = day, count = completionDays.count { it == day })
        }
    }
}
