package com.smartchat.feature.chat

import com.smartchat.data.SelectedAttachment

data class ChatUiState(
    val conversationId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val selectedAttachment: SelectedAttachment? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)
