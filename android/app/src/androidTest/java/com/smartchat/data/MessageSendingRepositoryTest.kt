package com.smartchat.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartchat.core.database.SmartChatDatabase
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.database.entity.MessageDeliveryState
import com.smartchat.core.database.entity.MessageEntity
import com.smartchat.core.database.entity.AttachmentEntity
import com.smartchat.core.database.entity.AttachmentUploadState
import com.smartchat.core.network.ApiEnvelope
import com.smartchat.core.network.ApiError
import com.smartchat.core.network.AttachmentDto
import com.smartchat.core.network.AuthData
import com.smartchat.core.network.CreateConversationRequest
import com.smartchat.core.network.HealthData
import com.smartchat.core.network.LoginRequest
import com.smartchat.core.network.MessageDto
import com.smartchat.core.network.PublicUser
import com.smartchat.core.network.RegisterRequest
import com.smartchat.core.network.SendMessageData
import com.smartchat.core.network.SendMessageRequest
import com.smartchat.core.network.SmartChatApi
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class MessageSendingRepositoryTest {
    private lateinit var database: SmartChatDatabase
    private lateinit var context: Context
    private var now = 1_000L
    private var syncRequests = 0

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SmartChatDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.conversationDao().upsert(conversation("conversation-1"))
        database.conversationDao().upsert(conversation("conversation-2"))
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun localUuidIsStableAcrossRetryAndSuccessfulReplayDoesNotDuplicateAssistant() = runBlocking {
        val keys = mutableListOf<String>()
        var calls = 0
        val api = FakeApi { conversationId, key, request ->
            keys += key
            calls += 1
            if (calls == 1) {
                providerError(
                    status = 504,
                    code = "AI_PROVIDER_TIMEOUT",
                    retryable = true
                )
            } else {
                successExchange(conversationId, request.content)
            }
        }
        val repository = repository(api)

        repository.sendMessage("conversation-1", "Hello")
        val local = userMessages("conversation-1").single()
        assertNotNull(UUID.fromString(local.id))
        assertEquals(MessageDeliveryState.FAILED_RETRYABLE, local.syncState)

        now += 20_000L
        repository.retryMessage(local.id)
        repository.retryMessage(local.id)

        val messages = database.messageDao().observeForConversation("conversation-1").first()
        assertEquals(listOf(local.id, local.id), keys)
        assertEquals(2, calls)
        assertEquals(MessageDeliveryState.SENT, messages.single { it.sender == "USER" }.syncState)
        assertEquals("backend-user", messages.single { it.sender == "USER" }.backendMessageId)
        assertEquals(1, messages.count { it.sender == "ASSISTANT" })
    }

    @Test
    fun concurrentForegroundAndQueueProcessingOnlyCallApiOnce() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val api = FakeApi { conversationId, _, request ->
            calls += 1
            entered.complete(Unit)
            release.await()
            successExchange(conversationId, request.content)
        }
        val repository = repository(api)
        database.messageDao().insert(pending("message", "conversation-1", 1L))

        val foreground = async { repository.retryMessage("message") }
        entered.await()
        val worker = async { repository.retryAllPendingMessages() }
        worker.await()
        release.complete(Unit)
        foreground.await()

        assertEquals(1, calls)
    }

    @Test
    fun failedMessageDoesNotShortCircuitLaterMessagesOrOtherConversations() = runBlocking {
        val calledConversations = mutableListOf<String>()
        val api = FakeApi { conversationId, _, request ->
            calledConversations += conversationId
            if (request.content == "permanent failure") {
                providerError(
                    status = 503,
                    code = "AI_QUOTA_EXCEEDED",
                    retryable = false
                )
            } else {
                successExchange(
                    conversationId = conversationId,
                    content = request.content,
                    suffix = request.content
                )
            }
        }
        val repository = repository(api)
        database.messageDao().insertAll(
            listOf(
                pending("first", "conversation-1", 1L, "permanent failure"),
                pending("second", "conversation-1", 2L, "later same conversation"),
                pending("third", "conversation-2", 3L, "other conversation")
            )
        )

        val result = repository.retryAllPendingMessages()

        assertEquals(
            listOf("conversation-1", "conversation-1", "conversation-2"),
            calledConversations
        )
        assertFalse(result.retryableWorkRemaining)
        assertEquals(
            MessageDeliveryState.FAILED_PERMANENT,
            database.messageDao().findById("first")?.syncState
        )
        assertEquals(MessageDeliveryState.SENT, database.messageDao().findById("second")?.syncState)
        assertEquals(MessageDeliveryState.SENT, database.messageDao().findById("third")?.syncState)
    }

    @Test
    fun quotaIsPermanentWhileRateLimitRemainsRetryable() = runBlocking {
        val quotaRepository = repository(
            FakeApi { _, _, _ ->
                providerError(503, "AI_QUOTA_EXCEEDED", retryable = false)
            }
        )
        quotaRepository.sendMessage("conversation-1", "quota")
        val quotaMessage = userMessages("conversation-1").single()
        assertEquals(MessageDeliveryState.FAILED_PERMANENT, quotaMessage.syncState)
        assertTrue(quotaMessage.lastError.orEmpty().contains("quota", ignoreCase = true))

        val rateRepository = repository(
            FakeApi { _, _, _ ->
                providerError(429, "AI_RATE_LIMITED", retryable = true)
            }
        )
        rateRepository.sendMessage("conversation-2", "rate")
        val rateMessage = userMessages("conversation-2").single()
        assertEquals(MessageDeliveryState.FAILED_RETRYABLE, rateMessage.syncState)
        assertTrue(rateMessage.nextAttemptAt!! > now)
    }

    @Test
    fun attachmentUploadsBeforeMessageAndIsNotRepeatedAfterMessageRetry() = runBlocking {
        val events = mutableListOf<String>()
        var sendCalls = 0
        val api = FakeApi(
            uploadHandler = { clientAttachmentId, _ ->
                events += "upload:$clientAttachmentId"
                Response.success(
                    ApiEnvelope(
                        success = true,
                        data = AttachmentDto(
                            id = "backend-attachment",
                            messageId = null,
                            fileName = "image.png",
                            mimeType = "image/png",
                            fileUrl = "/uploads/backend-attachment",
                            sizeBytes = 8,
                            createdAt = "2026-07-30T12:00:00.000Z"
                        )
                    )
                )
            }
        ) { conversationId, _, request ->
            events += "message:${request.attachmentIds.joinToString()}"
            sendCalls += 1
            if (sendCalls == 1) {
                providerError(504, "AI_PROVIDER_TIMEOUT", true)
            } else {
                successExchange(conversationId, request.content)
            }
        }
        val repository = repository(api)
        val file = java.io.File(context.filesDir, "attachment-test")
        file.writeBytes(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10))
        database.messageDao().insert(pending("message", "conversation-1", 1L))
        database.attachmentDao().insert(
            AttachmentEntity(
                id = "local-attachment",
                messageId = "message",
                contentUri = null,
                localFilePath = file.absolutePath,
                fileName = "image.png",
                mimeType = "image/png",
                sizeBytes = file.length(),
                syncState = AttachmentUploadState.PENDING_UPLOAD
            )
        )

        repository.retryMessage("message")
        now += 20_000L
        repository.retryMessage("message")

        assertEquals(
            listOf(
                "upload:local-attachment",
                "message:backend-attachment",
                "message:backend-attachment"
            ),
            events
        )
        assertEquals(
            AttachmentUploadState.UPLOADED,
            database.attachmentDao().findForMessage("message").single().syncState
        )
    }

    @Test
    fun failedAttachmentPreventsMessageRequest() = runBlocking {
        var messageCalls = 0
        val api = FakeApi(
            uploadHandler = { _, _ ->
                providerErrorForAttachment(
                    status = 503,
                    code = "ATTACHMENT_STORAGE_UNAVAILABLE",
                    retryable = true
                )
            }
        ) { conversationId, _, request ->
            messageCalls += 1
            successExchange(conversationId, request.content)
        }
        val repository = repository(api)
        val file = java.io.File(context.filesDir, "failed-attachment-test")
        file.writeBytes(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10))
        database.messageDao().insert(pending("failed-message", "conversation-1", 1L))
        database.attachmentDao().insert(
            AttachmentEntity(
                id = "failed-attachment",
                messageId = "failed-message",
                contentUri = null,
                localFilePath = file.absolutePath,
                fileName = "image.png",
                mimeType = "image/png",
                sizeBytes = file.length(),
                syncState = AttachmentUploadState.PENDING_UPLOAD
            )
        )

        repository.retryMessage("failed-message")

        assertEquals(0, messageCalls)
        assertEquals(
            AttachmentUploadState.FAILED_RETRYABLE,
            database.attachmentDao()
                .findForMessage("failed-message")
                .single()
                .syncState
        )
    }

    private fun repository(api: SmartChatApi) = DefaultChatRepository(
        database = database,
        api = api,
        contentResolver = context.contentResolver,
        filesDirectory = context.filesDir,
        onSyncNeeded = { syncRequests += 1 },
        clock = { now }
    )

    private suspend fun userMessages(conversationId: String) =
        database.messageDao().observeForConversation(conversationId).first()
            .filter { it.sender == "USER" }

    private fun conversation(id: String) = ConversationEntity(id, id, 1L, 1L)

    private fun pending(
        id: String,
        conversationId: String,
        createdAt: Long,
        content: String = id
    ) = MessageEntity(
        id = id,
        conversationId = conversationId,
        sender = "USER",
        content = content,
        createdAt = createdAt,
        syncState = MessageDeliveryState.PENDING
    )

    private fun successExchange(
        conversationId: String,
        content: String,
        suffix: String = ""
    ): Response<ApiEnvelope<SendMessageData>> {
        val suffixValue = suffix.ifEmpty { "" }
        return Response.success(
            ApiEnvelope(
                success = true,
                data = SendMessageData(
                    userMessage = messageDto(
                        id = "backend-user$suffixValue",
                        conversationId = conversationId,
                        sender = "USER",
                        content = content,
                        createdAt = "2026-07-30T12:00:00.000Z"
                    ),
                    assistantMessage = messageDto(
                        id = "backend-assistant$suffixValue",
                        conversationId = conversationId,
                        sender = "ASSISTANT",
                        content = "Reply",
                        createdAt = "2026-07-30T12:00:01.000Z"
                    )
                )
            )
        )
    }

    private fun providerError(
        status: Int,
        code: String,
        retryable: Boolean
    ): Response<ApiEnvelope<SendMessageData>> {
        val body = """
            {"error":{"code":"$code","message":"Safe provider message","retryable":$retryable}}
        """.trimIndent().toResponseBody()
        return Response.error(status, body)
    }

    private fun providerErrorForAttachment(
        status: Int,
        code: String,
        retryable: Boolean
    ): Response<ApiEnvelope<AttachmentDto>> {
        val body = """
            {"error":{"code":"$code","message":"Safe upload message","retryable":$retryable}}
        """.trimIndent().toResponseBody()
        return Response.error(status, body)
    }

    private fun messageDto(
        id: String,
        conversationId: String,
        sender: String,
        content: String,
        createdAt: String
    ) = MessageDto(
        id = id,
        conversationId = conversationId,
        sender = sender,
        content = content,
        createdAt = createdAt
    )

    private class FakeApi(
        private val uploadHandler: suspend (
            clientAttachmentId: String,
            file: MultipartBody.Part
        ) -> Response<ApiEnvelope<AttachmentDto>> = { _, _ -> error("Unused") },
        private val sendHandler: suspend (
            conversationId: String,
            idempotencyKey: String,
            request: SendMessageRequest
        ) -> Response<ApiEnvelope<SendMessageData>>
    ) : SmartChatApi {
        override suspend fun sendMessage(
            conversationId: String,
            idempotencyKey: String,
            request: SendMessageRequest
        ) = sendHandler(conversationId, idempotencyKey, request)

        override suspend fun register(request: RegisterRequest): Response<ApiEnvelope<AuthData>> =
            error("Unused")

        override suspend fun login(request: LoginRequest): Response<ApiEnvelope<AuthData>> =
            error("Unused")

        override suspend fun health(): Response<ApiEnvelope<HealthData>> = error("Unused")

        override suspend fun currentUser(): Response<ApiEnvelope<PublicUser>> = error("Unused")

        override suspend fun conversations() =
            error("Unused") as Response<ApiEnvelope<List<com.smartchat.core.network.ConversationSummaryDto>>>

        override suspend fun createConversation(request: CreateConversationRequest) =
            error("Unused") as Response<ApiEnvelope<com.smartchat.core.network.ConversationSummaryDto>>

        override suspend fun deleteConversation(conversationId: String): Response<Unit> =
            error("Unused")

        override suspend fun messages(conversationId: String) =
            error("Unused") as Response<ApiEnvelope<List<MessageDto>>>

        override suspend fun uploadAttachment(
            clientAttachmentId: String,
            file: MultipartBody.Part
        ): Response<ApiEnvelope<AttachmentDto>> =
            uploadHandler(clientAttachmentId, file)
    }
}
