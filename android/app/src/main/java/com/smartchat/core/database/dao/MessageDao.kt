package com.smartchat.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.smartchat.core.database.entity.MessageEntity
import com.smartchat.core.database.relation.MessageWithAttachments
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    fun observeForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Transaction
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    fun observeWithAttachments(conversationId: String): Flow<List<MessageWithAttachments>>

    @Upsert
    suspend fun insert(message: MessageEntity)

    @Upsert
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun findById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE backendMessageId = :backendMessageId LIMIT 1")
    suspend fun findByBackendMessageId(backendMessageId: String): MessageEntity?

    @Query(
        """
        SELECT * FROM messages
        WHERE sender = 'USER'
          AND (
              syncState = 'PENDING'
              OR (
                  syncState = 'FAILED_RETRYABLE'
                  AND (nextAttemptAt IS NULL OR nextAttemptAt <= :now)
              )
          )
        ORDER BY createdAt ASC, id ASC
        """
    )
    suspend fun findEligibleUserMessages(now: Long): List<MessageEntity>

    @Query(
        """
        SELECT COUNT(*) FROM messages
        WHERE sender = 'USER'
          AND syncState IN ('PENDING', 'SENDING', 'FAILED_RETRYABLE')
        """
    )
    suspend fun countRetryableUserMessages(): Int

    @Query(
        """
        UPDATE messages
        SET syncState = 'SENDING',
            attemptStartedAt = :now,
            nextAttemptAt = NULL,
            lastError = NULL
        WHERE id = :messageId
          AND sender = 'USER'
          AND (
              syncState = 'PENDING'
              OR (
                  syncState = 'FAILED_RETRYABLE'
                  AND (nextAttemptAt IS NULL OR nextAttemptAt <= :now)
              )
          )
          AND NOT EXISTS (
              SELECT 1 FROM messages AS active
              WHERE active.conversationId = messages.conversationId
                AND active.id != messages.id
                AND active.syncState = 'SENDING'
          )
          AND NOT EXISTS (
              SELECT 1 FROM messages AS older
              WHERE older.conversationId = messages.conversationId
                AND older.sender = 'USER'
                AND older.id != messages.id
                AND (
                    older.createdAt < messages.createdAt
                    OR (older.createdAt = messages.createdAt AND older.id < messages.id)
                )
                AND older.syncState IN ('PENDING', 'SENDING', 'FAILED_RETRYABLE')
          )
        """
    )
    suspend fun claimForSending(messageId: String, now: Long): Int

    @Query(
        """
        UPDATE messages
        SET syncState = 'FAILED_RETRYABLE',
            lastError = :recoveryMessage,
            attemptStartedAt = NULL,
            nextAttemptAt = :now
        WHERE sender = 'USER'
          AND syncState = 'SENDING'
          AND (attemptStartedAt IS NULL OR attemptStartedAt <= :staleBefore)
        """
    )
    suspend fun recoverStaleSending(
        staleBefore: Long,
        now: Long,
        recoveryMessage: String
    ): Int

    @Query(
        """
        UPDATE messages
        SET syncState = 'FAILED_RETRYABLE',
            lastError = :message,
            attemptStartedAt = NULL,
            nextAttemptAt = :nextAttemptAt
        WHERE id = :messageId AND syncState = 'SENDING'
        """
    )
    suspend fun markRetryableFailure(
        messageId: String,
        message: String,
        nextAttemptAt: Long
    ): Int

    @Query(
        """
        UPDATE messages
        SET syncState = 'FAILED_PERMANENT',
            lastError = :message,
            attemptStartedAt = NULL,
            nextAttemptAt = NULL
        WHERE id = :messageId AND syncState = 'SENDING'
        """
    )
    suspend fun markPermanentFailure(messageId: String, message: String): Int

    @Query(
        """
        UPDATE messages
        SET syncState = 'SENT',
            content = :content,
            lastError = NULL,
            attemptStartedAt = NULL,
            nextAttemptAt = NULL,
            backendMessageId = :backendMessageId
        WHERE id = :messageId AND syncState = 'SENDING'
        """
    )
    suspend fun markSent(
        messageId: String,
        backendMessageId: String,
        content: String
    ): Int

    @Update
    suspend fun update(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: String)
}
