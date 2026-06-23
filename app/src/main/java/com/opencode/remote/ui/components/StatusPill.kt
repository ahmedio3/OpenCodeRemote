package com.opencode.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.opencode.remote.ui.theme.StatusBusy
import com.opencode.remote.ui.theme.StatusIdle
import com.opencode.remote.ui.theme.StatusRetry
import com.opencode.remote.ui.theme.StatusError

@Composable
fun StatusPill(
    status: String,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (status.lowercase()) {
        "busy" -> "Busy" to StatusBusy
        "idle" -> "Idle" to StatusIdle
        "retry" -> "Retry" to StatusRetry
        "error" -> "Error" to StatusError
        "active" -> "Active" to StatusBusy
        else -> status to StatusIdle
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
