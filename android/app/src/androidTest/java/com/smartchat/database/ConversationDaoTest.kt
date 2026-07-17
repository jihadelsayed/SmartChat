package com.smartchat.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartchat.core.database.SmartChatDatabase
import com.smartchat.core.database.entity.AttachmentEntity
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.database.entity.MessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationDaoTest {
    private lateinit var database: SmartChatDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SmartChatDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun deletingConversationCascadesToMessagesAndAttachments() = runBlocking {
        database.conversationDao().insert(CONVERSATION)
        database.messageDao().insert(MESSAGE)
        database.attachmentDao().insert(ATTACHMENT)

        database.conversationDao().deleteById(CONVERSATION.id)

        assertTrue(database.messageDao().observeForConversation(CONVERSATION.id).first().isEmpty())
        assertTrue(database.attachmentDao().findForMessage(MESSAGE.id).isEmpty())
    }

    private companion object {
        val CONVERSATION = ConversationEntity("conversation", "Title", 1L, 1L)
        val MESSAGE = MessageEntity("message", "conversation", "USER", "Hello", 2L)
        val ATTACHMENT = AttachmentEntity(
            id = "attachment",
            messageId = "message",
            contentUri = "content://image",
            fileName = "image.png",
            mimeType = "image/png",
            sizeBytes = 100L
        )
    }
}
