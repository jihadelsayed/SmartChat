package com.smartchat.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartchat.core.datastore.SettingsStore
import com.smartchat.core.ui.components.ConfirmationDialog
import com.smartchat.data.ChatRepository

@Composable
fun SettingsScreen(settingsStore: SettingsStore, chatRepository: ChatRepository) {
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(settingsStore, chatRepository)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        SettingSwitch(
            label = "Dark theme",
            checked = state.darkTheme,
            onCheckedChange = viewModel::setDarkTheme
        )
        SettingSwitch(
            label = "Notifications",
            checked = state.notificationsEnabled,
            onCheckedChange = viewModel::setNotificationsEnabled
        )
        OutlinedButton(
            onClick = viewModel::requestClearHistory,
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear local cache and history")
        }
        Button(
            onClick = viewModel::logout,
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth().testTag("logout_button")
        ) {
            Text("Logout")
        }
        state.statusMessage?.let { Text(it) }
        Text("Failed messages are retried in the background when a network is available.")
    }

    if (state.showClearConfirmation) {
        ConfirmationDialog(
            title = "Clear local history?",
            message = "This removes locally cached conversations, messages, and attachment metadata.",
            confirmLabel = "Clear",
            onConfirm = viewModel::confirmClearHistory,
            onDismiss = viewModel::cancelClearHistory
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
