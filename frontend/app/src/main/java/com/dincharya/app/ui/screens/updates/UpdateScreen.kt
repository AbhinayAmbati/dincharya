package com.dincharya.app.ui.screens.updates

import android.content.Intent
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dincharya.app.R
import com.dincharya.app.app.APP_VERSION
import com.dincharya.app.ui.components.RuleCard
import com.dincharya.app.ui.components.SectionHeader

/**
 * Update screen — the one deliberately online corner of a local-first app.
 *
 * It asks GitHub for the latest published release, shows the release notes
 * rendered (no raw # and * markup), and on "Update" downloads the APK
 * in-app and hands it to Android's own installer. Nothing installs without
 * the user confirming it in the system dialog — the user stays in charge,
 * exactly like every other decision this app makes.
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
                APP_VERSION,
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
                        ReleaseNotes(s.latest.notes)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (s.updateAvailable) {
                        UpdateControls(
                            viewModel = viewModel,
                            apkUrl = s.latest.apkUrl,
                            pageUrl = s.latest.pageUrl,
                            context = context,
                        )
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

/**
 * The Update button and its states: idle, downloading (with a flat progress
 * bar), awaiting the system installer's verdict, or failed with a retry.
 */
@Composable
private fun UpdateControls(
    viewModel: UpdateViewModel,
    apkUrl: String,
    pageUrl: String,
    context: android.content.Context,
) {
    val install by viewModel.install.collectAsState()

    when (val i = install) {
        is UpdateViewModel.InstallState.Downloading -> {
            // Flat track, no percentage clutter — the monochrome way.
            LinearProgressIndicator(
                progress = { i.percent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.updates_downloading, i.percent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is UpdateViewModel.InstallState.AwaitingInstall -> {
            Text(
                stringResource(R.string.updates_installing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { viewModel.downloadAndInstall(apkUrl) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.updates_reinstall_hint)) }
        }

        is UpdateViewModel.InstallState.Failed -> {
            Text(
                i.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { viewModel.downloadAndInstall(apkUrl) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.updates_install)) }
        }

        is UpdateViewModel.InstallState.NeedsPermission -> {
            Text(
                stringResource(R.string.updates_need_permission),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { viewModel.openInstallPermissionSettings() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.updates_allow_installs)) }
        }

        is UpdateViewModel.InstallState.Idle -> {
            Button(
                onClick = { viewModel.downloadAndInstall(apkUrl) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.updates_install)) }
        }
    }
    Spacer(Modifier.height(6.dp))
    OutlinedButton(
        onClick = { open(context, pageUrl) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.updates_view_page)) }
    TextButton(
        onClick = { open(context, apkUrl) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.updates_browser_fallback), style = MaterialTheme.typography.labelMedium) }
}

/**
 * A small, deliberate markdown subset for release notes: headings (# .. ###),
 * bullet lists, horizontal rules, paragraphs and **bold** spans. No library,
 * no HTML — just enough typesetting for GitHub release bodies, Dincharya-style.
 */
@Composable
private fun ReleaseNotes(markdown: String) {
    Column {
        markdown.lines().forEach { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> Spacer(Modifier.height(8.dp))

                line == "---" || line == "***" -> Spacer(Modifier.height(4.dp))

                line.startsWith("### ") -> Text(
                    line.removePrefix("### "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )

                line.startsWith("## ") -> Text(
                    line.removePrefix("## "),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )

                line.startsWith("# ") -> Text(
                    line.removePrefix("# "),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )

                line.startsWith("- ") || line.startsWith("* ") -> Row {
                    Text("·  ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        renderInline(line.substring(2)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                else -> Text(
                    renderInline(line),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Renders **bold** spans into a styled string; everything else stays plain. */
private fun renderInline(text: String): AnnotatedString = buildAnnotatedString {
    val plain = StringBuilder()
    val boldSpans = mutableListOf<Pair<Int, Int>>()
    var boldStart = -1
    var i = 0
    while (i < text.length) {
        if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
            if (boldStart >= 0) {
                boldSpans += boldStart to plain.length
                boldStart = -1
            } else {
                boldStart = plain.length
            }
            i += 2
        } else {
            plain.append(text[i])
            i += 1
        }
    }
    append(plain)
    boldSpans.forEach { (start, end) ->
        addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, end)
    }
}

/** Open [url] in the browser — the release page, under the user's eye. */
private fun open(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
