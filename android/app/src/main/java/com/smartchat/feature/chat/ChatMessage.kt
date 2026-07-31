package com.smartchat.feature.chat

import com.smartchat.core.database.entity.AttachmentEntity

data class ChatMessage(
    val id: String,
    val sender: String,
    val content: String,
    val attachments: List<ChatAttachment> = emptyList(),
    val deliveryState: String,
    val lastError: String? = null
)

data class ChatAttachment(
    val id: String,
    val fileName: String,
    val localFilePath: String?,
    val remoteUrl: String?,
    val uploadState: String,
    val failureReason: String?
)
