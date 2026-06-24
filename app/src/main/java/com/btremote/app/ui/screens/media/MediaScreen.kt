package com.btremote.app.ui.screens.media

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btremote.app.bluetooth.ConsumerUsage
import com.btremote.app.ui.theme.ErrorRed
import com.btremote.app.ui.theme.GlowCyan
import com.btremote.app.ui.theme.GlowPink
import com.btremote.app.ui.theme.GlowPrimary
import com.btremote.app.ui.theme.NeonGreen

@Composable
fun MediaScreen(
    modifier: Modifier = Modifier,
    viewModel: MediaViewModel = hiltViewModel()
) {
    var isPlaying by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Section label
        Text(
            "MEDIA CONTROL",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            color = GlowPrimary.copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        // ── Volume & Brightness row ──────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickControlButton(
                icon = Icons.Outlined.BrightnessLow,
                label = "BRIGHT –",
                accentColor = GlowCyan,
                modifier = Modifier.weight(1f)
            ) { viewModel.send(ConsumerUsage.BRIGHTNESS_DOWN) }
            QuickControlButton(
                icon = Icons.Outlined.VolumeDown,
                label = "VOL –",
                accentColor = GlowPrimary,
                modifier = Modifier.weight(1f)
            ) { viewModel.send(ConsumerUsage.VOLUME_DOWN) }
            QuickControlButton(
                icon = Icons.Outlined.VolumeUp,
                label = "VOL +",
                accentColor = GlowPrimary,
                modifier = Modifier.weight(1f)
            ) { viewModel.send(ConsumerUsage.VOLUME_UP) }
            QuickControlButton(
                icon = Icons.Outlined.BrightnessHigh,
                label = "BRIGHT +",
                accentColor = GlowCyan,
                modifier = Modifier.weight(1f)
            ) { viewModel.send(ConsumerUsage.BRIGHTNESS_UP) }
        }

        Spacer(Modifier.weight(1f))

        // ── Play / Pause big button centred ─────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PlayPauseButton(isPlaying = isPlaying) {
                isPlaying = !isPlaying
                viewModel.send(ConsumerUsage.PLAY_PAUSE)
            }
        }

        Spacer(Modifier.weight(1f))

        // ── Transport row  ──  PREV | ⏮ ◀◀ ▶▶ ⏭ | NEXT ─────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportButton(
                icon = Icons.Outlined.SkipPrevious,
                label = "PREV",
                accentColor = GlowPink,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.send(ConsumerUsage.PREV_TRACK) }
            )

            TransportButton(
                icon = Icons.Outlined.FastRewind,
                label = "RW",
                accentColor = GlowPink,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.seekBackward() },
                onPressStart = { viewModel.seekBackwardStart() },
                onPressEnd = { viewModel.seekStop() }
            )

            TransportButton(
                icon = Icons.Outlined.FastForward,
                label = "FF",
                accentColor = GlowPink,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.seekForward() },
                onPressStart = { viewModel.seekForwardStart() },
                onPressEnd = { viewModel.seekStop() }
            )

            TransportButton(
                icon = Icons.Outlined.SkipNext,
                label = "NEXT",
                accentColor = GlowPink,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.send(ConsumerUsage.NEXT_TRACK) }
            )
        }

        // ── Mute pill ────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            MutePill { viewModel.send(ConsumerUsage.MUTE) }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Play / Pause
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "playPulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isPlaying) 1.06f else 1f,
        animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Reverse),
        label = "pulseScale"
    )
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(108.dp)
            .scale(if (isPlaying) pulse else 1f)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        GlowPrimary.copy(alpha = if (pressed) 0.45f else 0.30f),
                        GlowPrimary.copy(alpha = if (pressed) 0.15f else 0.08f)
                    )
                )
            )
            .border(
                width = 2.dp,
                color = GlowPrimary.copy(alpha = if (pressed) 0.85f else 0.55f),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            awaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = "Play/Pause",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(48.dp)
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Normal tap transport button (PREV / RW / FF / NEXT)
// ────────────────────────────────────────────────────────────────────────────

// Hold threshold: press-and-hold beyond this → continuous seek; below → single tap
private const val HOLD_THRESHOLD_MS = 180L

@Composable
private fun TransportButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 60.dp,
    onClick: (() -> Unit)? = null,
    onPressStart: (() -> Unit)? = null,
    onPressEnd: (() -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (pressed) accentColor.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (pressed) accentColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    if (onPressStart != null) {
                        // Hold-capable button (FF / RW): distinguish tap vs hold
                        val releasedBeforeThreshold = withTimeoutOrNull(HOLD_THRESHOLD_MS) {
                            // Suspends here until pointer lifts — returns non-null if released quickly
                            do {
                                val ev = awaitPointerEvent(PointerEventPass.Main)
                            } while (ev.changes.any { it.pressed })
                            true
                        }
                        if (releasedBeforeThreshold != null) {
                            // Quick tap → single seek
                            onClick?.invoke()
                        } else {
                            // Hold → start continuous seek, wait for release
                            onPressStart.invoke()
                            do {
                                val ev = awaitPointerEvent(PointerEventPass.Main)
                            } while (ev.changes.any { it.pressed })
                            onPressEnd?.invoke()
                        }
                    } else {
                        // Simple tap button (PREV / NEXT): fire onClick on release
                        do {
                            val ev = awaitPointerEvent(PointerEventPass.Main)
                        } while (ev.changes.any { it.pressed })
                        onClick?.invoke()
                    }

                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (pressed) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    fontSize = 9.sp
                ),
                color = if (pressed) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Quick vol/bright tap buttons (top row)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickControlButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (pressed) accentColor.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (pressed) accentColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            awaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (pressed) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp, fontSize = 8.sp),
            color = if (pressed) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Mute pill
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun MutePill(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (pressed) ErrorRed.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (pressed) ErrorRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            awaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                )
            }
            .padding(horizontal = 32.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.VolumeOff,
            contentDescription = "Mute",
            tint = if (pressed) ErrorRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "MUTE",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
            color = if (pressed) ErrorRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
    }
}
