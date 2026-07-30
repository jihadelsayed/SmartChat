package com.smartchat.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartchat.core.database.SmartChatDatabase
import com.smartchat.core.database.entity.AttachmentEntity
import com.smartchat.core.database.entity.AttachmentUploadState
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.database.entity.MessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttachmentDaoTest {
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
    fun attachmentMetadataIsStoredWithoutBinaryData() = runBlocking {
        database.conversationDao().upsert(ConversationEntity("conversation", "Title", 1L, 1L))
        database.messageDao().insert(MessageEntity("message", "conversation", "USER", "Image", 2L))
        val attachment = AttachmentEntity(
            id = "attachment",
            messageId = "message",
            contentUri = "content://image",
            fileName = "photo.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 2048L
        )

        database.attachmentDao().insert(attachment)

        assertEquals(listOf(attachment), database.attachmentDao().observeForMessage("message").first())
    }

    @Test
    fun uploadClaimAndStaleRecoveryAreAtomic() = runBlocking {
        insertMessage()
        database.attachmentDao().insert(
            AttachmentEntity(
                id = "attachment",
                messageId = "message",
                contentUri = null,
                localFilePath = "/owned/image",
                fileName = "image.png",
                mimeType = "image/png",
                sizeBytes = 8,
                syncState = AttachmentUploadState.PENDING_UPLOAD
            )
        )

        assertEquals(1, database.attachmentDao().claimUpload("attachment", 100L))
        assertEquals(0, database.attachmentDao().claimUpload("attachment", 100L))
        assertEquals(1, database.attachmentDao().recoverStaleUploads(100L))
        assertEquals(
            AttachmentUploadState.FAILED_RETRYABLE,
            database.attachmentDao().findForMessage("message").single().syncState
        )
    }

    private suspend fun insertMessage() {
        database.conversationDao().upsert(
            ConversationEntity("conversation", "Title", 1L, 1L)
        )
        database.messageDao().insert(
            MessageEntity("message", "conversation", "USER", "Image", 2L)
        )
    }
}
