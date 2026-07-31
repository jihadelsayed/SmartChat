package com.smartchat.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartchat.BuildConfig
import com.smartchat.core.network.ApiResult
import com.smartchat.data.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    initialConversationId: String?,
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        ChatUiState(conversationId = initialConversationId, isLoading = initialConversationId != null)
    )
    val state: StateFlow<ChatUiState> = _state.asStateFlow()
    private var messagesJob: Job? = null
    private var selectedAttachmentsJob: Job? = null

    init {
        observeSelectedAttachments(initialConversationId ?: NEW_CHAT_KEY)
        initialConversationId?.let { conversationId ->
            observeMessages(conversationId)
            viewModelScope.launch {
                when (val result = chatRepository.synchronizeMessages(conversationId)) {
                    is ApiResult.Success -> _state.value = _state.value.copy(isLoading = false)
                    is ApiResult.Error -> _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun updateInput(value: String) {
        _state.value = _state.value.copy(input = value, errorMessage = null)
    }

    fun selectImages(contentUris: List<String>) {
        if (contentUris.isEmpty()) return
        viewModelScope.launch {
            when (
                val result = chatRepository.selectAttachments(
                    _state.value.conversationId ?: NEW_CHAT_KEY,
                    contentUris
                )
            ) {
                is ApiResult.Success -> Unit
                is ApiResult.Error -> _state.value = _state.value.copy(errorMessage = result.message)
            }
        }
    }

    fun removeSelectedAttachment(attachmentId: String) {
        viewModelScope.launch {
            chatRepository.removeSelectedAttachment(attachmentId)
        }
    }

    fun send() {
        val content = _state.value.input.trim()
        val selectedAttachments = _state.value.selectedAttachments
        if ((content.isEmpty() && selectedAttachments.isEmpty()) || _state.value.isSending) return

        val localMessageId = selectedAttachments.firstOrNull()?.localMessageId
        _state.value = _state.value.copy(isSending = true, errorMessage = null)
        viewModelScope.launch {
            val conversationId = getOrCreateConversation(content) ?: return@launch
            val result = chatRepository.sendMessage(
                conversationId = conversationId,
                content = content,
                localMessageId = localMessageId
            )
            observeSelectedAttachments(conversationId)
            when (result) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    isSending = false,
                    input = ""
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isSending = false,
                    input = "",
                    errorMessage = result.message
                )
            }
        }
    }

    fun retryMessage(messageId: String) {
        viewModelScope.launch {
            when (val result = chatRepository.retryMessage(messageId)) {
                is ApiResult.Success -> Unit
                is ApiResult.Error -> _state.value = _state.value.copy(errorMessage = result.message)
            }
        }
    }

    private suspend fun getOrCreateConversation(firstMessage: String): String? {
        _state.value.conversationId?.let { return it }
        return when (val result = chatRepository.createConversation(firstMessage)) {
            is ApiResult.Success -> {
                observeMessages(result.value)
                _state.value = _state.value.copy(conversationId = result.value)
                result.value
            }
            is ApiResult.Error -> {
                _state.value = _state.value.copy(isSending = false, errorMessage = result.message)
                null
            }
        }
    }

    private fun observeMessages(conversationId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { messages ->
                _state.value = _state.value.copy(
                    messages = messages.map { item ->
                        ChatMessage(
                            id = item.message.id,
                            sender = item.message.sender,
                            content = item.message.content,
                            attachments = item.attachments.map { attachment ->
                                ChatAttachment(
                                    id = attachment.id,
                                    fileName = attachment.fileName,
                                    localFilePath = attachment.localFilePath,
                                    remoteUrl = attachment.backendUrl?.let { url ->
                                        if (url.startsWith("http")) url
                                        else BuildConfig.API_BASE_URL.trimEnd('/') + url
                                    },
                                    uploadState = attachment.syncState,
                                    failureReason = attachment.failureReason
                                )
                            },
                            deliveryState = item.message.syncState,
                            lastError = item.message.lastError
                        )
                    }
                )
            }
        }
    }

    private fun observeSelectedAttachments(conversationKey: String) {
        selectedAttachmentsJob?.cancel()
        selectedAttachmentsJob = viewModelScope.launch {
            chatRepository.observeSelectedAttachments(conversationKey).collect { attachments ->
                _state.value = _state.value.copy(selectedAttachments = attachments)
            }
        }
    }

    class Factory(
        private val conversationId: String?,
        private val chatRepository: ChatRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChatViewModel(conversationId, chatRepository) as T
    }

    private companion object {
        const val NEW_CHAT_KEY = "new-chat"
    }
}
