package com.smartchat.feature.history

import com.smartchat.MainDispatcherRule
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.database.relation.MessageWithAttachments
import com.smartchat.core.network.ApiResult
import com.smartchat.data.ChatRepository
import com.smartchat.data.PendingQueueResult
import com.smartchat.data.SelectedAttachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refreshLoadsAuthenticatedUsersConversations() = runTest {
        val repository = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repository)
        repository.conversations.value = listOf(
            ConversationEntity("conversation-1", "Course help", 1, 2)
        )

        advanceUntilIdle()

        assertEquals(1, repository.synchronizeCount)
        assertEquals(listOf("Course help"), viewModel.state.value.conversations.map { it.title })
        assertFalse(viewModel.state.value.isLoading)
    }

    private class FakeHistoryRepository : ChatRepository {
        val conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
        var synchronizeCount = 0

        override fun observeConversations(): Flow<List<ConversationEntity>> = conversations
        override fun observeMessages(conversationId: String): Flow<List<MessageWithAttachments>> =
            flowOf(emptyList())

        override suspend fun synchronizeConversations(): ApiResult<Unit> {
            synchronizeCount += 1
            return ApiResult.Success(Unit)
        }

        override suspend fun synchronizeMessages(conversationId: String): ApiResult<Unit> =
            ApiResult.Success(Unit)

        override suspend fun createConversation(firstMessage: String): ApiResult<String> =
            ApiResult.Success("conversation-1")

        override fun observeSelectedAttachments(
            conversationKey: String
        ): Flow<List<SelectedAttachment>> = flowOf(emptyList())

        override suspend fun selectAttachments(
            conversationKey: String,
            contentUris: List<String>
        ): ApiResult<Unit> = ApiResult.Success(Unit)

        override suspend fun removeSelectedAttachment(attachmentId: String) = Unit

        override suspend fun sendMessage(
            conversationId: String,
            content: String,
            localMessageId: String?
        ): ApiResult<Unit> = ApiResult.Success(Unit)

        override suspend fun retryMessage(messageId: String): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun retryAllPendingMessages(): PendingQueueResult =
            PendingQueueResult(retryableWorkRemaining = false)
        override suspend fun deleteConversation(conversationId: String): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun clearLocalData() = Unit
    }
}
