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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
@Composable
fun InsightsScreen(navController: NavController) {
    // Plain viewModel(): the default factory constructs AndroidViewModel(Application)
    // subclasses automatically, so no explicit factory is needed.
    val viewModel: InsightsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

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
