package com.smartchat.feature.chat

data class ChatUiState(
    val conversationId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val selectedAttachments: List<com.smartchat.data.SelectedAttachment> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)
