package com.smartchat.feature.chat

import com.smartchat.data.SelectedAttachment

data class ChatMessage(
    val id: String,
    val sender: String,
    val content: String,
    val attachment: SelectedAttachment? = null
)
