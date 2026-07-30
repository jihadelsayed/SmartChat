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
    indices = [
        Index("messageId"),
        Index(value = ["backendAttachmentId"], unique = true)
    ]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val contentUri: String?,
    val localFilePath: String? = null,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val contentHash: String? = null,
    val backendUrl: String? = null,
    val backendAttachmentId: String? = null,
    val syncState: String = AttachmentUploadState.PENDING_UPLOAD,
    val failureReason: String? = null,
    val attemptStartedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
