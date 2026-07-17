package com.smartchat.feature.chat

import com.smartchat.MainDispatcherRule
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.database.relation.MessageWithAttachments
import com.smartchat.core.network.ApiResult
import com.smartchat.data.ChatRepository
import com.smartchat.data.SelectedAttachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sendCreatesConversationAndSendsMessageOnce() = runTest {
        val repository = FakeChatRepository()
        val viewModel = ChatViewModel(null, repository)
        viewModel.updateInput("Hello SmartChat")

        viewModel.send()
        advanceUntilIdle()

        assertEquals("Hello SmartChat", repository.createdFromMessage)
        assertEquals(listOf("conversation-1" to "Hello SmartChat"), repository.sentMessages)
        assertEquals("conversation-1", viewModel.state.value.conversationId)
        assertFalse(viewModel.state.value.isSending)
        assertEquals("", viewModel.state.value.input)
    }

    private class FakeChatRepository : ChatRepository {
        private val conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
        private val messages = MutableStateFlow<List<MessageWithAttachments>>(emptyList())
        var createdFromMessage: String? = null
        val sentMessages = mutableListOf<Pair<String, String>>()

        override fun observeConversations(): Flow<List<ConversationEntity>> = conversations
        override fun observeMessages(conversationId: String): Flow<List<MessageWithAttachments>> = messages
        override suspend fun synchronizeConversations(): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun synchronizeMessages(conversationId: String): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun createConversation(firstMessage: String): ApiResult<String> {
            createdFromMessage = firstMessage
            return ApiResult.Success("conversation-1")
        }
        override suspend fun inspectAttachment(contentUri: String): ApiResult<SelectedAttachment> =
            ApiResult.Error("Not used")
        override suspend fun sendMessage(
            conversationId: String,
            content: String,
            attachment: SelectedAttachment?
        ): ApiResult<Unit> {
            sentMessages += conversationId to content
            return ApiResult.Success(Unit)
        }
        override suspend fun retryMessage(messageId: String): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun retryAllPendingMessages(): Boolean = true
        override suspend fun deleteConversation(conversationId: String): ApiResult<Unit> =
            ApiResult.Success(Unit)
        override suspend fun clearLocalData() = Unit
    }
}
