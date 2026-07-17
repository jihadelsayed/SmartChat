package com.smartchat.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartchat.core.database.SmartChatDatabase
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.database.entity.MessageEntity
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
        database.conversationDao().insert(ConversationEntity("conversation", "Title", 1L, 1L))
        database.messageDao().insert(
            MessageEntity(
                id = "message",
                conversationId = "conversation",
                sender = "USER",
                content = "Retry me",
                createdAt = 2L,
                syncState = "FAILED",
                lastError = "Offline"
            )
        )

        val pending = database.messageDao().findPendingUserMessages()

        assertEquals(listOf("message"), pending.map { it.id })
    }
}
