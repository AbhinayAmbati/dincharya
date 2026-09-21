package com.dincharya.app.ui.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dincharya.app.R
import com.dincharya.app.data.TaskEntity
import com.dincharya.app.ui.components.EmptyState
import com.dincharya.app.ui.components.MomentumBar
import com.dincharya.app.ui.components.RuleCard
import com.dincharya.app.ui.components.SectionHeader
import com.dincharya.app.ui.components.TaskRow
import com.dincharya.app.ui.navigation.Screen
import com.dincharya.app.ui.navigation.editRoute
import com.dincharya.app.learning.PlannedTask
import java.util.Calendar

/**
 * The Today screen — the app's home.
 *
 * Layout top-to-bottom: date, momentum bar, adaptation suggestions, then
 * task sections (overdue / today / anytime / later / done). One flat list,
 * no nesting — light and fast by design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(navController: NavController) {
    // Plain viewModel(): the default factory constructs AndroidViewModel(Application)
    // subclasses automatically, so no explicit factory is needed.
    val viewModel: TodayViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    // Pull down from the top to refresh (also rolls the day window if the
    // app stayed open past midnight). Deliberately invisible: no spinner
    // circle sits on the content — the refresh is instant.
    val pullState = rememberPullToRefreshState()
    if (pullState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshDay()
            pullState.endRefresh()
        }
    }

    // Task pending deletion — wired to a confirmation dialog below.
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }

    // Section titles are resolved HERE, in a composable context: the
    // LazyColumn content lambda below is NOT composable, so stringResource()
    // may not be called inside it directly.
    val overdueTitle = stringResource(R.string.section_overdue)
    val upcomingTitle = stringResource(R.string.section_upcoming)
    val anytimeTitle = stringResource(R.string.section_anytime)
    val doneTitle = stringResource(R.string.section_done_today)
    val deleteLabel = stringResource(R.string.task_delete)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullState.nestedScrollConnection),
    ) {
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

        // ---- Plan my day ----
        if (state.dayPlan == null) {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = viewModel::generateDayPlan,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.today_plan_button))
            }
            state.planMessage?.let { message ->
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.dayPlan?.let { plan ->
            PlanCard(
                plan = plan,
                onAccept = viewModel::acceptPlan,
                onDiscard = viewModel::discardPlan,
                onNudge = viewModel::nudgePlanItem,
                onRemove = viewModel::removePlanItem,
            )
        }

        // ---- Task list ----
        if (state.overdue.isEmpty() && state.upcoming.isEmpty() &&
            state.anytime.isEmpty() && state.completedToday.isEmpty()
        ) {
            // A day with nothing on it deserves the middle of the screen,
            // not a lonely line under the header.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                EmptyState(stringResource(R.string.today_empty))
            }
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
                                        ?: state.anytime.firstOrNull { it.id == taskId }
                                    task?.let { viewModel.applySuggestion(it, adaptation) }
                                },
                                onKeep = { viewModel.dismissSuggestion(taskId) },
                            )
                        }
                    }
                }

                taskSection(overdueTitle, state.overdue) { task ->
                    TaskRow(
                        task = task,
                        onToggleComplete = { viewModel.completeTask(task) },
                        onFocus = { navController.navigate(Screen.Focus.route) },
                        onDelete = { taskToDelete = task },
                        onOpen = { navController.navigate(editRoute(task.id)) },
                    )
                }
                taskSection(upcomingTitle, state.upcoming) { task ->
                    TaskRow(
                        task = task,
                        onToggleComplete = { viewModel.completeTask(task) },
                        onFocus = { navController.navigate(Screen.Focus.route) },
                        onDelete = { taskToDelete = task },
                        onOpen = { navController.navigate(editRoute(task.id)) },
                    )
                }
                taskSection(anytimeTitle, state.anytime) { task ->
                    TaskRow(
                        task = task,
                        onToggleComplete = { viewModel.completeTask(task) },
                        onFocus = { navController.navigate(Screen.Focus.route) },
                        onDelete = { taskToDelete = task },
                        onOpen = { navController.navigate(editRoute(task.id)) },
                    )
                }
                // Done today: tapping the circle undoes the completion (and
                // removes the occurrence it spawned); tapping the row edits.
                taskSection(doneTitle, state.completedToday) { task ->
                    TaskRow(
                        task = task,
                        onToggleComplete = { viewModel.undoTask(task) },
                        onFocus = { },
                        onDelete = { taskToDelete = task },
                        onOpen = { navController.navigate(editRoute(task.id)) },
                    )
                }
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
                }) { Text(deleteLabel) }
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

/**
 * The "Plan my day" review card: every proposed placement with its slot,
 * its honest duration and its reason, nudgeable by 15 minutes or removable
 * one by one before anything is accepted. Nothing moves until Accept.
 */
@Composable
private fun PlanCard(
    plan: com.dincharya.app.learning.DayPlan,
    onAccept: () -> Unit,
    onDiscard: () -> Unit,
    onNudge: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit,
) {
    RuleCard(Modifier.padding(vertical = 12.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.today_plan_title),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDiscard) {
                    Text(stringResource(R.string.today_plan_discard))
                }
            }
            plan.items.forEach { item ->
                PlanRow(item = item, onNudge = onNudge, onRemove = onRemove)
            }
            if (plan.unplaced.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.today_plan_unplaced, plan.unplaced.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.today_plan_accept))
            }
        }
    }
}

/** One planned slot: time, task, duration and reason, with nudge and remove. */
@Composable
private fun PlanRow(
    item: PlannedTask,
    onNudge: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val time = remember(item.startAt) {
        val cal = Calendar.getInstance().apply { timeInMillis = item.startAt }
        "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Text(time, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyMedium)
            Text(
                item.reason + "  ·  " + item.durationMinutes + " min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = { onNudge(item.taskId, -15) }) { Text("−") }
        TextButton(onClick = { onNudge(item.taskId, 15) }) { Text("+") }
        TextButton(onClick = { onRemove(item.taskId) }) { Text("×") }
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
