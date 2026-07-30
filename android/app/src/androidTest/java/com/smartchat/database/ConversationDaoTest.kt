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
import org.junit.Assert.assertEquals
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
    fun synchronizingExistingConversationUpdatesInPlaceAndPreservesMessages() = runBlocking {
        database.conversationDao().upsert(CONVERSATION)
        database.messageDao().insert(MESSAGE)
        database.messageDao().insert(SECOND_MESSAGE)

        val remote = CONVERSATION.copy(
            title = "Updated title",
            createdAt = 999L,
            updatedAt = 10L
        )
        database.conversationDao().synchronizeRemote(listOf(remote))
        database.conversationDao().synchronizeRemote(listOf(remote))

        val stored = database.conversationDao().findById(CONVERSATION.id)
        val messages = database.messageDao()
            .observeForConversation(CONVERSATION.id)
            .first()

        assertEquals("Updated title", stored?.title)
        assertEquals(CONVERSATION.createdAt, stored?.createdAt)
        assertEquals(10L, stored?.updatedAt)
        assertEquals(
            listOf(MESSAGE.id, SECOND_MESSAGE.id),
            messages.map { it.id }
        )
        assertEquals(
            1,
            database.conversationDao().observeAll().first()
                .count { it.id == CONVERSATION.id }
        )
    }

    @Test
    fun partialSynchronizationInsertsNewRowsWithoutDeletingLocalRows() = runBlocking {
        val unrelated = ConversationEntity(
            id = "unrelated",
            title = "Unrelated",
            createdAt = 2L,
            updatedAt = 20L
        )
        val localPending = ConversationEntity(
            id = "local-pending",
            title = "Local pending",
            createdAt = 3L,
            updatedAt = 30L
        )
        val remoteNew = ConversationEntity(
            id = "remote-new",
            title = "Remote new",
            createdAt = 4L,
            updatedAt = 40L
        )
        database.conversationDao().upsert(unrelated)
        database.conversationDao().upsert(localPending)

        database.conversationDao()
            .synchronizeRemote(listOf(remoteNew))
        database.conversationDao().synchronizeRemote(emptyList())

        assertEquals(
            setOf(unrelated.id, localPending.id, remoteNew.id),
            database.conversationDao().observeAll().first()
                .map { it.id }
                .toSet()
        )
    }

    @Test
    fun staleRemoteMetadataDoesNotOverwriteNewerLocalValues() = runBlocking {
        val local = CONVERSATION.copy(
            title = "Newer local title",
            updatedAt = 100L
        )
        database.conversationDao().upsert(local)

        database.conversationDao().synchronizeRemote(
            listOf(
                local.copy(
                    title = "Stale remote title",
                    updatedAt = 99L
                )
            )
        )

        assertEquals(local, database.conversationDao().findById(local.id))
    }

    @Test
    fun conversationsRemainOrderedByUpdatedAt() = runBlocking {
        val oldest = ConversationEntity("oldest", "Oldest", 1L, 10L)
        val newest = ConversationEntity("newest", "Newest", 2L, 30L)
        val middle = ConversationEntity("middle", "Middle", 3L, 20L)

        database.conversationDao()
            .synchronizeRemote(listOf(oldest, newest, middle))

        assertEquals(
            listOf(newest.id, middle.id, oldest.id),
            database.conversationDao().observeAll().first()
                .map { it.id }
        )
    }

    @Test
    fun foreignKeysAreEnabled() {
        database.openHelper.writableDatabase
            .query("PRAGMA foreign_keys")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
    }

    @Test
    fun deletingConversationExplicitlyCascadesToMessagesAndAttachments() = runBlocking {
        database.conversationDao().upsert(CONVERSATION)
        database.messageDao().insert(MESSAGE)
        database.messageDao().insert(SECOND_MESSAGE)
        database.attachmentDao().insert(ATTACHMENT)

        database.conversationDao().deleteById(CONVERSATION.id)

        assertTrue(database.messageDao().observeForConversation(CONVERSATION.id).first().isEmpty())
        assertTrue(database.attachmentDao().findForMessage(MESSAGE.id).isEmpty())
    }

    private companion object {
        val CONVERSATION = ConversationEntity("conversation", "Title", 1L, 1L)
        val MESSAGE = MessageEntity("message", "conversation", "USER", "Hello", 2L)
        val SECOND_MESSAGE =
            MessageEntity("message-2", "conversation", "ASSISTANT", "Hi", 3L)
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
