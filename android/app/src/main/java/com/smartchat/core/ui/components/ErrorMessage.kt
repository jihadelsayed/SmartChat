package com.smartchat.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
        onRetry?.let { retry -> Button(onClick = retry) { Text("Retry") } }
    }
}
