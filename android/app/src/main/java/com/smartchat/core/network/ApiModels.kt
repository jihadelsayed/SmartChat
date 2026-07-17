package com.smartchat.core.network

data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null
)

data class ApiError(
    val code: String,
    val message: String,
    val details: Any? = null
)

data class HealthData(
    val status: String,
    val service: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String
)

data class PublicUser(
    val id: String,
    val email: String,
    val displayName: String,
    val profileImageUrl: String?,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class AuthData(
    val accessToken: String,
    val user: PublicUser
)

data class ChatRequest(val message: String)

data class ChatData(val reply: String)

data class CreateConversationRequest(val title: String? = null)
data class SendMessageRequest(val content: String)

data class ConversationSummaryDto(
    val id: String,
    val userId: String,
    val title: String,
    val createdAt: String,
    val updatedAt: String,
    val messageCount: Int = 0
)

data class ConversationDetailDto(
    val id: String,
    val userId: String,
    val title: String,
    val createdAt: String,
    val updatedAt: String,
    val messages: List<MessageDto>
)

data class MessageDto(
    val id: String,
    val conversationId: String,
    val sender: String,
    val content: String,
    val createdAt: String,
    val attachments: List<AttachmentDto> = emptyList()
)

data class AttachmentDto(
    val id: String,
    val messageId: String,
    val fileName: String,
    val mimeType: String,
    val fileUrl: String,
    val sizeBytes: Long,
    val createdAt: String
)

data class SendMessageData(
    val userMessage: MessageDto,
    val assistantMessage: MessageDto
)

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Error(val message: String, val statusCode: Int? = null) : ApiResult<Nothing>
}
