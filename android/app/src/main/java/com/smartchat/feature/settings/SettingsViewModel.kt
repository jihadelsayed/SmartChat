package com.smartchat.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartchat.core.datastore.SettingsStore
import com.smartchat.data.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.preferences.collect { preferences ->
                _state.value = _state.value.copy(
                    darkTheme = preferences.darkTheme,
                    notificationsEnabled = preferences.notificationsEnabled
                )
            }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDarkTheme(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setNotificationsEnabled(enabled) }
    }

    fun requestClearHistory() {
        _state.value = _state.value.copy(showClearConfirmation = true)
    }

    fun cancelClearHistory() {
        _state.value = _state.value.copy(showClearConfirmation = false)
    }

    fun confirmClearHistory() {
        viewModelScope.launch {
            _state.value = _state.value.copy(showClearConfirmation = false, isWorking = true)
            chatRepository.clearLocalData()
            _state.value = _state.value.copy(
                isWorking = false,
                statusMessage = "Local chat history cleared."
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isWorking = true)
            chatRepository.clearLocalData()
            settingsStore.clearSession()
        }
    }

    class Factory(
        private val settingsStore: SettingsStore,
        private val chatRepository: ChatRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settingsStore, chatRepository) as T
    }
}
