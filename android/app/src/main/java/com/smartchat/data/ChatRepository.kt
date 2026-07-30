package com.smartchat.data

import android.content.ContentResolver
import androidx.room.withTransaction
import com.smartchat.core.database.SmartChatDatabase
import com.smartchat.core.database.entity.AttachmentEntity
import com.smartchat.core.database.entity.AttachmentUploadState
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.database.entity.MessageDeliveryState
import com.smartchat.core.database.entity.MessageEntity
import com.smartchat.core.database.relation.MessageWithAttachments
import com.smartchat.core.network.ApiResult
import com.smartchat.core.network.AttachmentDto
import com.smartchat.core.network.CreateConversationRequest
import com.smartchat.core.network.MessageDto
import com.smartchat.core.network.SendMessageData
import com.smartchat.core.network.SendMessageRequest
import com.smartchat.core.network.SmartChatApi
import com.smartchat.core.network.apiRequest
import com.smartchat.core.network.apiUnitRequest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class SelectedAttachment(
    val id: String,
    val localMessageId: String,
    val localFilePath: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
)

data class PendingQueueResult(
    val retryableWorkRemaining: Boolean
)

interface ChatRepository {
    fun observeConversations(): Flow<List<ConversationEntity>>
    fun observeMessages(conversationId: String): Flow<List<MessageWithAttachments>>
    suspend fun synchronizeConversations(): ApiResult<Unit>
    suspend fun synchronizeMessages(conversationId: String): ApiResult<Unit>
    suspend fun createConversation(firstMessage: String): ApiResult<String>
    fun observeSelectedAttachments(conversationKey: String): Flow<List<SelectedAttachment>>
    suspend fun selectAttachments(conversationKey: String, contentUris: List<String>): ApiResult<Unit>
    suspend fun removeSelectedAttachment(attachmentId: String)
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        localMessageId: String? = null
    ): ApiResult<Unit>
    suspend fun retryMessage(messageId: String): ApiResult<Unit>
    suspend fun retryAllPendingMessages(): PendingQueueResult
    suspend fun deleteConversation(conversationId: String): ApiResult<Unit>
    suspend fun clearLocalData()
}

