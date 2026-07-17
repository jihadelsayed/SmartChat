package com.smartchat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.smartchat.core.database.entity.MessageEntity
import com.smartchat.core.database.relation.MessageWithAttachments
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Transaction
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeWithAttachments(conversationId: String): Flow<List<MessageWithAttachments>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun findById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE sender = 'USER' AND syncState IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun findPendingUserMessages(): List<MessageEntity>

    @Update
    suspend fun update(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: String)
}
