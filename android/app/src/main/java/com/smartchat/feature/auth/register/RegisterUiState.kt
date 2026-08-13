package com.smartchat.feature.auth.register

import android.net.Uri

data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val profileImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authenticationSucceeded: Boolean = false
)