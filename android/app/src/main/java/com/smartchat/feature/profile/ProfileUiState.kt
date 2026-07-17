package com.smartchat.feature.profile

import com.smartchat.core.network.PublicUser

data class ProfileUiState(
    val user: PublicUser? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
