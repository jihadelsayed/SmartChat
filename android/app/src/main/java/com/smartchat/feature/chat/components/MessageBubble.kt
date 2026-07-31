package com.smartchat.feature.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smartchat.feature.chat.ChatMessage

@Composable
fun MessageBubble(message: ChatMessage) {
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
                message.attachment?.let { attachment ->
                    AsyncImage(
                        model = attachment.contentUri,
                        contentDescription = attachment.fileName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                Text(message.content.ifBlank { "…" })
            }
        }
    }
}
