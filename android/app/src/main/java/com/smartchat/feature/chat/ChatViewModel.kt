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
        val attachment = _state.value.selectedAttachment
        if ((content.isEmpty() && attachment == null) || _state.value.isSending) return

        val messageText = content.ifBlank { if (attachment != null) "Image" else "" }
        val conversationTitle = content.ifBlank { if (attachment != null) "Image attachment" else "New conversation" }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true, errorMessage = null)

            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = "USER",
                content = messageText,
                attachment = attachment
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

            val conversationId = _state.value.conversationId ?: when (val createResult = chatRepository.createConversation(conversationTitle)) {
                is ApiResult.Success -> createResult.value
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isSending = false,
                        errorMessage = createResult.message,
                        messages = _state.value.messages.dropLast(1) + ChatMessage(
                            id = assistantPlaceholder.id,
                            sender = "ASSISTANT",
                            content = "I couldn’t start a conversation right now."
                        )
                    )
                    return@launch
                }
            }

            when (val result = chatRepository.sendMessage(conversationId, messageText, attachment)) {
                is ApiResult.Success -> {
                    val assistantReply = if (attachment != null) {
                        "Image sent successfully."
                    } else {
                        "Message sent."
                    }
                    _state.value = _state.value.copy(
                        isSending = false,
                        conversationId = conversationId,
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
                        conversationId = conversationId,
                        errorMessage = result.message,
                        messages = _state.value.messages.dropLast(1) + ChatMessage(
                            id = assistantPlaceholder.id,
                            sender = "ASSISTANT",
                            content = "I couldn’t send your message right now."
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
