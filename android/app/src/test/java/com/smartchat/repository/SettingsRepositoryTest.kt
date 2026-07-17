package com.smartchat.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.smartchat.core.datastore.SessionState
import com.smartchat.core.datastore.SettingsRepository
import com.smartchat.core.network.PublicUser
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun preferencesAndSessionPersistInDataStore() = runTest {
        val file = File(temporaryFolder.root, "settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file }
        )
        val repository = SettingsRepository(dataStore)
        val user = PublicUser("id", "student@example.com", "Student", null)

        repository.setDarkTheme(true)
        repository.setNotificationsEnabled(false)
        repository.saveSession(user, "jwt-token")

        val preferences = repository.preferences.first()
        assertTrue(preferences.darkTheme)
        assertFalse(preferences.notificationsEnabled)
        assertEquals("jwt-token", repository.accessToken.first())
        assertEquals(SessionState.SignedIn, repository.sessionState.first())

        repository.clearSession()
        assertEquals(null, repository.accessToken.first())
        assertEquals(SessionState.SignedOut, repository.sessionState.first())
    }
}
