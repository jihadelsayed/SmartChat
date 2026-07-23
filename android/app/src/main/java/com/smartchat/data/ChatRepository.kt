package com.smartchat.data

import android.content.ContentResolver
import android.content.Intent
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.smartchat.core.database.SmartChatDatabase
import com.smartchat.core.database.entity.AttachmentEntity
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.database.entity.MessageEntity
import com.smartchat.core.database.relation.MessageWithAttachments
import com.smartchat.core.network.ApiResult
import com.smartchat.core.network.AttachmentDto
import com.smartchat.core.network.ChatRequest
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
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class SelectedAttachment(
    val contentUri: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
)

interface ChatRepository {
    fun observeConversations(): Flow<List<ConversationEntity>>
    fun observeMessages(conversationId: String): Flow<List<MessageWithAttachments>>
    suspend fun synchronizeConversations(): ApiResult<Unit>
    suspend fun synchronizeMessages(conversationId: String): ApiResult<Unit>
    suspend fun createConversation(firstMessage: String): ApiResult<String>
    suspend fun inspectAttachment(contentUri: String): ApiResult<SelectedAttachment>
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        attachment: SelectedAttachment? = null
    ): ApiResult<Unit>
    suspend fun sendAiMessage(message: String): ApiResult<String>
    suspend fun retryMessage(messageId: String): ApiResult<Unit>
    suspend fun retryAllPendingMessages(): Boolean
    suspend fun deleteConversation(conversationId: String): ApiResult<Unit>
    suspend fun clearLocalData()
}

