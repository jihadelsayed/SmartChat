package com.smartchat.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartchat.core.network.ApiResult
import com.smartchat.data.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    initialConversationId: String?,
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        ChatUiState(conversationId = initialConversationId, isLoading = initialConversationId != null)
    )
    val state: StateFlow<ChatUiState> = _state.asStateFlow()
    private var messagesJob: Job? = null

    init {
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

    fun selectImage(uri: String?) {
        if (uri == null) return
        viewModelScope.launch {
            when (val result = chatRepository.inspectAttachment(uri)) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    selectedAttachment = result.value,
                    errorMessage = null
                )
                is ApiResult.Error -> _state.value = _state.value.copy(errorMessage = result.message)
            }
        }
    }

    fun removeSelectedImage() {
        _state.value = _state.value.copy(selectedAttachment = null)
    }

    fun send() {
        val content = _state.value.input.trim()
        if (content.isEmpty() || _state.value.isSending) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true, errorMessage = null)

            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = "USER",
                content = content
            )
            val assistantPlaceholder = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = "ASSISTANT",
                content = ""
            )
            _state.value = _state.value.copy(
                input = "",
                selectedAttachment = null,
                messages = _state.value.messages + userMessage + assistantPlaceholder
            )

            when (val result = chatRepository.sendAiMessage(content)) {
                is ApiResult.Success -> {
                    val assistantReply = result.value.ifBlank { "I’m here to help. Ask me anything." }
                    _state.value = _state.value.copy(
                        isSending = false,
                        messages = _state.value.messages.dropLast(1) + ChatMessage(
                            id = assistantPlaceholder.id,
                            sender = "ASSISTANT",
                            content = assistantReply
                        )
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isSending = false,
                        errorMessage = result.message,
                        messages = _state.value.messages.dropLast(1) + ChatMessage(
                            id = assistantPlaceholder.id,
                            sender = "ASSISTANT",
                            content = "I couldn’t respond right now."
                        )
                    )
                }
            }
        }
    }

    fun retryFailedMessage() {
        if (_state.value.isSending) return
        _state.value = _state.value.copy(errorMessage = "Retry is not available in the simple chat mode yet.")
    }

    private fun observeMessages(conversationId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { _ ->
                _state.value = _state.value.copy(isLoading = false)
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
}
