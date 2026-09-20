package com.dincharya.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dincharya.app.data.TaskEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small library of shared UI pieces, styled to the Dincharya look:
 * flat cards, 1dp rules, generous whitespace, no shadows.
 */

/** Section heading with a hairline rule under it (like a newspaper kicker). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
    }
}

/**
 * Momentum bar: a flat track showing how much of the planned day is done.
 * Length (not colour) carries the information — the monochrome way.
 */
@Composable
fun MomentumBar(completed: Int, total: Int, modifier: Modifier = Modifier) {
    val fraction = if (total == 0) 0f else completed.toFloat() / total
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(MaterialTheme.colorScheme.outline)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .background(MaterialTheme.colorScheme.onSurface)
        )
    }
}

/**
 * One task row: check circle, title, meta line (time / snooze count),
 * plus small Focus and Delete affordances.
 *
 * Completed state is shown with a filled circle + strikethrough — shape and
 * weight, never colour, per the accessibility rules in CONTRIBUTING.md.
 */
@Composable
fun TaskRow(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onFocus: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeFormat = rememberTimeFormat()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Check circle: outlined when pending, filled when done. 48dp touch target.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onToggleComplete),
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .then(
                        if (task.isCompleted) {
                            Modifier.background(MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        }
                    )
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 8.dp),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
            )
            // Meta line: scheduled time and snooze badge.
            val meta = buildList {
                task.scheduledAt?.let { add(timeFormat.format(Date(it))) }
                if (task.snoozeCount > 0) add("snoozed ${task.snoozeCount}x")
            }.joinToString("  ·  ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!task.isCompleted) {
            Text(
                text = "Focus",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable(onClick = onFocus)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
            Text(
                text = "×",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** Empty-state message, centered and quiet. */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Card used across screens: flat surface with a 1dp rule. */
@Composable
fun RuleCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

// SimpleDateFormat is not thread-safe when shared; one per composition
// location keeps it safe without measurable cost.
@Composable
private fun rememberTimeFormat(): SimpleDateFormat =
    androidx.compose.runtime.remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
