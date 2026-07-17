package com.smartchat.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.smartchat.core.network.PublicUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "smartchat_settings")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsStore {
    constructor(context: Context) : this(context.settingsDataStore)

    override val preferences: Flow<SettingsPreferences> = dataStore.data.map { values ->
        SettingsPreferences(
            darkTheme = values[PreferenceKeys.darkTheme] ?: false,
            notificationsEnabled = values[PreferenceKeys.notificationsEnabled] ?: true
        )
    }

    override val accessToken: Flow<String?> = dataStore.data.map { values ->
        values[PreferenceKeys.accessToken]
    }

    override val sessionState: Flow<SessionState> = accessToken.map { token ->
        if (token.isNullOrBlank()) SessionState.SignedOut else SessionState.SignedIn
    }

    val displayName: Flow<String> = dataStore.data.map { values ->
        values[PreferenceKeys.displayName] ?: ""
    }

    val email: Flow<String> = dataStore.data.map { values ->
        values[PreferenceKeys.email] ?: ""
    }

    val profileImageUrl: Flow<String?> = dataStore.data.map { values ->
        values[PreferenceKeys.profileImageUrl]
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { values -> values[PreferenceKeys.darkTheme] = enabled }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { values -> values[PreferenceKeys.notificationsEnabled] = enabled }
    }

    override suspend fun saveSession(user: PublicUser, accessToken: String) {
        dataStore.edit { values ->
            values[PreferenceKeys.displayName] = user.displayName
            values[PreferenceKeys.email] = user.email
            user.profileImageUrl?.let { values[PreferenceKeys.profileImageUrl] = it }
                ?: values.remove(PreferenceKeys.profileImageUrl)
            values[PreferenceKeys.accessToken] = accessToken
        }
    }

    override suspend fun clearSession() {
        dataStore.edit { values ->
            values.remove(PreferenceKeys.accessToken)
            values.remove(PreferenceKeys.displayName)
            values.remove(PreferenceKeys.email)
            values.remove(PreferenceKeys.profileImageUrl)
        }
    }
}
