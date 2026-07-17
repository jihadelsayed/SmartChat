package com.smartchat.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartchat.core.database.entity.ConversationEntity
import com.smartchat.core.network.ApiResult
import com.smartchat.data.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val conversations: List<ConversationEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val pendingDelete: ConversationEntity? = null
)

class HistoryViewModel(private val repository: ChatRepository) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeConversations().collect { conversations ->
                _state.value = _state.value.copy(conversations = conversations)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.synchronizeConversations()) {
                is ApiResult.Success -> _state.value = _state.value.copy(isLoading = false)
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun requestDelete(conversation: ConversationEntity) {
        _state.value = _state.value.copy(pendingDelete = conversation)
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(pendingDelete = null)
    }

    fun confirmDelete() {
        val conversation = _state.value.pendingDelete ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(pendingDelete = null, errorMessage = null)
            when (val result = repository.deleteConversation(conversation.id)) {
                is ApiResult.Success -> Unit
                is ApiResult.Error -> _state.value = _state.value.copy(errorMessage = result.message)
            }
        }
    }

    class Factory(private val repository: ChatRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(repository) as T
    }
}
