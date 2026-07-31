package com.smartchat.feature.chat

import com.smartchat.MainDispatcherRule
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.database.relation.MessageWithAttachments
import com.smartchat.core.network.ApiResult
import com.smartchat.data.ChatRepository
import com.smartchat.data.SelectedAttachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sendAddsUserAndAssistantMessagesWhenAiReplySucceeds() = runTest {
        val repository = FakeChatRepository(ApiResult.Success("Hello from SmartChat"))
        val viewModel = ChatViewModel(null, repository)
        viewModel.updateInput("Hello SmartChat")

        viewModel.send()
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.messages.size)
        assertEquals("USER", viewModel.state.value.messages.first().sender)
        assertEquals("Hello SmartChat", viewModel.state.value.messages.first().content)
        assertEquals("ASSISTANT", viewModel.state.value.messages.last().sender)
        assertEquals("Hello from SmartChat", viewModel.state.value.messages.last().content)
        assertFalse(viewModel.state.value.isSending)
        assertEquals("", viewModel.state.value.input)
    }

    @Test
    fun sendUsesConversationMessageFlowWhenAttachmentIsPresent() = runTest {
        val repository = FakeChatRepository(ApiResult.Success("Hello from SmartChat"))
        val viewModel = ChatViewModel(null, repository)
        viewModel.updateInput("Here is an image")
        viewModel.selectImage("content://images/photo")
        advanceUntilIdle()

        viewModel.send()
        advanceUntilIdle()

        assertTrue(repository.sentMessagesByConversation.containsKey("conversation-1"))
        assertEquals("Here is an image", repository.sentMessagesByConversation.getValue("conversation-1").first)
        assertEquals("content://images/photo", repository.sentMessagesByConversation.getValue("conversation-1").second?.contentUri)
        assertEquals("conversation-1", viewModel.state.value.conversationId)
    }

    private class FakeChatRepository(
        private val aiResult: ApiResult<String>
    ) : ChatRepository {
        val sentMessages = mutableListOf<Pair<String, SelectedAttachment?>>()
        val sentMessageMap = mutableMapOf<String, Pair<String, SelectedAttachment?>>()

        override fun observeConversations(): Flow<List<ConversationEntity>> = flowOf(emptyList())
        override fun observeMessages(conversationId: String): Flow<List<MessageWithAttachments>> = flowOf(emptyList())
        override suspend fun synchronizeConversations(): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun synchronizeMessages(conversationId: String): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun createConversation(firstMessage: String): ApiResult<String> = ApiResult.Success("conversation-1")
        override suspend fun inspectAttachment(contentUri: String): ApiResult<SelectedAttachment> = ApiResult.Success(
            SelectedAttachment(contentUri, "photo.jpg", "image/jpeg", 123L)
        )
        override suspend fun sendMessage(conversationId: String, content: String, attachment: SelectedAttachment?): ApiResult<Unit> {
            sentMessages.add(conversationId to attachment)
            sentMessageMap[conversationId] = content to attachment
            return ApiResult.Success(Unit)
        }
        val sentMessagesByConversation: Map<String, Pair<String, SelectedAttachment?>>
            get() = sentMessageMap
        override suspend fun retryMessage(messageId: String): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun retryAllPendingMessages(): Boolean = true
        override suspend fun deleteConversation(conversationId: String): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun clearLocalData() = Unit
        override suspend fun sendAiMessage(message: String): ApiResult<String> = aiResult
    }
}
