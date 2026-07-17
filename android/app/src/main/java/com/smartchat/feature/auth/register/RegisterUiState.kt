package com.smartchat.feature.auth.register

data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authenticationSucceeded: Boolean = false
)
