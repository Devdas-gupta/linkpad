package com.btremote.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.btremote.app.bluetooth.ConnectionState
import com.btremote.app.bluetooth.deviceName
import com.btremote.app.bluetooth.isConnected
import com.btremote.app.ui.theme.ErrorRed
import com.btremote.app.ui.theme.SuccessGreen

@Composable
fun ConnectionBar(
    state: ConnectionState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    quickPairLabel: String? = null,
    onQuickPair: (() -> Unit)? = null
) {
    val connected = state.isConnected
    val transition = rememberInfiniteTransition(label = "connectionPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), repeatMode = RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    val (dotColor, label) = when (state) {
        is ConnectionState.Connected -> SuccessGreen.copy(alpha = pulse) to (state.deviceName ?: "Connected")
        is ConnectionState.Connecting -> MaterialTheme.colorScheme.primary.copy(alpha = pulse) to "Connecting…"
        is ConnectionState.Error -> ErrorRed to "Error: ${state.message}"
        is ConnectionState.Disconnected, ConnectionState.Idle ->
            MaterialTheme.colorScheme.onSurfaceVariant to "Not connected"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = if (connected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f, fill = true)
        )
        if (!connected) {
            if (onQuickPair != null && !quickPairLabel.isNullOrBlank()) {
                TextButton(onClick = onQuickPair) {
                    Text(
                        text = "QUICK",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            TextButton(onClick = onClick) {
                Text(
                    text = "CONNECT",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
