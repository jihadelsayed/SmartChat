package com.smartchat.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.smartchat.core.database.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC, id ASC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun findById(conversationId: String): ConversationEntity?

    @Upsert
    suspend fun upsert(conversation: ConversationEntity)

    @Transaction
    suspend fun synchronizeRemote(conversations: List<ConversationEntity>) {
        conversations.forEach { remote ->
            val local = findById(remote.id)
            when {
                local == null -> upsert(remote)
                remote.updatedAt >= local.updatedAt -> {
                    upsert(
                        local.copy(
                            title = remote.title,
                            updatedAt = remote.updatedAt
                        )
                    )
                }
            }
        }
    }

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteById(conversationId: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()
}
