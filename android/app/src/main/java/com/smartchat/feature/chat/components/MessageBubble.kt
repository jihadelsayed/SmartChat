package com.smartchat.feature.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.smartchat.core.database.entity.AttachmentUploadState
import com.smartchat.core.database.entity.MessageDeliveryState
import com.smartchat.feature.chat.ChatAttachment
import com.smartchat.feature.chat.ChatMessage
import java.io.File

@Composable
fun MessageBubble(
    message: ChatMessage,
    onRetry: (String) -> Unit
) {
    val isUser = message.sender == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.84f),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    Color(0xFF687EA3)
                } else {
                    Color(0xFFB6C1D2)
                },
                contentColor = if (isUser) Color.White else Color.Black
            )
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (isUser) "You" else "SmartChat", style = MaterialTheme.typography.labelMedium)
                if (message.content.isNotBlank()) {
                    Text(message.content)
                }
                message.attachments.forEach { attachment ->
                    MessageAttachmentImage(attachment)
                }
                if (isUser) {
                    when (message.deliveryState) {
                        MessageDeliveryState.PENDING,
                        MessageDeliveryState.SENDING -> Text(
                            text = "Sending…",
                            style = MaterialTheme.typography.labelSmall
                        )
                        MessageDeliveryState.FAILED_RETRYABLE -> {
                            message.lastError?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            TextButton(
                                onClick = { onRetry(message.id) },
                                modifier = Modifier.testTag("retry_message_${message.id}")
                            ) {
                                Text("Retry")
                            }
                        }
                        MessageDeliveryState.FAILED_PERMANENT -> Text(
                            text = message.lastError ?: "This message could not be sent.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageAttachmentImage(attachment: ChatAttachment) {
    val localFile = attachment.localFilePath?.let(::File)?.takeIf(File::isFile)
    val model: Any? = localFile ?: attachment.remoteUrl
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = attachment.fileName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp, max = 240.dp),
            loading = { CircularProgressIndicator() },
            error = { Text("Image preview unavailable") },
            success = { SubcomposeAsyncImageContent() }
        )
        when (attachment.uploadState) {
            AttachmentUploadState.LOCAL,
            AttachmentUploadState.PENDING_UPLOAD -> Text("Waiting to upload")
            AttachmentUploadState.UPLOADING -> Text("Uploading…")
            AttachmentUploadState.FAILED_RETRYABLE,
            AttachmentUploadState.FAILED_PERMANENT -> Text(
                attachment.failureReason ?: "Image upload failed.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
