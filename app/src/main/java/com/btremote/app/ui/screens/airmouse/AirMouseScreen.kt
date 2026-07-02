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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AirMouseScreen(
    modifier: Modifier = Modifier,
    viewModel: AirMouseViewModel = hiltViewModel()
) {
    val prefs       by viewModel.preferences.collectAsStateWithLifecycle()
    val tilt        by viewModel.tilt.collectAsStateWithLifecycle()
    val sensorAvailable by viewModel.sensorAvailable.collectAsStateWithLifecycle()

    val enabled     = prefs?.airMouseEnabled ?: false
    val sensitivity = (prefs?.airMouseSensitivity ?: 8).coerceIn(1, 20)
    val gameMode    = prefs?.airMouseGameMode ?: true
    val showLeft    = prefs?.airMouseShowLeft ?: true
    val showRight   = prefs?.airMouseShowRight ?: true
    val showMiddle  = prefs?.airMouseShowMiddle ?: false
    val showReset   = prefs?.airMouseShowReset ?: false
    val gameModeAvailable = viewModel.isGameModeAvailable

    // No start/stop here — AirMouseController handles sensor globally
    // so air mouse works on keyboard and all other screens too

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Status card ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (enabled && sensorAvailable)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surface
                )
                .border(
                    width = 1.dp,
                    color = if (enabled && sensorAvailable)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Animated status dot
            val pulse = rememberInfiniteTransition(label = "dot")
            val dotScale by pulse.animateFloat(
                initialValue = 0.85f,
                targetValue = if (enabled && sensorAvailable) 1.15f else 0.85f,
                animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Reverse),
                label = "dotScale"
            )
            Box(
                modifier = Modifier
                    .size((36 * dotScale).dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled && sensorAvailable)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
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
                        !sensorAvailable            -> "No Gyroscope"
                        enabled && gameMode && gameModeAvailable -> "Game Mode Active"
                        enabled                     -> "Air Mouse ON"
                        else                        -> "Air Mouse OFF"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (enabled && sensorAvailable)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    when {
                        !sensorAvailable                         -> "Gyroscope unavailable on this device"
                        enabled && gameMode && gameModeAvailable -> "Drift-free · Works on all screens"
                        enabled                                  -> "Tilt phone to move cursor"
                        else                                     -> "Enable below or in Settings → Mouse"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (sensorAvailable) {
                Switch(
                    checked = enabled,
                    onCheckedChange = { viewModel.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = MaterialTheme.colorScheme.surface,
                        checkedTrackColor   = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        // ── Crosshair canvas ──────────────────────────────────────────
        val cursorPulse by rememberInfiniteTransition(label = "cursor").animateFloat(
            initialValue = 1f,
            targetValue  = if (enabled && sensorAvailable) 1.22f else 1f,
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
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val outline   = MaterialTheme.colorScheme.outline
            val primary   = MaterialTheme.colorScheme.primary
            val secondary = MaterialTheme.colorScheme.secondary
            val onSurface = MaterialTheme.colorScheme.onSurface

            Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                val cx     = size.width / 2f
                val cy     = size.height / 2f
                val rOuter = size.minDimension * 0.46f
                val rMid   = size.minDimension * 0.30f
                val rInner = size.minDimension * 0.15f

                // Grid lines
                drawLine(outline.copy(alpha = 0.2f), Offset(0f, cy), Offset(size.width, cy), 0.8.dp.toPx())
                drawLine(outline.copy(alpha = 0.2f), Offset(cx, 0f), Offset(cx, size.height), 0.8.dp.toPx())

                // Concentric rings
                drawCircle(outline.copy(alpha = 0.5f), rOuter, Offset(cx, cy), style = Stroke(1.dp.toPx()))
                drawCircle(outline.copy(alpha = 0.35f), rMid,   Offset(cx, cy), style = Stroke(0.7.dp.toPx()))
                drawCircle(outline.copy(alpha = 0.25f), rInner, Offset(cx, cy), style = Stroke(0.5.dp.toPx()))

                // Cursor dot
                val maxOffset = rOuter * 0.85f
                val (dx, dy) = tilt
                val visualX  = (dx * 6f).coerceIn(-maxOffset, maxOffset)
                val visualY  = (dy * 6f).coerceIn(-maxOffset, maxOffset)
                val pos      = Offset(cx + visualX, cy + visualY)

                // Glow halo
                drawCircle(primary.copy(alpha = 0.12f * cursorPulse), 32f * cursorPulse, pos)
                drawCircle(primary.copy(alpha = 0.25f), 14f, pos)
                drawCircle(primary, 8f, pos)
                drawCircle(Color.White.copy(alpha = 0.85f), 3f, pos)
            }

            if (!enabled || !sensorAvailable) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        Icons.Outlined.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (!sensorAvailable) "No gyroscope" else "Air Mouse OFF",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }
            }

            // Sensitivity badge
            if (enabled && sensorAvailable) {
                Text(
                    "Sensitivity $sensitivity",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )
            }
        }

        // ── Click buttons row ──────────────────────────────────────────
        val buttons = buildList {
            if (showReset)  add("RESET"  to { viewModel.calibrate() })
            if (showLeft)   add("LEFT"   to { viewModel.leftClick() })
            if (showMiddle) add("MIDDLE" to { viewModel.middleClick() })
            if (showRight)  add("RIGHT"  to { viewModel.rightClick() })
        }

        if (buttons.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                buttons.forEach { (label, action) ->
                    when (label) {
                        "LEFT", "RIGHT" -> Button(
                            onClick = action,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) { Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }

                        "MIDDLE" -> FilledTonalButton(
                            onClick = action,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(label, style = MaterialTheme.typography.labelLarge) }

                        else -> OutlinedButton(
                            onClick = action,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
