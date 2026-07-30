package com.smartchat.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartchat.core.database.SmartChatDatabase
import com.smartchat.core.database.entity.AttachmentEntity
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.database.entity.MessageDeliveryState
import com.smartchat.core.database.entity.MessageEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageDaoTest {
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
    fun failedUserMessageIsReturnedForBackgroundRetry() = runBlocking {
        database.conversationDao().upsert(ConversationEntity("conversation", "Title", 1L, 1L))
        database.messageDao().insert(
            MessageEntity(
                id = "message",
                conversationId = "conversation",
                sender = "USER",
                content = "Retry me",
                createdAt = 2L,
                syncState = MessageDeliveryState.FAILED_RETRYABLE,
                lastError = "Offline"
            )
        )

        val pending = database.messageDao().findEligibleUserMessages(Long.MAX_VALUE)

        assertEquals(listOf("message"), pending.map { it.id })
    }

    @Test
    fun onlyOneConcurrentCallerCanClaimAMessage() = runBlocking {
        database.conversationDao().upsert(ConversationEntity("conversation", "Title", 1L, 1L))
        database.messageDao().insert(
            MessageEntity(
                id = "message",
                conversationId = "conversation",
                sender = "USER",
                content = "Send once",
                createdAt = 2L,
                syncState = MessageDeliveryState.PENDING
            )
        )

        val claims = coroutineScope {
            listOf(
                async { database.messageDao().claimForSending("message", 10L) },
                async { database.messageDao().claimForSending("message", 10L) }
            ).awaitAll()
        }

        assertEquals(1, claims.sum())
        assertEquals(
            MessageDeliveryState.SENDING,
            database.messageDao().findById("message")?.syncState
        )
    }

    @Test
    fun olderMessageMustBeClaimedFirstWithinConversation() = runBlocking {
        database.conversationDao().upsert(ConversationEntity("conversation", "Title", 1L, 1L))
        database.messageDao().insertAll(
            listOf(
                pendingMessage("first", "conversation", 2L),
                pendingMessage("second", "conversation", 3L)
            )
        )

        assertEquals(0, database.messageDao().claimForSending("second", 10L))
        assertEquals(1, database.messageDao().claimForSending("first", 10L))
        assertEquals(0, database.messageDao().claimForSending("second", 10L))
    }

    @Test
    fun staleSendingMessageIsRecoveredForRetry() = runBlocking {
        database.conversationDao().upsert(ConversationEntity("conversation", "Title", 1L, 1L))
        database.messageDao().insert(
            pendingMessage("message", "conversation", 2L).copy(
                syncState = MessageDeliveryState.SENDING,
                attemptStartedAt = 100L
            )
        )

        assertEquals(
            1,
            database.messageDao().recoverStaleSending(
                staleBefore = 500L,
                now = 1_000L,
                recoveryMessage = "Recovered"
            )
        )
        val recovered = database.messageDao().findById("message")
        assertEquals(MessageDeliveryState.FAILED_RETRYABLE, recovered?.syncState)
        assertEquals(1_000L, recovered?.nextAttemptAt)
        assertEquals(null, recovered?.attemptStartedAt)
    }

    @Test
    fun permanentFailureDoesNotCountAsRetryableWork() = runBlocking {
        database.conversationDao().upsert(ConversationEntity("conversation", "Title", 1L, 1L))
        database.messageDao().insert(
            pendingMessage("message", "conversation", 2L).copy(
                syncState = MessageDeliveryState.FAILED_PERMANENT
            )
        )

        assertEquals(0, database.messageDao().countRetryableUserMessages())
    }

    @Test
    fun upsertingMessageDoesNotDeleteItsAttachments() = runBlocking {
        database.conversationDao().upsert(
            ConversationEntity("conversation", "Title", 1L, 1L)
        )
        val message = MessageEntity(
            id = "message",
            conversationId = "conversation",
            sender = "USER",
            content = "Original",
            createdAt = 2L
        )
        val attachment = AttachmentEntity(
            id = "attachment",
            messageId = message.id,
            contentUri = "content://image",
            fileName = "image.png",
            mimeType = "image/png",
            sizeBytes = 100L
        )
        database.messageDao().insert(message)
        database.attachmentDao().insert(attachment)

        database.messageDao().insert(
            message.copy(content = "Updated")
        )

        assertEquals(
            "Updated",
            database.messageDao().findById(message.id)?.content
        )
        assertEquals(
            listOf(attachment.id),
            database.attachmentDao().findForMessage(message.id)
                .map { it.id }
        )
    }

    private fun pendingMessage(
        id: String,
        conversationId: String,
        createdAt: Long
    ) = MessageEntity(
        id = id,
        conversationId = conversationId,
        sender = "USER",
        content = id,
        createdAt = createdAt,
        syncState = MessageDeliveryState.PENDING
    )
}
