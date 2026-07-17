package com.smartchat.feature.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartchat.data.ChatRepository
import com.smartchat.feature.chat.components.AttachmentPreview
import com.smartchat.feature.chat.components.ChatMessageList
import com.smartchat.feature.chat.components.MessageInput

@Composable
fun ChatScreen(
    conversationId: String?,
    chatRepository: ChatRepository
) {
    val viewModel: ChatViewModel = viewModel(
        key = conversationId ?: "new-chat",
        factory = ChatViewModel.Factory(conversationId, chatRepository)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.selectImage(uri?.toString())
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).testTag("chat_screen")) {
        Text("AI Chat", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        if (state.isLoading && state.messages.isEmpty()) CircularProgressIndicator()
        ChatMessageList(state.messages, Modifier.weight(1f).fillMaxWidth())
        state.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            if (state.messages.any { it.message.syncState == "FAILED" }) {
                Button(onClick = viewModel::retryFailedMessage, enabled = !state.isSending) {
                    Text("Retry failed message")
                }
            }
        }
        state.selectedAttachment?.let { attachment ->
            AttachmentPreview(attachment, viewModel::removeSelectedImage, Modifier.fillMaxWidth())
        }
        MessageInput(
            value = state.input,
            isSending = state.isSending,
            onValueChange = viewModel::updateInput,
            onPickImage = { imagePicker.launch(arrayOf("image/*")) },
            onSend = viewModel::send
        )
    }
}
