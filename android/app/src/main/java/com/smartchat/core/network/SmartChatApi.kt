package com.smartchat.core.network

import retrofit2.http.Body
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.POST
import okhttp3.MultipartBody

interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiEnvelope<AuthData>>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiEnvelope<AuthData>>
}

interface SmartChatApi : AuthApi {
    @GET("api/health")
    suspend fun health(): Response<ApiEnvelope<HealthData>>

    @GET("api/v1/users/me")
    suspend fun currentUser(): Response<ApiEnvelope<PublicUser>>

    @GET("api/v1/conversations")
    suspend fun conversations(): Response<ApiEnvelope<List<ConversationSummaryDto>>>

    @POST("api/v1/conversations")
    suspend fun createConversation(
        @Body request: CreateConversationRequest
    ): Response<ApiEnvelope<ConversationSummaryDto>>

    @DELETE("api/v1/conversations/{conversationId}")
    suspend fun deleteConversation(
        @Path("conversationId") conversationId: String
    ): Response<Unit>

    @GET("api/v1/conversations/{conversationId}/messages")
    suspend fun messages(
        @Path("conversationId") conversationId: String
    ): Response<ApiEnvelope<List<MessageDto>>>

    @POST("api/v1/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: SendMessageRequest
    ): Response<ApiEnvelope<SendMessageData>>

    @Multipart
    @POST("api/v1/attachments")
    suspend fun uploadAttachment(
        @Header("X-Client-Attachment-Id") clientAttachmentId: String,
        @Part file: MultipartBody.Part
    ): Response<ApiEnvelope<AttachmentDto>>

}
