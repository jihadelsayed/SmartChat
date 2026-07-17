package com.smartchat.feature.settings

data class SettingsUiState(
    val darkTheme: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val showClearConfirmation: Boolean = false,
    val isWorking: Boolean = false,
    val statusMessage: String? = null
)
