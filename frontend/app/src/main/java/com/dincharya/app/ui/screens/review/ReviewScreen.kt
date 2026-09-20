package com.dincharya.app.ui.screens.review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.dincharya.app.R
import com.dincharya.app.ui.components.SectionHeader

/**
 * Evening Review — 30 seconds, three numbers, one optional sentence.
 * Small on purpose: reviews that feel like work stop happening.
 */
@Composable
fun ReviewScreen(navController: NavController) {
    val viewModel: ReviewViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ReviewViewModel(this[APPLICATION_KEY] as android.app.Application) }
        }
    )
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.review_header), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // ---- Three numbers ----
        Row(Modifier.fillMaxWidth()) {
            BigNumber(
                label = stringResource(R.string.review_completed),
                value = state.completedToday.size,
                modifier = Modifier.weight(1f),
            )
            BigNumber(
                label = stringResource(R.string.review_moved),
                value = state.movedToday,
                modifier = Modifier.weight(1f),
            )
            BigNumber(
                label = stringResource(R.string.review_pending),
                value = state.stillPending,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(20.dp))

        // ---- What got done ----
        if (state.completedToday.isNotEmpty()) {
            SectionHeader(stringResource(R.string.section_done_today))
            Spacer(Modifier.height(8.dp))
            state.completedToday.forEach { task ->
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---- Reflection note ----
        Text(
            stringResource(R.string.review_note_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.note,
            onValueChange = { viewModel.note = it },
            placeholder = { Text(stringResource(R.string.review_note_placeholder)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))

        if (state.alreadyReviewed) {
            Text(
                stringResource(R.string.review_already_done),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Button(
                onClick = { viewModel.markReviewed() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.review_mark_done))
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** One third of the three-number row at the top. */
@Composable
private fun BigNumber(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
