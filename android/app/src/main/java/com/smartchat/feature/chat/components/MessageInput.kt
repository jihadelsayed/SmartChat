package com.smartchat.feature.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageInput(
    value: String,
    hasAttachments: Boolean,
    isSending: Boolean,
    onValueChange: (String) -> Unit,
    onPickImages: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Message") },
            modifier = Modifier.weight(1f),
            enabled = !isSending,
            maxLines = 4
        )
        Button(onClick = onPickImages, enabled = !isSending) { Text("Image") }
        Button(
            onClick = onSend,
            enabled = (value.isNotBlank() || hasAttachments) && !isSending
        ) { Text("Send") }
    }
}
