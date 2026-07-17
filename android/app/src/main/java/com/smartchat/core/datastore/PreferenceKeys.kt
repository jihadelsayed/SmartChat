package com.smartchat.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object PreferenceKeys {
    val accessToken = stringPreferencesKey("access_token")
    val displayName = stringPreferencesKey("display_name")
    val email = stringPreferencesKey("email")
    val profileImageUrl = stringPreferencesKey("profile_image_url")
    val darkTheme = booleanPreferencesKey("dark_theme")
    val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
}
