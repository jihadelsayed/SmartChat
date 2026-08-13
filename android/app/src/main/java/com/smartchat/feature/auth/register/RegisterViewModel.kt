package com.smartchat.feature.auth.register

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
import android.net.Uri
import com.smartchat.repository.ProfileRepository

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun updateDisplayName(value: String) {
        _state.value = _state.value.copy(displayName = value, errorMessage = null)
    }

    fun updateEmail(value: String) {
        _state.value = _state.value.copy(email = value, errorMessage = null)
    }

    fun updatePassword(value: String) {
        _state.value = _state.value.copy(password = value, errorMessage = null)
    }

    fun updateProfileImage(uri: Uri?) {
        _state.value = _state.value.copy(
            profileImageUri = uri,
            errorMessage = null
        )
    }

    fun register() {
        val snapshot = _state.value
        if (snapshot.isLoading) return
        val validationError = when {
            snapshot.displayName.trim().length < 2 -> "Display name must have at least 2 characters."
            !ValidationUtils.isValidEmail(snapshot.email) -> "Enter a valid email address."
            else -> ValidationUtils.passwordError(snapshot.password)
        }
        if (validationError != null) {
            _state.value = snapshot.copy(errorMessage = validationError)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (
                val result = authRepository.register(
                    snapshot.displayName,
                    snapshot.email,
                    snapshot.password
                )
            ) {
                is ApiResult.Success -> {
                    val imageUri = snapshot.profileImageUri

                    if (imageUri != null) {
                        when (val uploadResult = profileRepository.uploadProfileImage(imageUri)) {
                            is ApiResult.Success -> {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    authenticationSucceeded = true
                                )
                            }

                            is ApiResult.Error -> {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    errorMessage = "Account created, but profile picture upload failed: ${uploadResult.message}"
                                )
                            }
                        }
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            authenticationSucceeded = true
                        )
                    }
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

    class Factory(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RegisterViewModel(
                authRepository,
                profileRepository
            ) as T
    }
}
