package com.btremote.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.btremote.app.bluetooth.ConnectionState
import com.btremote.app.bluetooth.deviceName
import com.btremote.app.bluetooth.isConnected
import com.btremote.app.data.PairedDeviceEntry
import com.btremote.app.ui.theme.ErrorRed
import com.btremote.app.ui.theme.GlassBorderDark
import com.btremote.app.ui.theme.GlassDark
import com.btremote.app.ui.theme.GlowCyan
import com.btremote.app.ui.theme.GlowPrimary
import com.btremote.app.ui.theme.NeonGreen
import com.btremote.app.ui.theme.SuccessGreen

/**
 * Connection bar with a drop-down device switcher.
 *
 * Tapping the bar (when connected) expands a dropdown listing:
 *   • Current connection (checked)
 *   • Paired history (tap to switch)
 *   • "Scan for devices" row
 */
@Composable
fun ConnectionBar(
    state: ConnectionState,
    pairedHistory: List<PairedDeviceEntry> = emptyList(),
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    quickPairLabel: String? = null,
    onQuickPair: (() -> Unit)? = null,
    onSwitchDevice: ((PairedDeviceEntry) -> Unit)? = null,
    onScanRequested: (() -> Unit)? = null,
    onDisconnect: (() -> Unit)? = null,
    onForgetDevice: ((PairedDeviceEntry) -> Unit)? = null
) {
    val connected  = state.isConnected
    var expanded   by remember { mutableStateOf(false) }

    val dotColor = when (state) {
        is ConnectionState.Connected  -> SuccessGreen
        is ConnectionState.Connecting -> GlowPrimary
        is ConnectionState.Error      -> ErrorRed
        else                          -> MaterialTheme.colorScheme.outline
    }

    val label = when (state) {
        is ConnectionState.Connected  -> state.deviceName ?: "Connected"
        is ConnectionState.Connecting -> "Connecting…"
        is ConnectionState.Error      -> "Error: ${state.message}"
        else                          -> "Not connected"
    }

    val barBorderColor by animateColorAsState(
        targetValue = when {
            connected -> NeonGreen.copy(alpha = 0.40f)
            state is ConnectionState.Connecting -> GlowPrimary.copy(alpha = 0.30f)
            else      -> GlassBorderDark
        },
        animationSpec = tween(600),
        label = "barBorder"
    )

    // BUG 28 — Pulsing alpha for the dot when connecting
    val connectingTransition = rememberInfiniteTransition(label = "dotPulse")
    val dotPulseAlpha by connectingTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulseAlpha"
    )
    val dotAlpha = if (state is ConnectionState.Connecting) dotPulseAlpha else 1f

    // Drop-shadow glow when connected
    val glowBrush = if (connected)
        Brush.verticalGradient(listOf(NeonGreen.copy(alpha = 0.04f), Color.Transparent))
    else
        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))

    Column(modifier = modifier.fillMaxWidth()) {
        // Dismiss dropdown when back pressed
        if (expanded) {
            BackHandler { expanded = false }
        }

        // ── Main bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(glowBrush)
                .background(GlassDark)
                .border(
                    width = 1.dp,
                    color = barBorderColor,
                    shape = RoundedCornerShape(0.dp)
                )
                .height(50.dp)
                .pointerInput(connected) {
                    detectTapGestures(onTap = {
                        if (connected || pairedHistory.isNotEmpty()) {
                            expanded = !expanded
                        } else {
                            onClick()
                        }
                    })
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BUG 28 — Status dot pulses when connecting
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = dotAlpha))
            )
            Spacer(Modifier.width(10.dp))

            Text(
                text  = label,
                color = when {
                    connected -> MaterialTheme.colorScheme.onSurface
                    state is ConnectionState.Error -> ErrorRed
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )

            // Expand/collapse chevron (when there are options)
            if (connected || pairedHistory.isNotEmpty()) {
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Expand device list",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
            }

            // Quick + Connect buttons when not connected and not expandable
            if (!connected && pairedHistory.isEmpty()) {
                if (onQuickPair != null && !quickPairLabel.isNullOrBlank()) {
                    TextButton(onClick = onQuickPair) {
                        Text("⚡ QUICK", color = GlowCyan, style = MaterialTheme.typography.labelLarge)
                    }
                }
                TextButton(onClick = onClick) {
                    Text("CONNECT", color = GlowPrimary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // ── Dropdown device switcher ───────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically(tween(220)) + fadeIn(tween(180)),
            exit    = shrinkVertically(tween(180)) + fadeOut(tween(140))
        ) {
            DeviceDropdown(
                currentDeviceName = if (connected) label else null,
                pairedHistory     = pairedHistory,
                quickPairLabel    = quickPairLabel,
                onQuickPair       = onQuickPair,
                onScanRequested   = {
                    expanded = false
                    onScanRequested?.invoke() ?: onClick()
                },
                onSwitchDevice    = { entry ->
                    expanded = false
                    onSwitchDevice?.invoke(entry)
                },
                onDismiss         = { expanded = false },
                onDisconnect      = if (connected && onDisconnect != null) {
                    { expanded = false; onDisconnect.invoke() }
                } else null,
                onForgetDevice    = onForgetDevice
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Dropdown content
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeviceDropdown(
    currentDeviceName: String?,
    pairedHistory: List<PairedDeviceEntry>,
    quickPairLabel: String?,
    onQuickPair: (() -> Unit)?,
    onScanRequested: () -> Unit,
    onSwitchDevice: (PairedDeviceEntry) -> Unit,
    onDismiss: () -> Unit,
    onDisconnect: (() -> Unit)? = null,
    onForgetDevice: ((PairedDeviceEntry) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        GlassDark,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .border(1.dp, GlassBorderDark, RoundedCornerShape(0.dp))
            .padding(vertical = 6.dp)
    ) {
        // Current connection row + Disconnect option
        if (currentDeviceName != null) {
            DropdownRow(
                label    = currentDeviceName,
                sublabel = "Connected",
                isActive = true,
                onClick  = onDismiss
            ) {
                Icon(Icons.Outlined.Check, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
            }
            // Disconnect button
            if (onDisconnect != null) {
                DropdownRow(
                    label    = "Disconnect",
                    sublabel = "End current session",
                    isActive = false,
                    onClick  = { onDisconnect() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ErrorRed.copy(alpha = 0.15f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.BluetoothDisabled, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Paired history — with forget icon
        pairedHistory.forEach { entry ->
            if (entry.name == currentDeviceName) return@forEach  // skip active
            DropdownRow(
                label    = entry.name,
                sublabel = entry.address,
                isActive = false,
                onClick  = { onSwitchDevice(entry) },
                trailingContent = if (onForgetDevice != null) {{
                    androidx.compose.material3.IconButton(
                        onClick = { onForgetDevice(entry) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            "Forget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }} else null
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GlowPrimary.copy(alpha = 0.15f))
                        .border(1.dp, GlowPrimary.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Bluetooth, null, tint = GlowPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Quick pair
        if (onQuickPair != null && !quickPairLabel.isNullOrBlank()) {
            DropdownRow(
                label    = "Quick Reconnect",
                sublabel = quickPairLabel,
                isActive = false,
                onClick  = { onQuickPair(); onDismiss() }
            ) {
                Text("⚡", style = MaterialTheme.typography.titleSmall)
            }
        }

        // Scan row
        DropdownRow(
            label    = "Scan for devices",
            sublabel = "Discover new Bluetooth devices",
            isActive = false,
            onClick  = onScanRequested
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(GlowCyan.copy(alpha = 0.15f))
                    .border(1.dp, GlowCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Search, null, tint = GlowCyan, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun DropdownRow(
    label: String,
    sublabel: String,
    isActive: Boolean,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
    leadingIcon: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (pressed) GlowPrimary.copy(alpha = 0.07f)
                else Color.Transparent
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap   = { onClick() }
                )
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leadingIcon()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isActive) NeonGreen else MaterialTheme.colorScheme.onSurface
            )
            if (sublabel.isNotBlank()) {
                Text(
                    sublabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailingContent?.invoke()
    }
}
