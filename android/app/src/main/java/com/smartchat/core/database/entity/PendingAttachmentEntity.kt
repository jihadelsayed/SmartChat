package com.smartchat.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_attachments",
    indices = [
        Index("localMessageId"),
        Index("conversationKey")
    ]
)
data class PendingAttachmentEntity(
    @PrimaryKey val id: String,
    val localMessageId: String,
    val conversationKey: String,
    val fileName: String,
    val mimeType: String,
    val localFilePath: String,
    val sizeBytes: Long,
    val contentHash: String,
    val createdAt: Long
)
