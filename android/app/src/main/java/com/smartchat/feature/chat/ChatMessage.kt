package com.smartchat.feature.chat

data class ChatMessage(
    val id: String,
    val sender: String,
    val content: String
)
