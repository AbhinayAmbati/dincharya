package com.dincharya.app.ui.screens.updates

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dincharya.app.R
import com.dincharya.app.ui.components.RuleCard
import com.dincharya.app.ui.components.SectionHeader

/**
 * Update screen — the one deliberately online corner of a local-first app.
 *
 * It asks GitHub for the latest published release, shows the release notes
 * verbatim, and offers plain browser links. Nothing auto-downloads and
 * nothing auto-installs: the user stays in charge of the update, exactly
 * like every other decision this app makes.
 */
@Composable
fun UpdateScreen(navController: NavController) {
    val viewModel: UpdateViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.updates_header), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.updates_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        // ---- Installed version ----
        SectionHeader(stringResource(R.string.updates_current))
        Spacer(Modifier.height(8.dp))
        RuleCard {
            Text(
                UpdateViewModel.APP_VERSION,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.updates_installed_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))

        // ---- Latest release ----
        SectionHeader(stringResource(R.string.updates_latest))
        Spacer(Modifier.height(8.dp))

        when (val s = state) {
            is UpdateViewModel.State.Idle, is UpdateViewModel.State.Checking -> {
                Button(
                    onClick = { viewModel.check() },
                    enabled = s is UpdateViewModel.State.Idle,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(
                        if (s is UpdateViewModel.State.Checking) stringResource(R.string.updates_checking)
                        else stringResource(R.string.updates_check)
                    )
                }
            }

            is UpdateViewModel.State.Result -> {
                RuleCard {
                    Text(
                        if (s.updateAvailable) {
                            stringResource(R.string.updates_new_available, s.latest.version)
                        } else {
                            stringResource(R.string.updates_up_to_date)
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        s.latest.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (s.latest.notes.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            s.latest.notes,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (s.updateAvailable) {
                        Button(
                            onClick = { open(context, s.latest.apkUrl) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.updates_download)) }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { open(context, s.latest.pageUrl) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.updates_view_page)) }
                    } else {
                        TextButton(onClick = { open(context, s.latest.pageUrl) }) {
                            Text(stringResource(R.string.updates_view_page))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { viewModel.check() }) {
                    Text(stringResource(R.string.updates_check_again))
                }
            }

            is UpdateViewModel.State.Error -> {
                Text(
                    s.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.check() }) {
                    Text(stringResource(R.string.updates_check_again))
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** Open [url] in the browser — the download happens there, under the user's eye. */
private fun open(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
