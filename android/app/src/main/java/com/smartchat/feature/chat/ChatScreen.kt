package com.smartchat.feature.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        pickerOpen = false
        viewModel.selectImages(uris.map { it.toString() })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFAAD2FA))
            .padding(16.dp)
            .testTag("chat_screen")
    ) {
        Text("AI Chat", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(12.dp))
        if (state.isLoading && state.messages.isEmpty()) CircularProgressIndicator()
        ChatMessageList(
            messages = state.messages,
            onRetry = viewModel::retryMessage,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        state.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        if (state.isSending) {
            CircularProgressIndicator(modifier = Modifier.testTag("ai_loading_indicator"))
        }
        state.selectedAttachments.forEach { attachment ->
            AttachmentPreview(
                attachment = attachment,
                onRemove = { viewModel.removeSelectedAttachment(attachment.id) }
            )
        }
        MessageInput(
            value = state.input,
            hasAttachments = state.selectedAttachments.isNotEmpty(),
            isSending = state.isSending,
            onValueChange = viewModel::updateInput,
            onPickImages = {
                if (!pickerOpen) {
                    pickerOpen = true
                    imagePicker.launch("image/*")
                }
            },
            onSend = viewModel::send
        )
    }
}
