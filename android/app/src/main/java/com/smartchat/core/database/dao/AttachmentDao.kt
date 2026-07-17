package com.smartchat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartchat.core.database.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE messageId = :messageId ORDER BY createdAt ASC")
    fun observeForMessage(messageId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE messageId = :messageId ORDER BY createdAt ASC")
    suspend fun findForMessage(messageId: String): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<AttachmentEntity>)

    @Query("UPDATE attachments SET messageId = :newMessageId WHERE messageId = :oldMessageId")
    suspend fun moveToMessage(oldMessageId: String, newMessageId: String)

    @Query("UPDATE attachments SET backendUrl = :backendUrl, syncState = 'SYNCED' WHERE id = :attachmentId")
    suspend fun markUploaded(attachmentId: String, backendUrl: String)

    @Query("UPDATE attachments SET syncState = 'FAILED' WHERE id = :attachmentId")
    suspend fun markFailed(attachmentId: String)
}
