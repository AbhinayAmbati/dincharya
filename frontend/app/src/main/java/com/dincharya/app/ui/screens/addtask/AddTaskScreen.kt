package com.dincharya.app.ui.screens.addtask

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dincharya.app.R
import com.dincharya.app.data.RepeatRule
import com.dincharya.app.data.TaskCategory
import com.dincharya.app.data.TaskPriority
import com.dincharya.app.ui.components.SectionHeader

/**
 * Add Task screen — one scrollable form, no dialogs, no wizards.
 * A complete task is 6 taps + a title; everything else has a sane default.
 */
@Composable
fun AddTaskScreen(navController: NavController) {
    // Plain viewModel(): the default factory constructs AndroidViewModel(Application)
    // subclasses automatically, so no explicit factory is needed.
    val viewModel: AddTaskViewModel = viewModel()

    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(if (viewModel.isEdit) R.string.add_edit_header else R.string.add_title_header),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(16.dp))

        // ---- Title ----
        OutlinedTextField(
            value = viewModel.title,
            onValueChange = { viewModel.title = it },
            label = { Text(stringResource(R.string.add_title_label)) },
            placeholder = { Text(stringResource(R.string.add_title_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))

        // ---- Category ----
        SectionHeader(stringResource(R.string.add_category_label))
        Spacer(Modifier.height(8.dp))
        ChipRow(
            options = TaskCategory.entries.map { it.label },
            selectedIndex = TaskCategory.entries.indexOf(viewModel.category),
        ) { index -> viewModel.selectCategory(TaskCategory.entries[index]) }
        Spacer(Modifier.height(24.dp))

        // ---- Priority ----
        SectionHeader(stringResource(R.string.add_priority_label))
        Spacer(Modifier.height(8.dp))
        ChipRow(
            options = TaskPriority.entries.map { it.label },
            selectedIndex = TaskPriority.entries.indexOf(viewModel.priority),
        ) { index -> viewModel.priority = TaskPriority.entries[index] }
        Spacer(Modifier.height(24.dp))

        // ---- Duration ----
        SectionHeader(stringResource(R.string.add_duration_label))
        Spacer(Modifier.height(8.dp))
        ChipRow(
            options = viewModel.durationOptions.map { "${it} min" },
            selectedIndex = viewModel.durationOptions.indexOf(viewModel.durationMinutes),
        ) { index -> viewModel.durationMinutes = viewModel.durationOptions[index] }
        Spacer(Modifier.height(24.dp))

        // ---- Repeat ----
        SectionHeader(stringResource(R.string.add_repeat_label))
        Spacer(Modifier.height(8.dp))
        ChipRow(
            options = RepeatRule.entries.map { it.label },
            selectedIndex = RepeatRule.entries.indexOf(viewModel.repeatRule),
        ) { index -> viewModel.repeatRule = RepeatRule.entries[index] }
        Spacer(Modifier.height(24.dp))

        // ---- Subtasks ----
        SectionHeader(stringResource(R.string.add_subtasks_label))
        Spacer(Modifier.height(8.dp))
        viewModel.subtaskTitles.forEachIndexed { index, _ ->
            OutlinedTextField(
                value = viewModel.subtaskTitles[index],
                onValueChange = { viewModel.setSubtask(index, it) },
                label = { Text(stringResource(R.string.add_subtask_placeholder, index + 1)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )
        }
        TextButton(onClick = { viewModel.addSubtaskRow() }) {
            Text(stringResource(R.string.add_subtask_add))
        }
        Spacer(Modifier.height(24.dp))

        // ---- Note ----
        SectionHeader(stringResource(R.string.add_note_label))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.note,
            onValueChange = { viewModel.note = it },
            label = { Text(stringResource(R.string.add_note_placeholder)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))

        // ---- Reminder time ----
        SectionHeader(stringResource(R.string.add_time_label))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.add_time_none),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            // "Anytime" toggle — off means a specific reminder time.
            Switch(
                checked = viewModel.useTime,
                onCheckedChange = { viewModel.useTime = it },
            )
        }
        if (viewModel.useTime) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Stepper(
                    label = stringResource(R.string.add_hour),
                    value = viewModel.hour,
                    range = 0..23,
                    onChange = { viewModel.setHour(it) },
                )
                Text(":", style = MaterialTheme.typography.titleLarge)
                Stepper(
                    label = stringResource(R.string.add_minute),
                    value = viewModel.minute,
                    range = 0..59 step 5,
                    onChange = { viewModel.setMinute(it) },
                )
            }
        }
        Spacer(Modifier.height(32.dp))

        // ---- Save ----
        Button(
            onClick = { viewModel.save(onSaved = { navController.popBackStack() }) },
            enabled = viewModel.canSave && viewModel.loaded,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                stringResource(if (viewModel.isEdit) R.string.add_save_edit else R.string.add_save),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        // A completed task being edited can be reopened right here.
        if (viewModel.isEdit && viewModel.editingCompleted) {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { viewModel.markPending(onDone = { navController.popBackStack() }) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.add_mark_pending)) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** A single horizontal row of selectable chips (used for every option set). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options.size) { index ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                label = { Text(options[index]) },
            )
        }
    }
}

/**
 * Minimal +/- stepper for hour and minute. Big 44dp round buttons keep it
 * thumb-friendly; a stepper (over a time picker dialog) keeps this screen
 * fast and predictable.
 */
@Composable
private fun Stepper(
    label: String,
    value: Int,
    range: IntProgression,
    onChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleButton(text = "−") { onChange(wrapStep(value, range, -1)) }
            Text(
                text = "%02d".format(value),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .width(56.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            CircleButton(text = "+") { onChange(wrapStep(value, range, +1)) }
        }
    }
}

/** Round 44dp tap target with a typographic symbol inside. */
@Composable
private fun CircleButton(text: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** Step [value] within [range], wrapping around at both ends. */
private fun wrapStep(value: Int, range: IntProgression, step: Int): Int {
    val list = range.toList()
    val index = list.indexOf(value).coerceAtLeast(0)
    val next = (index + step).mod(list.size)
    return list[next]
}
