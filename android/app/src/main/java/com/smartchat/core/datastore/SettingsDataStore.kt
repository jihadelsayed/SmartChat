package com.smartchat.core.datastore

import com.smartchat.core.network.PublicUser
import kotlinx.coroutines.flow.Flow

interface SessionStore {
    val accessToken: Flow<String?>
    val sessionState: Flow<SessionState>

    suspend fun saveSession(user: PublicUser, accessToken: String)
    suspend fun clearSession()
}

interface SettingsStore : SessionStore {
    val preferences: Flow<SettingsPreferences>
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
