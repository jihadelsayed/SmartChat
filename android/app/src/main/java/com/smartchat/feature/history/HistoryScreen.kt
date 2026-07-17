package com.smartchat.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartchat.core.ui.components.ConfirmationDialog
import com.smartchat.core.ui.components.EmptyState
import com.smartchat.core.ui.components.ErrorMessage
import com.smartchat.core.ui.components.LoadingIndicator
import com.smartchat.core.util.DateFormatter
import com.smartchat.data.ChatRepository

@Composable
fun HistoryScreen(chatRepository: ChatRepository, onOpenConversation: (String) -> Unit) {
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(chatRepository))
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(16.dp).testTag("history_screen")) {
        Text("Chat History", style = MaterialTheme.typography.headlineMedium)
        state.errorMessage?.let {
            ErrorMessage(it, Modifier.fillMaxWidth(), viewModel::refresh)
        }
        when {
            state.isLoading && state.conversations.isEmpty() -> LoadingIndicator(Modifier.weight(1f))
            state.conversations.isEmpty() -> EmptyState("No saved conversations yet.", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.conversations, key = { it.id }) { conversation ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onOpenConversation(conversation.id)
                        }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(conversation.title, style = MaterialTheme.typography.titleMedium)
                                Text(DateFormatter.formatDateTime(conversation.updatedAt))
                            }
                            TextButton(onClick = { viewModel.requestDelete(conversation) }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    state.pendingDelete?.let { conversation ->
        ConfirmationDialog(
            title = "Delete conversation?",
            message = "Delete “${conversation.title}” locally and from the backend?",
            confirmLabel = "Delete",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete
        )
    }
}
