package com.smartchat.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val contentUri: String?,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val backendUrl: String? = null,
    val syncState: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)
