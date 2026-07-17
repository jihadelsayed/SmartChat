package com.smartchat.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.smartchat.core.database.entity.AttachmentEntity
import com.smartchat.core.database.entity.MessageEntity

data class MessageWithAttachments(
    @Embedded val message: MessageEntity,
    @Relation(parentColumn = "id", entityColumn = "messageId")
    val attachments: List<AttachmentEntity>
)
