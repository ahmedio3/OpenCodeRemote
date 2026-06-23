package com.opencode.remote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ContextChip(
    label: String,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SuggestionChip(
        onClick = { onDismiss?.invoke() },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        shape = RoundedCornerShape(6.dp),
        border = SuggestionChipDefaults.suggestionChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            enabled = true
        ),
        modifier = modifier.height(28.dp)
    )
}
