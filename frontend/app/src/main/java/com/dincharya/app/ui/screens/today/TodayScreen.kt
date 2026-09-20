package com.dincharya.app.ui.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.navigation.NavController
import com.dincharya.app.R
import com.dincharya.app.data.TaskEntity
import com.dincharya.app.ui.components.EmptyState
import com.dincharya.app.ui.components.MomentumBar
import com.dincharya.app.ui.components.RuleCard
import com.dincharya.app.ui.components.SectionHeader
import com.dincharya.app.ui.components.TaskRow
import com.dincharya.app.ui.navigation.Screen

/**
 * The Today screen — the app's home.
 *
 * Layout top-to-bottom: date, momentum bar, adaptation suggestions, then
 * task sections (overdue / today / anytime / later / done). One flat list,
 * no nesting — light and fast by design.
 */
@Composable
fun TodayScreen(navController: NavController) {
    val viewModel: TodayViewModel = viewModel(
        factory = viewModelFactory {
            // APPLICATION_KEY is the Application instance, provided by Android.
            initializer { TodayViewModel(this[APPLICATION_KEY] as android.app.Application) }
        }
    )
    val state by viewModel.uiState.collectAsState()

    // Task pending deletion — wired to a confirmation dialog below.
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---- Header ----
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(20.dp))
            Text(state.dateTitle, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.momentum_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                MomentumBar(completed = state.completedToday.size, total = state.completedToday.size + state.overdue.size + state.upcoming.size)
            }
            Spacer(Modifier.height(12.dp))
        }

        // ---- Task list ----
        if (state.overdue.isEmpty() && state.upcoming.isEmpty() &&
            state.anytime.isEmpty() && state.later.isEmpty() && state.completedToday.isEmpty()
        ) {
            EmptyState(stringResource(R.string.today_empty))
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                // Adaptation suggestions first — the app's learning made visible.
                if (state.suggestions.isNotEmpty()) {
                    item {
                        state.suggestions.forEach { (taskId, adaptation) ->
                            SuggestionCard(
                                reason = adaptation.reason,
                                onApply = {
                                    // Find the task in any section to apply the move.
                                    val task = state.overdue.firstOrNull { it.id == taskId }
                                        ?: state.upcoming.firstOrNull { it.id == taskId }
                                        ?: state.later.firstOrNull { it.id == taskId }
                                        ?: state.anytime.firstOrNull { it.id == taskId }
                                    task?.let { viewModel.applySuggestion(it, adaptation) }
                                },
                                onKeep = { viewModel.dismissSuggestion(taskId) },
                            )
                        }
                    }
                }

                taskSection(stringResource(R.string.section_overdue), state.overdue) { task ->
                    TaskRow(
                        task = task,
                        onToggleComplete = { viewModel.completeTask(task) },
                        onFocus = { navController.navigate(Screen.Focus.route) },
                        onDelete = { taskToDelete = task },
                    )
                }
                taskSection(stringResource(R.string.section_upcoming), state.upcoming) { task ->
                    TaskRow(
                        task = task,
                        onToggleComplete = { viewModel.completeTask(task) },
                        onFocus = { navController.navigate(Screen.Focus.route) },
                        onDelete = { taskToDelete = task },
                    )
                }
                taskSection(stringResource(R.string.section_anytime), state.anytime) { task ->
                    TaskRow(
                        task = task,
                        onToggleComplete = { viewModel.completeTask(task) },
                        onFocus = { navController.navigate(Screen.Focus.route) },
                        onDelete = { taskToDelete = task },
                    )
                }
                taskSection("Later", state.later) { task ->
                    TaskRow(
                        task = task,
                        onToggleComplete = { viewModel.completeTask(task) },
                        onFocus = { navController.navigate(Screen.Focus.route) },
                        onDelete = { taskToDelete = task },
                    )
                }
                taskSection(stringResource(R.string.section_done_today), state.completedToday) { task ->
                    TaskRow(
                        task = task,
                        onToggleComplete = { viewModel.completeTask(task) },
                        onFocus = { },
                        onDelete = { taskToDelete = task },
                    )
                }
            }
        }
    }

    // ---- Delete confirmation ----
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text(stringResource(R.string.task_confirm_delete)) },
            text = { Text(stringResource(R.string.task_confirm_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task)
                    taskToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

/**
 * A suggestion card: the adaptation reason in plain language, with Apply /
 * Keep. This is the "suggest, don't dictate" principle made visible.
 */
@Composable
private fun SuggestionCard(
    reason: String,
    onApply: () -> Unit,
    onKeep: () -> Unit,
) {
    RuleCard(Modifier.padding(vertical = 8.dp)) {
        Column {
            Text(reason, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onApply) {
                    Text(stringResource(R.string.suggestion_apply))
                }
                TextButton(onClick = onKeep) {
                    Text(stringResource(R.string.suggestion_keep))
                }
            }
        }
    }
}

/** Emits a section header + its rows only when [tasks] is non-empty. */
private fun androidx.compose.foundation.lazy.LazyListScope.taskSection(
    title: String,
    tasks: List<TaskEntity>,
    row: @Composable (TaskEntity) -> Unit,
) {
    if (tasks.isEmpty()) return
    item { SectionHeader(title, Modifier.padding(top = 16.dp)) }
    items(tasks, key = { it.id }) { task -> row(task) }
}
