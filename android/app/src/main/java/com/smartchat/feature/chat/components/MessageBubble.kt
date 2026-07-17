package com.smartchat.feature.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smartchat.BuildConfig
import com.smartchat.core.database.relation.MessageWithAttachments

@Composable
fun MessageBubble(messageWithAttachments: MessageWithAttachments) {
    val message = messageWithAttachments.message
    val isUser = message.sender == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.84f),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (isUser) "You" else "SmartChat", style = MaterialTheme.typography.labelMedium)
                Text(message.content)
                messageWithAttachments.attachments.forEach { attachment ->
                    val model = attachment.contentUri ?: attachment.backendUrl?.let { remoteUrl ->
                        if (remoteUrl.startsWith("http")) remoteUrl
                        else BuildConfig.API_BASE_URL.trimEnd('/') + "/" + remoteUrl.trimStart('/')
                    }
                    if (model != null) {
                        AsyncImage(
                            model = model,
                            contentDescription = attachment.fileName,
                            modifier = Modifier.size(160.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                }
                when (message.syncState) {
                    "PENDING" -> Text("Sending…", style = MaterialTheme.typography.labelSmall)
                    "FAILED" -> Text("Not sent", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
