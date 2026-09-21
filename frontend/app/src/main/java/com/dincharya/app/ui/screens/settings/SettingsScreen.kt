package com.dincharya.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dincharya.app.R
import com.dincharya.app.app.SettingsStore
import com.dincharya.app.ui.components.RuleCard
import com.dincharya.app.ui.components.SectionHeader
import com.dincharya.app.ui.navigation.Screen
import kotlinx.coroutines.launch

/**
 * Settings screen: appearance, reminders, and the privacy promise.
 *
 * Layout language matches the rest of the app: grouped sections opened by
 * a hairline kicker, full-width rows with a leading thin-stroke icon,
 * generous spacing. Every row is a single, self-explanatory control.
 */
@Composable
fun SettingsScreen(navController: NavController) {
    // Plain viewModel(): the default factory constructs AndroidViewModel(Application)
    // subclasses automatically, so no explicit factory is needed.
    val viewModel: SettingsViewModel = viewModel()

    // Local mirrors so the switches feel instant while persisting.
    var themeMode by remember { mutableStateOf(viewModel.themeMode) }
    var notifications by remember { mutableStateOf(viewModel.notificationsEnabled) }

    val exportScope = rememberCoroutineScope()
    val exportContext = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.settings_header), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))

        // ---- Appearance ----
        SectionHeader(stringResource(R.string.settings_appearance))
        Spacer(Modifier.height(8.dp))
        ThemeOption(
            icon = Icons.Outlined.Devices,
            label = stringResource(R.string.settings_theme_system),
            selected = themeMode == SettingsStore.THEME_SYSTEM,
        ) { themeMode = SettingsStore.THEME_SYSTEM; viewModel.setTheme(SettingsStore.THEME_SYSTEM) }
        ThemeOption(
            icon = Icons.Outlined.LightMode,
            label = stringResource(R.string.settings_theme_light),
            selected = themeMode == SettingsStore.THEME_LIGHT,
        ) { themeMode = SettingsStore.THEME_LIGHT; viewModel.setTheme(SettingsStore.THEME_LIGHT) }
        ThemeOption(
            icon = Icons.Outlined.DarkMode,
            label = stringResource(R.string.settings_theme_dark),
            selected = themeMode == SettingsStore.THEME_DARK,
        ) { themeMode = SettingsStore.THEME_DARK; viewModel.setTheme(SettingsStore.THEME_DARK) }
        Spacer(Modifier.height(24.dp))

        // ---- Reminders ----
        SectionHeader(stringResource(R.string.settings_notifications))
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_notifications_enabled),
                    style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.settings_notifications_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(16.dp))
            Switch(
                checked = notifications,
                onCheckedChange = {
                    notifications = it
                    viewModel.setNotificationsEnabled(it)
                },
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.settings_chronotype, viewModel.chronotype),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        // ---- Data ----
        SectionHeader(stringResource(R.string.settings_data_section))
        Spacer(Modifier.height(12.dp))
        SettingsLink(
            icon = Icons.Outlined.Download,
            label = stringResource(R.string.settings_export),
        ) {
            // Export every task + event to a JSON file and hand it to the
            // system share sheet — the user decides where it lands.
            exportScope.launch {
                val file = viewModel.exportData()
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    exportContext,
                    exportContext.packageName + ".fileprovider",
                    file,
                )
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                exportContext.startActivity(
                    android.content.Intent.createChooser(send, file.name)
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        // ---- About ----
        SectionHeader(stringResource(R.string.settings_about))
        Spacer(Modifier.height(12.dp))
        RuleCard {
            Text(
                "Dincharya: the schedule that learns you.",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_privacy_line),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_version, com.dincharya.app.app.APP_VERSION),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        SettingsLink(
            icon = Icons.Outlined.SystemUpdate,
            label = stringResource(R.string.settings_check_updates),
        ) { navController.navigate(Screen.Updates.route) }
        SettingsLink(
            icon = Icons.Outlined.PrivacyTip,
            label = stringResource(R.string.privacy_title),
        ) { navController.navigate(Screen.Privacy.route) }
        SettingsLink(
            icon = Icons.Outlined.Description,
            label = stringResource(R.string.terms_title),
        ) { navController.navigate(Screen.Terms.route) }
        SettingsLink(
            icon = Icons.Outlined.Replay,
            label = stringResource(R.string.settings_replay_onboarding),
        ) {
            viewModel.replayOnboarding()
            navController.navigate(Screen.Onboarding.route) {
                popUpTo(Screen.Today.route) { inclusive = true }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * One selectable theme row: leading icon, label, monochrome selection dot.
 * A filled dot marks the active theme, an outlined ring the others —
 * shape, never colour, carries the state. Full row is tappable.
 */
@Composable
private fun ThemeOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(14.dp))
        Text(
            text = label,
            style = if (selected) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.size(8.dp))
        Box(
            modifier = Modifier
                .size(18.dp)
                .then(
                    if (selected) {
                        Modifier.background(MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    }
                ),
        )
    }
}

/**
 * A navigation row: leading thin-stroke icon, label, trailing chevron.
 * Used for the Privacy Policy, Terms of Use and replay-introduction links.
 */
@Composable
private fun SettingsLink(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
    }
}