class DefaultChatRepository(
    private val database: SmartChatDatabase,
    private val api: SmartChatApi,
    contentResolver: ContentResolver,
    filesDirectory: File,
    private val onSyncNeeded: () -> Unit,
    private val clock: () -> Long = System::currentTimeMillis
) : ChatRepository {
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val attachmentDao = database.attachmentDao()
    private val pendingAttachmentDao = database.pendingAttachmentDao()
    private val attachmentFileStore = AttachmentFileStore(
        contentResolver,
        filesDirectory,
        clock
    )

    override fun observeConversations(): Flow<List<ConversationEntity>> = conversationDao.observeAll()

    override fun observeMessages(conversationId: String): Flow<List<MessageWithAttachments>> =
        messageDao.observeWithAttachments(conversationId)

    override suspend fun synchronizeConversations(): ApiResult<Unit> {
        return when (val result = apiRequest(api::conversations)) {
            is ApiResult.Success -> {
                val conversations = result.value.mapNotNull { remote ->
                    val createdAt = parseTimestampOrNull(remote.createdAt)
                        ?: return@mapNotNull null
                    val updatedAt = parseTimestampOrNull(remote.updatedAt)
                        ?: return@mapNotNull null
                    ConversationEntity(
                        id = remote.id,
                        title = remote.title,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                }
                conversationDao.synchronizeRemote(conversations)
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> result
        }
    }

    override suspend fun synchronizeMessages(conversationId: String): ApiResult<Unit> {
        return when (val result = apiRequest { api.messages(conversationId) }) {
            is ApiResult.Success -> {
                database.withTransaction {
                    result.value.forEach { message -> saveRemoteMessage(message) }
                }
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> result
        }
    }

    override suspend fun createConversation(firstMessage: String): ApiResult<String> {
        val title = firstMessage.trim().take(120).ifBlank { "New conversation" }
        return when (val result = apiRequest { api.createConversation(CreateConversationRequest(title)) }) {
            is ApiResult.Success -> {
                val remote = result.value
                conversationDao.upsert(
                    ConversationEntity(
                        id = remote.id,
                        title = remote.title,
                        createdAt = parseTimestamp(remote.createdAt),
                        updatedAt = parseTimestamp(remote.updatedAt)
                    )
                )
                ApiResult.Success(remote.id)
            }
            is ApiResult.Error -> result
        }
    }

    override fun observeSelectedAttachments(
        conversationKey: String
    ): Flow<List<SelectedAttachment>> =
        pendingAttachmentDao.observe(conversationKey).map { attachments ->
            attachments.map { attachment ->
                SelectedAttachment(
                    id = attachment.id,
                    localMessageId = attachment.localMessageId,
                    localFilePath = attachment.localFilePath,
                    fileName = attachment.fileName,
                    mimeType = attachment.mimeType,
                    sizeBytes = attachment.sizeBytes
                )
            }
        }

    override suspend fun selectAttachments(
        conversationKey: String,
        contentUris: List<String>
    ): ApiResult<Unit> {
        val existing = pendingAttachmentDao.findForConversation(conversationKey)
        if (existing.size + contentUris.size > AttachmentFileStore.MAX_ATTACHMENTS) {
            return ApiResult.Error("You can attach up to four images.", retryable = false)
        }
        val localMessageId = existing.firstOrNull()?.localMessageId ?: UUID.randomUUID().toString()
        val staged = mutableListOf<com.smartchat.core.database.entity.PendingAttachmentEntity>()
        for (contentUri in contentUris) {
            when (
                val result = attachmentFileStore.copyToOwnedStorage(
                    contentUri,
                    conversationKey,
                    localMessageId
                )
            ) {
                is ApiResult.Error -> {
                    staged.forEach {
                        attachmentFileStore.delete(it.localFilePath)
                    }
                    return result
                }
                is ApiResult.Success -> {
                    if (
                        (existing + staged).any {
                            it.contentHash == result.value.contentHash
                        }
                    ) {
                        attachmentFileStore.delete(result.value.localFilePath)
                    } else {
                        staged += result.value
                    }
                }
            }
        }
        try {
            pendingAttachmentDao.upsertAll(staged)
        } catch (_: Exception) {
            staged.forEach {
                attachmentFileStore.delete(it.localFilePath)
            }
            return ApiResult.Error("SmartChat could not save the selected images.")
        }
        return ApiResult.Success(Unit)
    }

    override suspend fun removeSelectedAttachment(attachmentId: String) {
        pendingAttachmentDao.findById(attachmentId)?.let { attachment ->
            pendingAttachmentDao.deleteById(attachmentId)
            attachmentFileStore.delete(attachment.localFilePath)
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
        localMessageId: String?
    ): ApiResult<Unit> {
        val queuedMessageId = localMessageId ?: UUID.randomUUID().toString()
        val stagedAttachments =
            pendingAttachmentDao.findForLocalMessage(queuedMessageId)
        database.withTransaction {
            messageDao.insert(
                MessageEntity(
                    id = queuedMessageId,
                    conversationId = conversationId,
                    sender = "USER",
                    content = content,
                    createdAt = clock(),
                    syncState = MessageDeliveryState.PENDING
                )
            )
            stagedAttachments.forEach { selected ->
                attachmentDao.insert(
                    AttachmentEntity(
                        id = selected.id,
                        messageId = queuedMessageId,
                        contentUri = null,
                        localFilePath = selected.localFilePath,
                        fileName = selected.fileName,
                        mimeType = selected.mimeType,
                        sizeBytes = selected.sizeBytes,
                        contentHash = selected.contentHash,
                        syncState = AttachmentUploadState.PENDING_UPLOAD
                    )
                )
            }
            pendingAttachmentDao.deleteForLocalMessage(queuedMessageId)
        }
        return sendPendingMessage(queuedMessageId)
    }

    override suspend fun retryMessage(messageId: String): ApiResult<Unit> {
        val message = messageDao.findById(messageId)
            ?: return ApiResult.Error("The pending message no longer exists.")
        if (message.sender != "USER" || message.syncState == MessageDeliveryState.SENT) {
            return ApiResult.Success(Unit)
        }
        if (message.syncState == MessageDeliveryState.FAILED_PERMANENT) {
            return ApiResult.Error(
                message = message.lastError ?: "This message cannot be retried automatically.",
                retryable = false
            )
        }
        return sendPendingMessage(messageId)
    }

    override suspend fun retryAllPendingMessages(): PendingQueueResult {
        val now = clock()
        messageDao.recoverStaleSending(
            staleBefore = now - STALE_SENDING_TIMEOUT_MILLIS,
            now = now,
            recoveryMessage = "The previous send was interrupted and will be retried."
        )
        attachmentDao.recoverStaleUploads(
            staleBefore = now - STALE_SENDING_TIMEOUT_MILLIS
        )
        val pendingMessages = messageDao.findEligibleUserMessages(now)
        for (pending in pendingMessages) {
            if (messageDao.claimForSending(pending.id, clock()) == 1) {
                messageDao.findById(pending.id)?.let { claimed ->
                    sendClaimedMessage(claimed)
                }
            }
        }
        return PendingQueueResult(
            retryableWorkRemaining = messageDao.countRetryableUserMessages() > 0
        )
    }

    override suspend fun deleteConversation(conversationId: String): ApiResult<Unit> {
        return when (val result = apiUnitRequest { api.deleteConversation(conversationId) }) {
            is ApiResult.Success -> {
                deleteLocalConversation(conversationId)
                result
            }
            is ApiResult.Error -> {
                if (result.statusCode == 404) {
                    deleteLocalConversation(conversationId)
                    ApiResult.Success(Unit)
                } else {
                    result
                }
            }
        }
    }

    override suspend fun clearLocalData() {
        val localFiles = attachmentDao.findLocalFilePaths() +
            pendingAttachmentDao.findAll().map { it.localFilePath }
        conversationDao.clearAll()
        pendingAttachmentDao.clearAll()
        localFiles.forEach(attachmentFileStore::delete)
    }

    private suspend fun deleteLocalConversation(conversationId: String) {
        val files = attachmentDao.findLocalFilePathsForConversation(conversationId)
        val staged = pendingAttachmentDao.findForConversation(conversationId)
        conversationDao.deleteById(conversationId)
        staged.forEach { pendingAttachmentDao.deleteById(it.id) }
        (files + staged.map { it.localFilePath })
            .forEach(attachmentFileStore::delete)
    }

    private suspend fun sendPendingMessage(localMessageId: String): ApiResult<Unit> {
        val existing = messageDao.findById(localMessageId)
            ?: return ApiResult.Error("The pending message no longer exists.")
        if (existing.syncState == MessageDeliveryState.SENT) {
            return ApiResult.Success(Unit)
        }
        if (messageDao.claimForSending(localMessageId, clock()) != 1) {
            val current = messageDao.findById(localMessageId)
                ?: return ApiResult.Error("The pending message no longer exists.")
            if (current.syncState != MessageDeliveryState.FAILED_PERMANENT) {
                onSyncNeeded()
                return ApiResult.Success(Unit)
            }
            return ApiResult.Error(
                message = current.lastError ?: "This message cannot be retried automatically.",
                retryable = false
            )
        }
        val localMessage = messageDao.findById(localMessageId)
            ?: return ApiResult.Error("The pending message no longer exists.")
        return sendClaimedMessage(localMessage)
    }

    private suspend fun sendClaimedMessage(localMessage: MessageEntity): ApiResult<Unit> {
        val uploadedAttachmentIds = when (
            val uploadResult = uploadRequiredAttachments(localMessage.id)
        ) {
            is ApiResult.Success -> uploadResult.value
            is ApiResult.Error -> {
                return recordSendFailure(localMessage.id, uploadResult)
            }
        }
        return when (
            val result = apiRequest {
                api.sendMessage(
                    localMessage.conversationId,
                    localMessage.id,
                    SendMessageRequest(
                        content = localMessage.content,
                        attachmentIds = uploadedAttachmentIds
                    )
                )
            }
        ) {
            is ApiResult.Success -> completeExchange(localMessage, result.value)
            is ApiResult.Error -> {
                recordSendFailure(localMessage.id, result)
            }
        }
    }

    private suspend fun recordSendFailure(
        messageId: String,
        error: ApiResult.Error
    ): ApiResult.Error {
        val retryable = when {
            error.code == "AI_QUOTA_EXCEEDED" -> false
            error.code == "AI_REQUEST_IN_PROGRESS" -> true
            error.code in RETRYABLE_AI_CODES -> true
            error.statusCode == 401 -> false
            error.retryable != null -> error.retryable
            error.statusCode != null && error.statusCode in 400..499 -> false
            else -> true
        }
        val displayMessage = when (error.code) {
            "AI_QUOTA_EXCEEDED" -> "AI service quota is unavailable. Please try again later."
            else -> error.message
        }
        if (retryable) {
            val delay = when {
                error.code == "AI_REQUEST_IN_PROGRESS" -> IN_PROGRESS_RETRY_DELAY_MILLIS
                error.retryAfterMillis != null -> error.retryAfterMillis
                else -> DEFAULT_RETRY_DELAY_MILLIS
            }.coerceAtLeast(DEFAULT_RETRY_DELAY_MILLIS)
            messageDao.markRetryableFailure(
                messageId = messageId,
                message = displayMessage,
                nextAttemptAt = clock() + delay
            )
            onSyncNeeded()
        } else {
            messageDao.markPermanentFailure(messageId, displayMessage)
        }
        return error.copy(message = displayMessage, retryable = retryable)
    }

    private suspend fun completeExchange(
        localMessage: MessageEntity,
        exchange: SendMessageData
    ): ApiResult<Unit> {
        val completed = database.withTransaction {
            if (
                messageDao.markSent(
                    messageId = localMessage.id,
                    backendMessageId = exchange.userMessage.id,
                    content = exchange.userMessage.content
                ) != 1
            ) {
                return@withTransaction false
            }
            saveRemoteMessage(exchange.assistantMessage)
            conversationDao.findById(localMessage.conversationId)?.let { conversation ->
                val assistantCreatedAt = parseTimestamp(exchange.assistantMessage.createdAt)
                conversationDao.update(
                    conversation.copy(updatedAt = maxOf(conversation.updatedAt, assistantCreatedAt))
                )
            }
            true
        }
        if (!completed) {
            return ApiResult.Error(
                message = "The message result could not be saved locally.",
                retryable = true
            )
        }

        return ApiResult.Success(Unit)
    }

    private suspend fun uploadRequiredAttachments(
        messageId: String
    ): ApiResult<List<String>> {
        val attachments = attachmentDao.findForMessage(messageId)
        val backendIds = mutableListOf<String>()
        for (attachment in attachments) {
            if (attachment.syncState == AttachmentUploadState.UPLOADED) {
                attachment.backendAttachmentId?.let(backendIds::add)
                    ?: return ApiResult.Error(
                        "An uploaded attachment is missing its server ID.",
                        retryable = true
                    )
                continue
            }
            if (attachment.syncState == AttachmentUploadState.FAILED_PERMANENT) {
                return ApiResult.Error(
                    attachment.failureReason ?: "This image cannot be uploaded.",
                    retryable = false
                )
            }
            if (attachmentDao.claimUpload(attachment.id, clock()) != 1) {
                return ApiResult.Error(
                    "An attachment upload is already in progress.",
                    code = "ATTACHMENT_UPLOAD_IN_PROGRESS",
                    retryable = true
                )
            }
            when (val result = uploadAttachment(attachment)) {
                is ApiResult.Success -> backendIds += result.value
                is ApiResult.Error -> return result
            }
        }
        return ApiResult.Success(backendIds)
    }

    private suspend fun uploadAttachment(
        attachment: AttachmentEntity
    ): ApiResult<String> {
        val file = attachment.localFilePath?.let(::File)
        if (file == null || !file.isFile) {
            val error = ApiResult.Error(
                "The selected image is no longer available.",
                retryable = false
            )
            attachmentDao.markFailed(
                attachment.id,
                AttachmentUploadState.FAILED_PERMANENT,
                error.message
            )
            return error
        }
        val bytes = file.readBytes()
        val requestBody = bytes.toRequestBody(attachment.mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", attachment.fileName, requestBody)
        return when (
            val result = apiRequest {
                api.uploadAttachment(attachment.id, part)
            }
        ) {
            is ApiResult.Success -> {
                attachmentDao.markUploaded(
                    attachment.id,
                    result.value.id,
                    result.value.fileUrl
                )
                attachmentFileStore.delete(attachment.localFilePath)
                ApiResult.Success(result.value.id)
            }
            is ApiResult.Error -> {
                val retryable = when {
                    result.retryable != null -> result.retryable
                    result.statusCode != null && result.statusCode in 400..499 -> false
                    else -> true
                }
                attachmentDao.markFailed(
                    attachment.id,
                    if (retryable) {
                        AttachmentUploadState.FAILED_RETRYABLE
                    } else {
                        AttachmentUploadState.FAILED_PERMANENT
                    },
                    result.message
                )
                result.copy(retryable = retryable)
            }
        }
    }

    private suspend fun saveRemoteMessage(message: MessageDto) {
        val linkedLocal = messageDao.findByBackendMessageId(message.id)
        val existing = linkedLocal ?: messageDao.findById(message.id)
        val localMessageId = linkedLocal?.id ?: message.id
        messageDao.insert(
            MessageEntity(
                id = localMessageId,
                conversationId = message.conversationId,
                sender = message.sender,
                content = message.content,
                createdAt = existing?.createdAt ?: parseTimestamp(message.createdAt),
                syncState = MessageDeliveryState.SENT,
                backendMessageId = linkedLocal?.backendMessageId
            )
        )
        if (message.attachments.isNotEmpty()) {
            attachmentDao.insertAll(
                message.attachments.map { attachment ->
                    val linkedAttachment =
                        attachmentDao.findByBackendId(attachment.id)
                    remoteAttachmentEntity(
                        attachment,
                        localMessageId,
                        linkedAttachment
                    )
                }
            )
        }
    }

    private fun remoteAttachmentEntity(
        attachment: AttachmentDto,
        localMessageId: String,
        existing: AttachmentEntity? = null
    ): AttachmentEntity =
        AttachmentEntity(
            id = existing?.id ?: attachment.id,
            messageId = localMessageId,
            contentUri = null,
            localFilePath = existing?.localFilePath,
            fileName = attachment.fileName,
            mimeType = attachment.mimeType,
            sizeBytes = attachment.sizeBytes,
            backendUrl = attachment.fileUrl,
            backendAttachmentId = attachment.id,
            syncState = AttachmentUploadState.UPLOADED,
            createdAt = existing?.createdAt ?: parseTimestamp(attachment.createdAt)
        )

    private fun parseTimestamp(value: String): Long {
        return parseTimestampOrNull(value) ?: System.currentTimeMillis()
    }

    private fun parseTimestampOrNull(value: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX"
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(value)?.time
            }.getOrNull()
        }
    }

    private companion object {
        const val STALE_SENDING_TIMEOUT_MILLIS = 5 * 60 * 1_000L
        const val DEFAULT_RETRY_DELAY_MILLIS = 10_000L
        const val IN_PROGRESS_RETRY_DELAY_MILLIS = 10_000L

        val RETRYABLE_AI_CODES = setOf(
            "AI_RATE_LIMITED",
            "AI_PROVIDER_TIMEOUT",
            "AI_PROVIDER_UNAVAILABLE"
        )
    }
}
