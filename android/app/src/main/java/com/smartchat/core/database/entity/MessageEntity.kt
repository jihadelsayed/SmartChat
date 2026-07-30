package com.smartchat.core.database.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("conversationId"),
        Index(value = ["backendMessageId"], unique = true)
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val sender: String,
    val content: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "'SYNCED'")
    val syncState: String = MessageDeliveryState.SENT,
    val lastError: String? = null,
    val attemptStartedAt: Long? = null,
    val nextAttemptAt: Long? = null,
    val backendMessageId: String? = null
)
