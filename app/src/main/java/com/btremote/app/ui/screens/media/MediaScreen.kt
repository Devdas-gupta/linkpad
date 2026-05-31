package com.btremote.app.ui.screens.media

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.VolumeDown
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btremote.app.bluetooth.ConsumerUsage

@Composable
fun MediaScreen(
    modifier: Modifier = Modifier,
    viewModel: MediaViewModel = hiltViewModel()
) {
    var isPlaying by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "MEDIA CONTROL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        // Top row: brightness + volume quick adjust
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickIcon(Icons.Outlined.BrightnessLow, "Brightness down", Modifier.weight(1f)) {
                viewModel.send(ConsumerUsage.BRIGHTNESS_DOWN)
            }
            QuickIcon(Icons.Outlined.VolumeDown, "Volume down", Modifier.weight(1f)) {
                viewModel.send(ConsumerUsage.VOLUME_DOWN)
            }
            QuickIcon(Icons.Outlined.VolumeUp, "Volume up", Modifier.weight(1f)) {
                viewModel.send(ConsumerUsage.VOLUME_UP)
            }
            QuickIcon(Icons.Outlined.BrightnessHigh, "Brightness up", Modifier.weight(1f)) {
                viewModel.send(ConsumerUsage.BRIGHTNESS_UP)
            }
        }

        Spacer(Modifier.weight(0.5f))

        // Vinyl-style transport
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleButton(
                icon = Icons.Outlined.SkipPrevious,
                description = "Previous",
                size = 56.dp,
                background = MaterialTheme.colorScheme.surfaceVariant,
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = { viewModel.send(ConsumerUsage.PREV_TRACK) }
            )
            CircleButton(
                icon = Icons.Outlined.FastRewind,
                description = "Rewind",
                size = 48.dp,
                background = MaterialTheme.colorScheme.surfaceVariant,
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = { viewModel.seekBackward() }
            )
            PlayPauseButton(
                isPlaying = isPlaying,
                onClick = {
                    isPlaying = !isPlaying
                    viewModel.send(ConsumerUsage.PLAY_PAUSE)
                }
            )
            CircleButton(
                icon = Icons.Outlined.FastForward,
                description = "Forward",
                size = 48.dp,
                background = MaterialTheme.colorScheme.surfaceVariant,
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = { viewModel.seekForward() }
            )
            CircleButton(
                icon = Icons.Outlined.SkipNext,
                description = "Next",
                size = 56.dp,
                background = MaterialTheme.colorScheme.surfaceVariant,
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = { viewModel.send(ConsumerUsage.NEXT_TRACK) }
            )
        }

        Spacer(Modifier.weight(0.5f))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            MutePill { viewModel.send(ConsumerUsage.MUTE) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "playPulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.06f else 1f,
        animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Reverse),
        label = "pulseScale"
    )
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(96.dp)
            .scale(if (isPlaying) pulse else 1f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(Unit) {
                detectTap {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = "Play/Pause",
            tint = Color.White,
            modifier = Modifier.size(44.dp)
        )
    }
}

@Composable
private fun CircleButton(
    icon: ImageVector,
    description: String,
    size: androidx.compose.ui.unit.Dp,
    background: Color,
    tint: Color,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(background)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(size * 0.28f))
            .pointerInput(Unit) {
                detectTap {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(size * 0.45f))
    }
}

@Composable
private fun QuickIcon(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectTap {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MutePill(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTap {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.VolumeOff, "Mute", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("MUTE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTap(onTap: () -> Unit) {
    detectTapGestures(onTap = { onTap() })
}
