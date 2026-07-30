package com.smartchat.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartchat.core.database.entity.PendingAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingAttachmentDao {
    @Query(
        "SELECT * FROM pending_attachments WHERE conversationKey = :conversationKey " +
            "ORDER BY createdAt ASC, id ASC"
    )
    fun observe(conversationKey: String): Flow<List<PendingAttachmentEntity>>

    @Query(
        "SELECT * FROM pending_attachments WHERE localMessageId = :localMessageId " +
            "ORDER BY createdAt ASC, id ASC"
    )
    suspend fun findForLocalMessage(localMessageId: String): List<PendingAttachmentEntity>

    @Query(
        "SELECT * FROM pending_attachments WHERE conversationKey = :conversationKey " +
            "ORDER BY createdAt ASC, id ASC"
    )
    suspend fun findForConversation(conversationKey: String): List<PendingAttachmentEntity>

    @Upsert
    suspend fun upsertAll(attachments: List<PendingAttachmentEntity>)

    @Query("SELECT * FROM pending_attachments WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PendingAttachmentEntity?

    @Query("DELETE FROM pending_attachments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pending_attachments WHERE localMessageId = :localMessageId")
    suspend fun deleteForLocalMessage(localMessageId: String)

    @Query("SELECT * FROM pending_attachments")
    suspend fun findAll(): List<PendingAttachmentEntity>

    @Query("DELETE FROM pending_attachments")
    suspend fun clearAll()
}
