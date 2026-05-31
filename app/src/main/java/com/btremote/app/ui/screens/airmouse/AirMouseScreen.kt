package com.btremote.app.ui.screens.airmouse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AirMouseScreen(
    modifier: Modifier = Modifier,
    viewModel: AirMouseViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val tilt by viewModel.tilt.collectAsStateWithLifecycle()
    val enabled = prefs?.airMouseEnabled ?: false
    val sensitivity = (prefs?.airMouseSensitivity ?: 8).coerceIn(1, 20)
    val invert = prefs?.airMouseInvert ?: false
    val showLeft = prefs?.airMouseShowLeft ?: true
    val showRight = prefs?.airMouseShowRight ?: true
    val showMiddle = prefs?.airMouseShowMiddle ?: false
    val showReset = prefs?.airMouseShowReset ?: true

    LaunchedEffect(enabled, sensitivity, invert) {
        if (enabled) viewModel.start(sensitivity, invert) else viewModel.stop()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.stop() }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status banner — disabled state hint when off
        if (!enabled || !viewModel.sensorAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    when {
                        !viewModel.sensorAvailable -> "Gyroscope unavailable on this device"
                        !enabled -> "Air mouse is off — enable in Settings → Mouse"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Crosshair canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            val outline = MaterialTheme.colorScheme.outline
            val primary = MaterialTheme.colorScheme.primary
            val secondary = MaterialTheme.colorScheme.secondary
            Canvas(modifier = Modifier.size(280.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val rOuter = size.minDimension * 0.48f
                val rMid = size.minDimension * 0.32f
                val rInner = size.minDimension * 0.16f
                drawCircle(color = outline, radius = rOuter, center = Offset(cx, cy), style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = outline.copy(alpha = 0.7f), radius = rMid, center = Offset(cx, cy), style = Stroke(width = 0.7.dp.toPx()))
                drawCircle(color = outline.copy(alpha = 0.5f), radius = rInner, center = Offset(cx, cy), style = Stroke(width = 0.5.dp.toPx()))
                drawLine(outline.copy(alpha = 0.4f), Offset(0f, cy), Offset(size.width, cy), 0.5.dp.toPx())
                drawLine(outline.copy(alpha = 0.4f), Offset(cx, 0f), Offset(cx, size.height), 0.5.dp.toPx())

                val maxOffset = rOuter * 0.85f
                val (dx, dy) = tilt
                val visualX = (dx * 6f).coerceIn(-maxOffset, maxOffset)
                val visualY = (dy * 6f).coerceIn(-maxOffset, maxOffset)
                val pos = Offset(cx + visualX, cy + visualY)
                drawCircle(color = primary.copy(alpha = 0.25f), radius = 24f, center = pos)
                drawCircle(color = primary, radius = 12f, center = pos)
                drawCircle(color = secondary, radius = 4f, center = pos)
            }
        }

        // Click button row — only visible buttons render
        val visibleButtons = buildList {
            if (showReset) add("RESET" to { viewModel.calibrate() })
            if (showLeft) add("LEFT" to { viewModel.leftClick() })
            if (showMiddle) add("MIDDLE" to { viewModel.middleClick() })
            if (showRight) add("RIGHT" to { viewModel.rightClick() })
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
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White) }
                    } else {
                        OutlinedButton(
                            onClick = action,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(label, style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
        }
    }
}
