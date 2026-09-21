package com.dincharya.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dincharya.app.R
import com.dincharya.app.ui.components.EmptyState
import com.dincharya.app.ui.components.RuleCard
import com.dincharya.app.ui.components.SectionHeader
import java.text.NumberFormat
import java.util.Locale

/**
 * Insights screen — the honest mirror.
 *
 * Presented as a calm, text-first page: one number the user cares about
 * (completion rate), the best window, per-time-of-day bars and per-category
 * stats. Bars communicate through LENGTH, not colour (accessibility rule).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(navController: NavController) {
    // Plain viewModel(): the default factory constructs AndroidViewModel(Application)
    // subclasses automatically, so no explicit factory is needed.
    val viewModel: InsightsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    // Pull down from the top to recompute the mirror on demand.
    val pullState = rememberPullToRefreshState()
    if (pullState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
            pullState.endRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullState.nestedScrollConnection),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.insights_header), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.insights_events_analysed, state.totalEvents),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        if (state.totalEvents == 0) {
            EmptyState(stringResource(R.string.insights_empty))
            Spacer(Modifier.height(32.dp))
            return@Column
        }

        // ---- Headline numbers ----
        val percent = NumberFormat.getPercentInstance(Locale.getDefault())
        StatRow(
            label = stringResource(R.string.insights_completion_rate),
            value = percent.format(state.completionRate),
        )
        StatRow(
            label = stringResource(R.string.insights_best_window),
            value = state.bestHour?.let { "%02d:00".format(it) }
                ?: stringResource(R.string.insights_no_best_window),
        )
        StatRow(
            label = stringResource(R.string.insights_snoozed),
            value = state.snoozeCount.toString(),
        )
        StatRow(
            label = stringResource(R.string.insights_streak),
            value = stringResource(R.string.insights_streak_days, state.streakDays),
        )

        // ---- Weekly report ----
        SectionHeader(stringResource(R.string.insights_weekly), Modifier.padding(top = 20.dp))
        Spacer(Modifier.height(8.dp))
        RuleCard {
            Text(
                stringResource(
                    R.string.insights_weekly_line,
                    state.weeklyCompleted,
                    state.weeklySnoozed,
                    state.pendingNow,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // ---- Consistency heatmap ----
        SectionHeader(stringResource(R.string.insights_heatmap), Modifier.padding(top = 20.dp))
        Spacer(Modifier.height(8.dp))
        CompletionHeatmap(cells = state.heatmap)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.insights_heatmap_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ---- The honest mirror line ----
        if (state.morningRate != null && state.eveningRate != null) {
            Spacer(Modifier.height(8.dp))
            RuleCard {
                Text(stringResource(R.string.insights_honest_mirror), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "You complete ${percent.format(state.morningRate)} of morning tasks, " +
                        "but only ${percent.format(state.eveningRate)} of evening ones.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // ---- Completion by time of day ----
        SectionHeader(stringResource(R.string.insights_by_time), Modifier.padding(top = 20.dp))
        Spacer(Modifier.height(8.dp))
        state.windowStats.forEach { window ->
            RateBar(label = window.label, rate = window.rate, samples = window.samples)
        }

        // ---- By category ----
        if (state.categoryStats.isNotEmpty()) {
            SectionHeader(stringResource(R.string.insights_by_category), Modifier.padding(top = 20.dp))
            Spacer(Modifier.height(8.dp))
            state.categoryStats.forEach { category ->
                RateBar(
                    label = category.category.lowercase(Locale.US).replaceFirstChar { it.uppercase() },
                    rate = category.rate,
                    samples = category.total,
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        }

        // The pull-to-refresh spinner sits above the content, centred.
        PullToRefreshContainer(
            state = pullState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/**
 * GitHub-style consistency grid: one small square per day, arranged in
 * week columns (oldest on the left, today on the right). Intensity is
 * monochrome ALPHA, never colour — the accessibility rule again.
 */
@Composable
private fun CompletionHeatmap(cells: List<com.dincharya.app.ui.screens.insights.HeatCell>) {
    if (cells.isEmpty()) return
    val max = cells.maxOf { it.count }.coerceAtLeast(1)
    // Pad the oldest week so the grid always starts on a Monday.
    val firstDow = java.util.Calendar.getInstance().apply {
        timeInMillis = cells.first().dayStart
    }.get(java.util.Calendar.DAY_OF_WEEK)
    val leading = (firstDow + 5) % 7 // Calendar.MONDAY == 2
    val padded = List(leading) { null } + cells

    androidx.compose.foundation.lazy.grid.LazyHorizontalGrid(
        rows = androidx.compose.foundation.lazy.grid.GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        userScrollEnabled = false,
    ) {
        items(padded.size) { index ->
            val cell = padded[index]
            val alpha = when {
                cell == null -> 0f
                cell.count == 0 -> 0.12f
                else -> 0.3f + 0.7f * (cell.count.toFloat() / max)
            }
            Box(
                Modifier
                    .size(12.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                    )
            )
        }
    }
}

/** A label + big value line for the top of the screen. */
@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

/**
 * A horizontal rate bar: label on the left, a track whose FILL shows the
 * rate, and the sample count so users know how much to trust it.
 */
@Composable
private fun RateBar(label: String, rate: Float, samples: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 12.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .background(MaterialTheme.colorScheme.outline),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(rate.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        }
        Text(
            "  $samples",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
