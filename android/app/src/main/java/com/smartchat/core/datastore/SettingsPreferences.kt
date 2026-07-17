package com.smartchat.core.datastore

data class SettingsPreferences(
    val darkTheme: Boolean = false,
    val notificationsEnabled: Boolean = true
)

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data object SignedIn : SessionState
}
