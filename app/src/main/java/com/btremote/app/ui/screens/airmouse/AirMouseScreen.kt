package com.btremote.app.ui.screens.airmouse

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Navigation

@Composable
fun AirMouseScreen(
    modifier: Modifier = Modifier,
    viewModel: AirMouseViewModel = hiltViewModel()
) {
    val prefs       by viewModel.preferences.collectAsStateWithLifecycle()
    val tilt        by viewModel.tilt.collectAsStateWithLifecycle()
    // BUG 45 — Collect sensorAvailable as StateFlow so UI recomposes on change
    val sensorAvailable by viewModel.sensorAvailable.collectAsStateWithLifecycle()
    val enabled     = prefs?.airMouseEnabled ?: false
    val sensitivity = (prefs?.airMouseSensitivity ?: 8).coerceIn(1, 20)
    val invert      = prefs?.airMouseInvert ?: false
    val gameMode    = prefs?.airMouseGameMode ?: true
    val showLeft    = prefs?.airMouseShowLeft ?: true
    val showRight   = prefs?.airMouseShowRight ?: true
    val showMiddle  = prefs?.airMouseShowMiddle ?: false
    val showReset   = prefs?.airMouseShowReset ?: true

    LaunchedEffect(enabled, sensitivity, invert, gameMode) {
        if (enabled) viewModel.start(sensitivity, invert, gameMode) else viewModel.stop()
    }
    DisposableEffect(Unit) { onDispose { viewModel.stop() } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Status / enable row ───────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = if (enabled && sensorAvailable)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled && sensorAvailable)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Navigation,
                    contentDescription = null,
                    tint = if (enabled && sensorAvailable)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when {
                        !sensorAvailable -> "No gyroscope"
                        enabled && gameMode -> "Air Mouse ON · Game Mode"
                        enabled -> "Air Mouse ON"
                        else -> "Air Mouse OFF"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled && sensorAvailable)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    when {
                        !sensorAvailable -> "Gyroscope unavailable on this device"
                        enabled && gameMode -> "Drift-free · Tilt phone to move cursor"
                        enabled -> "Tilt phone to move cursor"
                        else -> "Enable in Settings → Mouse"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Quick toggle — only if sensor available. Writes the pref via ViewModel.
            if (sensorAvailable) {
                Switch(
                    checked = enabled,
                    onCheckedChange = { viewModel.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor  = MaterialTheme.colorScheme.surface,
                        checkedTrackColor  = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        // ── Crosshair canvas ──────────────────────────────────────────
        val infiniteTransition = rememberInfiniteTransition(label = "cursorPulse")
        val cursorPulse by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue  = if (enabled && sensorAvailable) 1.18f else 1f,
            animationSpec = infiniteRepeatable(tween(700), repeatMode = RepeatMode.Reverse),
            label = "cursorScale"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    if (enabled && sensorAvailable)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val outline   = MaterialTheme.colorScheme.outline
            val primary   = MaterialTheme.colorScheme.primary
            val secondary = MaterialTheme.colorScheme.secondary

            Canvas(modifier = Modifier.size(280.dp)) {
                val cx     = size.width / 2f
                val cy     = size.height / 2f
                val rOuter = size.minDimension * 0.48f
                val rMid   = size.minDimension * 0.32f
                val rInner = size.minDimension * 0.16f

                drawCircle(color = outline, radius = rOuter, center = Offset(cx, cy), style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = outline.copy(alpha = 0.7f), radius = rMid,  center = Offset(cx, cy), style = Stroke(width = 0.7.dp.toPx()))
                drawCircle(color = outline.copy(alpha = 0.5f), radius = rInner, center = Offset(cx, cy), style = Stroke(width = 0.5.dp.toPx()))
                drawLine(outline.copy(alpha = 0.4f), Offset(0f, cy), Offset(size.width, cy), 0.5.dp.toPx())
                drawLine(outline.copy(alpha = 0.4f), Offset(cx, 0f), Offset(cx, size.height), 0.5.dp.toPx())

                val maxOffset   = rOuter * 0.85f
                val (dx, dy)    = tilt
                val visualX     = (dx * 6f).coerceIn(-maxOffset, maxOffset)
                val visualY     = (dy * 6f).coerceIn(-maxOffset, maxOffset)
                val pos         = Offset(cx + visualX, cy + visualY)

                drawCircle(color = primary.copy(alpha = 0.20f), radius = 28f * cursorPulse, center = pos)
                drawCircle(color = primary, radius = 12f, center = pos)
                drawCircle(color = secondary, radius = 4f, center = pos)
            }

            if (!enabled || !sensorAvailable) {
                Text(
                    if (!sensorAvailable) "No gyroscope" else "OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                )
            }
        }

        // ── Click button row ──────────────────────────────────────────
        val visibleButtons = buildList {
            if (showReset)  add("RESET"  to { viewModel.calibrate() })
            if (showLeft)   add("LEFT"   to { viewModel.leftClick() })
            if (showMiddle) add("MIDDLE" to { viewModel.middleClick() })
            if (showRight)  add("RIGHT"  to { viewModel.rightClick() })
        }
        if (visibleButtons.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleButtons.forEach { (label, action) ->
                    val isPrimary = label == "LEFT" || label == "RIGHT"
                    if (isPrimary) {
                        Button(
                            onClick = action,
                            modifier = Modifier.weight(1f).height(58.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) { Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White) }
                    } else {
                        OutlinedButton(
                            onClick = action,
                            modifier = Modifier.weight(1f).height(58.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text(label, style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
        }
    }
}
