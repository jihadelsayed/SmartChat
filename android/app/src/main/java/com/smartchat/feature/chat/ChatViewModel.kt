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
            val attachment = _state.value.selectedAttachment
            _state.value = _state.value.copy(isSending = true, errorMessage = null)
            val conversationId = _state.value.conversationId ?: when (
                val createResult = chatRepository.createConversation(content)
            ) {
                is ApiResult.Success -> createResult.value.also { createdId ->
                    _state.value = _state.value.copy(conversationId = createdId)
                    observeMessages(createdId)
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isSending = false,
                        errorMessage = createResult.message
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(input = "", selectedAttachment = null)
            when (val result = chatRepository.sendMessage(conversationId, content, attachment)) {
                is ApiResult.Success -> _state.value = _state.value.copy(isSending = false)
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isSending = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun retryFailedMessage() {
        if (_state.value.isSending) return
        val failed = _state.value.messages.lastOrNull {
            it.message.sender == "USER" && it.message.syncState == "FAILED"
        } ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true, errorMessage = null)
            when (val result = chatRepository.retryMessage(failed.message.id)) {
                is ApiResult.Success -> _state.value = _state.value.copy(isSending = false)
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isSending = false,
                    errorMessage = result.message
                )
            }
        }
    }

    private fun observeMessages(conversationId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { messages ->
                _state.value = _state.value.copy(messages = messages)
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
