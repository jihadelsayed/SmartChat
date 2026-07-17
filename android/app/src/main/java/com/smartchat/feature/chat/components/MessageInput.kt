package com.smartchat.feature.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageInput(
    value: String,
    isSending: Boolean,
    onValueChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onSend: () -> Unit
) {
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
