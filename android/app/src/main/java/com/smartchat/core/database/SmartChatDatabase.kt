package com.smartchat.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartchat.core.database.dao.ConversationDao
import com.smartchat.core.database.dao.AttachmentDao
import com.smartchat.core.database.dao.MessageDao
import com.smartchat.core.database.dao.PendingAttachmentDao
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.database.entity.AttachmentEntity
import com.smartchat.core.database.entity.MessageEntity
import com.smartchat.core.database.entity.PendingAttachmentEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        AttachmentEntity::class,
        PendingAttachmentEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class SmartChatDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun pendingAttachmentDao(): PendingAttachmentDao
}
