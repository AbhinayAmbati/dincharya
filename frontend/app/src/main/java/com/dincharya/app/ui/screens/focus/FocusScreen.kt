package com.dincharya.app.ui.screens.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dincharya.app.R
import com.dincharya.app.ui.components.EmptyState
import com.dincharya.app.ui.components.MomentumBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Focus screen: pick a task, run a timer for its estimated duration,
 * finish it in one sitting.
 *
 * Deliberately empty of everything else — no lists, no tabs while active.
 * The clock is the whole interface.
 */
@Composable
fun FocusScreen(navController: NavController) {
    // Plain viewModel(): the default factory constructs AndroidViewModel(Application)
    // subclasses automatically, so no explicit factory is needed.
    val viewModel: FocusViewModel = viewModel()
    val pending by viewModel.pendingTasks.collectAsState()
    val selected = viewModel.selectedTask

    if (selected == null) {
        // ---- Task picker ----
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.focus_header), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.focus_pick_task),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            if (pending.isEmpty()) {
                EmptyState(stringResource(R.string.focus_empty))
            } else {
                pending.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(task.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                            Text(
                                "${task.estimatedMinutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(onClick = { viewModel.select(task) }) {
                            Text(stringResource(R.string.focus_start))
                        }
                    }
                }
            }
        }
        return
    }

    // ---- Active session ----

    // Drive the countdown: one VM tick per second while the timer runs.
    LaunchedEffect(viewModel.running) {
        while (isActive && viewModel.running) {
            delay(1_000)
            viewModel.tick()
        }
    }

    // Confirmation before completing early or abandoning.
    var showConfirm by remember { mutableStateOf(false) }

    val remaining = viewModel.remainingSeconds
    val progress = if (viewModel.totalSeconds == 0) 0f
    else 1f - remaining.toFloat() / viewModel.totalSeconds

    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                selected.title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            // The clock — the only thing that needs to be big.
            Text(
                text = "%02d:%02d".format(remaining / 60, remaining % 60),
                fontSize = 72.sp,
                style = MaterialTheme.typography.displayLarge,
                color = if (viewModel.finished) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(24.dp))
            MomentumBar(
                completed = (progress * 100).toInt(),
                total = 100,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
            Row {
                if (!viewModel.finished) {
                    OutlinedButton(
                        onClick = { if (viewModel.running) viewModel.pause() else viewModel.start() },
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Text(
                            if (viewModel.running) stringResource(R.string.focus_pause)
                            else stringResource(R.string.focus_resume)
                        )
                    }
                }
                Button(onClick = { showConfirm = true }) {
                    Text(
                        if (viewModel.finished) stringResource(R.string.focus_done)
                        else stringResource(R.string.focus_give_up)
                    )
                }
            }
            TextButton(onClick = { viewModel.deselect() }) {
                Text(stringResource(R.string.task_delete), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showConfirm) {
        if (viewModel.finished) {
            // Timer ran out — offering completion.
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                title = { Text(stringResource(R.string.focus_confirm_done)) },
                text = { Text(selected.title) },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirm = false
                        viewModel.completeSelected(onDone = { navController.popBackStack() })
                    }) { Text(stringResource(R.string.focus_done)) }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        } else {
            // Abandoning early — the session ends but the task stays pending.
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                title = { Text(stringResource(R.string.focus_confirm_giveup)) },
                text = { Text(selected.title) },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirm = false
                        viewModel.deselect()
                        navController.popBackStack()
                    }) { Text(stringResource(R.string.focus_giveup_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }
    }
}
