package com.smartchat.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartchat.core.network.ApiResult
import com.smartchat.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.loadCurrentUser()) {
                is ApiResult.Success -> _state.value = ProfileUiState(
                    user = result.value,
                    isLoading = false
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    class Factory(private val repository: ProfileRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileViewModel(repository) as T
    }
}