class DefaultChatRepository(
    private val database: SmartChatDatabase,
    private val api: SmartChatApi,
    private val contentResolver: ContentResolver,
    private val onSyncNeeded: () -> Unit
) : ChatRepository {
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val attachmentDao = database.attachmentDao()

    override fun observeConversations(): Flow<List<ConversationEntity>> = conversationDao.observeAll()

    override fun observeMessages(conversationId: String): Flow<List<MessageWithAttachments>> =
        messageDao.observeWithAttachments(conversationId)

    override suspend fun synchronizeConversations(): ApiResult<Unit> {
        return when (val result = apiRequest(api::conversations)) {
            is ApiResult.Success -> {
                conversationDao.insertAll(result.value.map { remote ->
                    ConversationEntity(
                        id = remote.id,
                        title = remote.title,
                        createdAt = parseTimestamp(remote.createdAt),
                        updatedAt = parseTimestamp(remote.updatedAt)
                    )
                })
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
                conversationDao.insert(
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

    override suspend fun inspectAttachment(contentUri: String): ApiResult<SelectedAttachment> {
        val uri = contentUri.toUri()
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val mimeType = contentResolver.getType(uri).orEmpty().lowercase()
        val supportedTypes = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/heic",
            "image/heif"
        )
        if (mimeType !in supportedTypes) {
            return ApiResult.Error("Unsupported image type. Choose JPEG, PNG, WebP, GIF, or HEIC.")
        }
        var fileName = uri.lastPathSegment ?: "image"
        var sizeBytes = -1L
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        if (sizeBytes < 0) {
            sizeBytes = runCatching {
                contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
            }.getOrNull() ?: -1L
        }
        if (sizeBytes < 0) {
            return ApiResult.Error("SmartChat could not determine the image size.")
        }
        if (sizeBytes > 10L * 1024L * 1024L) {
            return ApiResult.Error("Images must be 10 MB or smaller.")
        }
        return ApiResult.Success(
            SelectedAttachment(
                contentUri = contentUri,
                fileName = fileName,
                mimeType = mimeType,
                sizeBytes = sizeBytes
            )
        )
    }

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
        attachment: SelectedAttachment?
    ): ApiResult<Unit> {
        val localMessageId = UUID.randomUUID().toString()
        messageDao.insert(
            MessageEntity(
                id = localMessageId,
                conversationId = conversationId,
                sender = "USER",
                content = content,
                createdAt = System.currentTimeMillis(),
                syncState = "PENDING"
            )
        )
        attachment?.let { selected ->
            attachmentDao.insert(
                AttachmentEntity(
                    id = UUID.randomUUID().toString(),
                    messageId = localMessageId,
                    contentUri = selected.contentUri,
                    fileName = selected.fileName,
                    mimeType = selected.mimeType,
                    sizeBytes = selected.sizeBytes
                )
            )
        }
        return sendPendingMessage(localMessageId)
    }

    override suspend fun sendAiMessage(message: String): ApiResult<String> {
        return when (val result = apiRequest { api.chat(ChatRequest(message)) }) {
            is ApiResult.Success -> ApiResult.Success(result.value.reply)
            is ApiResult.Error -> result
        }
    }

    override suspend fun retryMessage(messageId: String): ApiResult<Unit> {
        val message = messageDao.findById(messageId)
            ?: return ApiResult.Error("The pending message no longer exists.")
        if (message.sender != "USER" || message.syncState == "SYNCED") {
            return ApiResult.Success(Unit)
        }
        messageDao.update(message.copy(syncState = "PENDING", lastError = null))
        return sendPendingMessage(messageId)
    }

    override suspend fun retryAllPendingMessages(): Boolean {
        return messageDao.findPendingUserMessages().all { pending ->
            retryMessage(pending.id) is ApiResult.Success
        }
    }

    override suspend fun deleteConversation(conversationId: String): ApiResult<Unit> {
        return when (val result = apiUnitRequest { api.deleteConversation(conversationId) }) {
            is ApiResult.Success -> {
                conversationDao.deleteById(conversationId)
                result
            }
            is ApiResult.Error -> {
                if (result.statusCode == 404) {
                    conversationDao.deleteById(conversationId)
                    ApiResult.Success(Unit)
                } else {
                    result
                }
            }
        }
    }

    override suspend fun clearLocalData() {
        conversationDao.clearAll()
    }

    private suspend fun sendPendingMessage(localMessageId: String): ApiResult<Unit> {
        val localMessage = messageDao.findById(localMessageId)
            ?: return ApiResult.Error("The pending message no longer exists.")
        return when (
            val result = apiRequest {
                api.sendMessage(
                    localMessage.conversationId,
                    SendMessageRequest(localMessage.content)
                )
            }
        ) {
            is ApiResult.Success -> completeExchange(localMessage, result.value)
            is ApiResult.Error -> {
                messageDao.update(
                    localMessage.copy(syncState = "FAILED", lastError = result.message)
                )
                onSyncNeeded()
                result
            }
        }
    }

    private suspend fun completeExchange(
        localMessage: MessageEntity,
        exchange: SendMessageData
    ): ApiResult<Unit> {
        val localAttachments = attachmentDao.findForMessage(localMessage.id)
        database.withTransaction {
            saveRemoteMessage(exchange.userMessage)
            if (localAttachments.isNotEmpty()) {
                attachmentDao.moveToMessage(localMessage.id, exchange.userMessage.id)
            }
            messageDao.deleteById(localMessage.id)
            saveRemoteMessage(exchange.assistantMessage)
            conversationDao.findById(localMessage.conversationId)?.let { conversation ->
                conversationDao.update(conversation.copy(updatedAt = System.currentTimeMillis()))
            }
        }

        val failedUpload = localAttachments.firstOrNull { attachment ->
            uploadAttachment(exchange.userMessage.id, attachment) is ApiResult.Error
        }
        return if (failedUpload == null) {
            ApiResult.Success(Unit)
        } else {
            ApiResult.Error("The message was sent, but its image could not be uploaded.")
        }
    }

    private suspend fun uploadAttachment(
        messageId: String,
        attachment: AttachmentEntity
    ): ApiResult<Unit> {
        val uri = attachment.contentUri?.toUri()
            ?: return ApiResult.Error("The selected image is no longer available.")
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return ApiResult.Error("The selected image is no longer available.")
        val requestBody = bytes.toRequestBody(attachment.mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", attachment.fileName, requestBody)
        return when (val result = apiRequest { api.uploadAttachment(messageId, part) }) {
            is ApiResult.Success -> {
                attachmentDao.markUploaded(attachment.id, result.value.fileUrl)
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> {
                attachmentDao.markFailed(attachment.id)
                result
            }
        }
    }

    private suspend fun saveRemoteMessage(message: MessageDto) {
        messageDao.insert(
            MessageEntity(
                id = message.id,
                conversationId = message.conversationId,
                sender = message.sender,
                content = message.content,
                createdAt = parseTimestamp(message.createdAt),
                syncState = "SYNCED"
            )
        )
        if (message.attachments.isNotEmpty()) {
            attachmentDao.insertAll(message.attachments.map(::remoteAttachmentEntity))
        }
    }

    private fun remoteAttachmentEntity(attachment: AttachmentDto): AttachmentEntity =
        AttachmentEntity(
            id = attachment.id,
            messageId = attachment.messageId,
            contentUri = null,
            fileName = attachment.fileName,
            mimeType = attachment.mimeType,
            sizeBytes = attachment.sizeBytes,
            backendUrl = attachment.fileUrl,
            syncState = "SYNCED",
            createdAt = parseTimestamp(attachment.createdAt)
        )

    private fun parseTimestamp(value: String): Long {
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
        } ?: System.currentTimeMillis()
    }
}
