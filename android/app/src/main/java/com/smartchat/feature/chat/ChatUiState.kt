package com.smartchat.feature.chat

import com.smartchat.core.database.relation.MessageWithAttachments
import com.smartchat.data.SelectedAttachment

data class ChatUiState(
    val conversationId: String? = null,
    val messages: List<MessageWithAttachments> = emptyList(),
    val input: String = "",
    val selectedAttachment: SelectedAttachment? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)
