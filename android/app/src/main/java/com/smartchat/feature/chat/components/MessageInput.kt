package com.smartchat.feature.chat.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smartchat.data.SelectedAttachment

@Composable
fun MessageInput(
    value: String,
    isSending: Boolean,
    selectedAttachment: SelectedAttachment?,
    onValueChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onSend: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        selectedAttachment?.let { attachment ->
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    AsyncImage(
                        model = attachment.contentUri,
                        contentDescription = attachment.fileName,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Text(attachment.fileName)
                    OutlinedButton(onClick = onRemoveAttachment) { Text("Remove") }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onPickImage, enabled = !isSending) { Text("Image") }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Message") },
                modifier = Modifier.weight(1f),
                enabled = !isSending,
                maxLines = 4
            )
            Button(onClick = onSend, enabled = value.isNotBlank() && !isSending) { Text("Send") }
        }
    }
}
