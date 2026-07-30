package com.smartchat.feature.chat

import com.smartchat.MainDispatcherRule
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.database.entity.MessageEntity
import com.smartchat.core.database.relation.MessageWithAttachments
import com.smartchat.core.network.ApiResult
import com.smartchat.data.ChatRepository
import com.smartchat.data.PendingQueueResult
import com.smartchat.data.SelectedAttachment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun selectedConversationLoadsMessagesInRepositoryOrder() = runTest {
        val repository = FakeChatRepository()
        repository.messages.value = listOf(
            message("user-1", "USER", "First", 1),
            message("assistant-1", "ASSISTANT", "Second", 2)
        )

        val viewModel = ChatViewModel("conversation-1", repository)
        advanceUntilIdle()

        assertEquals(listOf("First", "Second"), viewModel.state.value.messages.map { it.content })
        assertEquals("conversation-1", repository.synchronizedConversationId)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun firstSendCreatesConversationAndContinuesInIt() = runTest {
        val repository = FakeChatRepository()
        val viewModel = ChatViewModel(null, repository)
        viewModel.updateInput("Hello SmartChat")

        viewModel.send()
        advanceUntilIdle()

        assertEquals("Hello SmartChat", repository.createdFromMessage)
        assertEquals("conversation-1", repository.sentConversationId)
        assertEquals("Hello SmartChat", repository.sentContent)
        assertEquals("conversation-1", viewModel.state.value.conversationId)
        assertEquals(listOf("Hello SmartChat", "Hello from SmartChat"), viewModel.state.value.messages.map { it.content })
        assertEquals("", viewModel.state.value.input)
        assertFalse(viewModel.state.value.isSending)
    }

    @Test
    fun providerFailureKeepsTypedMessageAndShowsUsefulError() = runTest {
        val repository = FakeChatRepository(
            sendResult = ApiResult.Error("The AI service is temporarily unavailable. Please try again.", 502)
        )
        val viewModel = ChatViewModel("conversation-1", repository)
        advanceUntilIdle()
        viewModel.updateInput("Please keep this")

        viewModel.send()
        advanceUntilIdle()

        assertEquals("", viewModel.state.value.input)
        assertEquals(
            "The AI service is temporarily unavailable. Please try again.",
            viewModel.state.value.errorMessage
        )
        assertFalse(viewModel.state.value.isSending)
    }

    @Test
    fun duplicateSendsAreIgnoredWhileRequestIsRunning() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeChatRepository(sendGate = gate)
        val viewModel = ChatViewModel("conversation-1", repository)
        advanceUntilIdle()
        viewModel.updateInput("Only once")

        viewModel.send()
        viewModel.send()
        runCurrent()

        assertTrue(viewModel.state.value.isSending)
        assertEquals(1, repository.sendCount)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.sendCount)
        assertFalse(viewModel.state.value.isSending)
    }

    @Test
    fun blankMessagesAreRejected() = runTest {
        val repository = FakeChatRepository()
        val viewModel = ChatViewModel(null, repository)
        viewModel.updateInput("   ")

        viewModel.send()
        advanceUntilIdle()

        assertEquals(0, repository.sendCount)
        assertNull(repository.createdFromMessage)
    }

    @Test
    fun attachmentOnlySendUsesPersistedDraftMessageId() = runTest {
        val repository = FakeChatRepository()
        repository.selectedAttachments.value = listOf(
            SelectedAttachment(
                id = "attachment",
                localMessageId = "local-message",
                localFilePath = "/owned/image.png",
                fileName = "image.png",
                mimeType = "image/png",
                sizeBytes = 8
            )
        )
        val viewModel = ChatViewModel("conversation-1", repository)
        advanceUntilIdle()

        viewModel.send()
        advanceUntilIdle()

        assertEquals(1, repository.sendCount)
        assertEquals("local-message", repository.sentLocalMessageId)
    }

    private class FakeChatRepository(
        private val sendResult: ApiResult<Unit> = ApiResult.Success(Unit),
        private val sendGate: CompletableDeferred<Unit>? = null
    ) : ChatRepository {
        val messages = MutableStateFlow<List<MessageWithAttachments>>(emptyList())
        val selectedAttachments = MutableStateFlow<List<SelectedAttachment>>(emptyList())
        var createdFromMessage: String? = null
        var sentConversationId: String? = null
        var sentContent: String? = null
        var sentLocalMessageId: String? = null
        var sendCount = 0
        var synchronizedConversationId: String? = null

        override fun observeConversations(): Flow<List<ConversationEntity>> = flowOf(emptyList())

        override fun observeMessages(conversationId: String): Flow<List<MessageWithAttachments>> =
            messages

        override suspend fun synchronizeConversations(): ApiResult<Unit> = ApiResult.Success(Unit)

        override suspend fun synchronizeMessages(conversationId: String): ApiResult<Unit> {
            synchronizedConversationId = conversationId
            return ApiResult.Success(Unit)
        }

        override suspend fun createConversation(firstMessage: String): ApiResult<String> {
            createdFromMessage = firstMessage
            return ApiResult.Success("conversation-1")
        }

        override fun observeSelectedAttachments(
            conversationKey: String
        ): Flow<List<SelectedAttachment>> = selectedAttachments

        override suspend fun selectAttachments(
            conversationKey: String,
            contentUris: List<String>
        ): ApiResult<Unit> = ApiResult.Success(Unit)

        override suspend fun removeSelectedAttachment(attachmentId: String) = Unit

        override suspend fun sendMessage(
            conversationId: String,
            content: String,
            localMessageId: String?
        ): ApiResult<Unit> {
            sendCount += 1
            sentConversationId = conversationId
            sentContent = content
            sentLocalMessageId = localMessageId
            sendGate?.await()
            if (sendResult is ApiResult.Success) {
                messages.value = listOf(
                    message("user-1", "USER", content, 1),
                    message("assistant-1", "ASSISTANT", "Hello from SmartChat", 2)
                )
            }
            return sendResult
        }

        override suspend fun retryMessage(messageId: String): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun retryAllPendingMessages(): PendingQueueResult =
            PendingQueueResult(retryableWorkRemaining = false)
        override suspend fun deleteConversation(conversationId: String): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun clearLocalData() = Unit
    }

    companion object {
        private fun message(
            id: String,
            sender: String,
            content: String,
            createdAt: Long
        ) = MessageWithAttachments(
            message = MessageEntity(
                id = id,
                conversationId = "conversation-1",
                sender = sender,
                content = content,
                createdAt = createdAt
            ),
            attachments = emptyList()
        )
    }
}
