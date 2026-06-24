package com.opencode.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opencode.remote.ui.detail.ProcessedMessage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageBubble(
    message: ProcessedMessage,
    showThinking: Boolean,
    onToggleThinking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleShape = if (isUser) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 4.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(bubbleShape)
                .background(containerColor, bubbleShape)
                .padding(12.dp)
        ) {
            // Role label
            if (!isUser && message.text.isNotBlank()) {
                Text(
                    text = "Assistant",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Thinking section (only for assistant messages)
            if (!isUser && message.thinking.isNotBlank()) {
                ThinkingSection(
                    thoughts = message.thinking,
                    expanded = showThinking,
                    onToggle = onToggleThinking,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Main text content
            if (message.text.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }

            // Timestamp
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            Text(
                text = sdf.format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
            )
        }
    }
}
