package com.smartchat.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartchat.core.database.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE messageId = :messageId ORDER BY createdAt ASC, id ASC")
    fun observeForMessage(messageId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE messageId = :messageId ORDER BY createdAt ASC, id ASC")
    suspend fun findForMessage(messageId: String): List<AttachmentEntity>

    @Upsert
    suspend fun insert(attachment: AttachmentEntity)

    @Upsert
    suspend fun insertAll(attachments: List<AttachmentEntity>)

    @Query("UPDATE attachments SET messageId = :newMessageId WHERE messageId = :oldMessageId")
    suspend fun moveToMessage(oldMessageId: String, newMessageId: String)

    @Query("SELECT * FROM attachments WHERE backendAttachmentId = :backendId LIMIT 1")
    suspend fun findByBackendId(backendId: String): AttachmentEntity?

    @Query(
        """
        UPDATE attachments
        SET syncState = 'UPLOADING', attemptStartedAt = :now, failureReason = NULL
        WHERE id = :attachmentId
          AND syncState IN ('LOCAL', 'PENDING_UPLOAD', 'FAILED_RETRYABLE')
        """
    )
    suspend fun claimUpload(attachmentId: String, now: Long): Int

    @Query(
        """
        UPDATE attachments
        SET backendAttachmentId = :backendId,
            backendUrl = :backendUrl,
            syncState = 'UPLOADED',
            localFilePath = NULL,
            attemptStartedAt = NULL,
            failureReason = NULL
        WHERE id = :attachmentId AND syncState = 'UPLOADING'
        """
    )
    suspend fun markUploaded(attachmentId: String, backendId: String, backendUrl: String): Int

    @Query(
        """
        UPDATE attachments
        SET syncState = :state, failureReason = :reason, attemptStartedAt = NULL
        WHERE id = :attachmentId AND syncState = 'UPLOADING'
        """
    )
    suspend fun markFailed(attachmentId: String, state: String, reason: String): Int

    @Query(
        """
        UPDATE attachments
        SET syncState = 'FAILED_RETRYABLE',
            failureReason = 'The previous upload was interrupted and will be retried.',
            attemptStartedAt = NULL
        WHERE syncState = 'UPLOADING'
          AND (attemptStartedAt IS NULL OR attemptStartedAt <= :staleBefore)
        """
    )
    suspend fun recoverStaleUploads(staleBefore: Long): Int

    @Query("SELECT localFilePath FROM attachments WHERE localFilePath IS NOT NULL")
    suspend fun findLocalFilePaths(): List<String>

    @Query(
        """
        SELECT localFilePath FROM attachments
        WHERE localFilePath IS NOT NULL
          AND messageId IN (
              SELECT id FROM messages WHERE conversationId = :conversationId
          )
        """
    )
    suspend fun findLocalFilePathsForConversation(conversationId: String): List<String>
}
