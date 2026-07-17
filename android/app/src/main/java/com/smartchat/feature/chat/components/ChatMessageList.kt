package com.smartchat.feature.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartchat.core.database.relation.MessageWithAttachments

@Composable
fun ChatMessageList(
    messages: List<MessageWithAttachments>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (messages.isEmpty()) item { Text("Start a conversation with SmartChat.") }
        items(messages, key = { it.message.id }) { message -> MessageBubble(message) }
    }
}
