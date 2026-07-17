package com.smartchat.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartchat.core.database.dao.ConversationDao
import com.smartchat.core.database.dao.AttachmentDao
import com.smartchat.core.database.dao.MessageDao
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.database.entity.AttachmentEntity
import com.smartchat.core.database.entity.MessageEntity

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, AttachmentEntity::class],
    version = 2,
    exportSchema = true
)
abstract class SmartChatDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
}
