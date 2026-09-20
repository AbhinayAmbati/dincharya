package com.dincharya.app.ui.screens.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.dincharya.app.R
import com.dincharya.app.app.SettingsStore
import com.dincharya.app.ui.components.RuleCard
import com.dincharya.app.ui.components.SectionHeader
import com.dincharya.app.ui.navigation.Screen

/** App version shown in About — keep in sync with app/build.gradle.kts. */
private const val APP_VERSION = "0.1.0"

/**
 * Settings screen: appearance, reminders, and the privacy promise.
 * Every row is a single, self-explanatory control — no sub-screens.
 */
@Composable
fun SettingsScreen(navController: NavController) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(this[APPLICATION_KEY] as android.app.Application) }
        }
    )

    // Local mirrors so the switches feel instant while persisting.
    var themeMode by remember { mutableStateOf(viewModel.themeMode) }
    var notifications by remember { mutableStateOf(viewModel.notificationsEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.settings_header), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // ---- Appearance ----
        SectionHeader(stringResource(R.string.settings_appearance))
        Spacer(Modifier.height(8.dp))
        ThemeOption(
            label = stringResource(R.string.settings_theme_system),
            selected = themeMode == SettingsStore.THEME_SYSTEM,
        ) { themeMode = SettingsStore.THEME_SYSTEM; viewModel.setTheme(SettingsStore.THEME_SYSTEM) }
        ThemeOption(
            label = stringResource(R.string.settings_theme_light),
            selected = themeMode == SettingsStore.THEME_LIGHT,
        ) { themeMode = SettingsStore.THEME_LIGHT; viewModel.setTheme(SettingsStore.THEME_LIGHT) }
        ThemeOption(
            label = stringResource(R.string.settings_theme_dark),
            selected = themeMode == SettingsStore.THEME_DARK,
        ) { themeMode = SettingsStore.THEME_DARK; viewModel.setTheme(SettingsStore.THEME_DARK) }
        Spacer(Modifier.height(20.dp))

        // ---- Reminders ----
        SectionHeader(stringResource(R.string.settings_notifications))
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_notifications_enabled),
                    style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.settings_notifications_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = notifications,
                onCheckedChange = {
                    notifications = it
                    viewModel.setNotificationsEnabled(it)
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.settings_chronotype, viewModel.chronotype),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(20.dp))

        // ---- About ----
        SectionHeader(stringResource(R.string.settings_about))
        Spacer(Modifier.height(8.dp))
        RuleCard {
            Text(
                "Dincharya — the schedule that learns you.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.settings_privacy_line),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.settings_version, APP_VERSION),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            viewModel.replayOnboarding()
            navController.navigate(Screen.Onboarding.route) {
                popUpTo(Screen.Today.route) { inclusive = true }
            }
        }) {
            Text(stringResource(R.string.settings_replay_onboarding))
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** One selectable theme row; the selected one is bold. */
@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = (if (selected) "— " else "") + label,
        style = if (selected) MaterialTheme.typography.titleMedium
        else MaterialTheme.typography.bodyMedium,
        color = if (selected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    )
}
