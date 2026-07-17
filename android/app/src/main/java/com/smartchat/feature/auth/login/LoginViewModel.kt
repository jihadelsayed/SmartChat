package com.smartchat.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartchat.core.network.ApiResult
import com.smartchat.core.util.ValidationUtils
import com.smartchat.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun updateEmail(value: String) {
        _state.value = _state.value.copy(email = value, errorMessage = null)
    }

    fun updatePassword(value: String) {
        _state.value = _state.value.copy(password = value, errorMessage = null)
    }

    fun login() {
        val snapshot = _state.value
        if (snapshot.isLoading) return
        if (!ValidationUtils.isValidEmail(snapshot.email)) {
            _state.value = snapshot.copy(errorMessage = "Enter a valid email address.")
            return
        }
        if (snapshot.password.isBlank()) {
            _state.value = snapshot.copy(errorMessage = "Enter your password.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.login(snapshot.email, snapshot.password)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        authenticationSucceeded = true
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoginViewModel(repository) as T
    }
}
