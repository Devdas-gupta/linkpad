package com.btremote.app.ui.screens.tv

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Input
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btremote.app.ui.theme.GlowCyan
import com.btremote.app.ui.theme.GlowPink
import com.btremote.app.ui.theme.GlowPrimary
import com.btremote.app.ui.theme.NeonGreen

@Composable
fun TvRemoteScreen(
    modifier: Modifier = Modifier,
    viewModel: TvRemoteViewModel = hiltViewModel()
) {
    var isPlaying by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Top system row: Power | Input | Back | Home | Menu ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TvSystemButton(Icons.Outlined.PowerSettingsNew, "Power",  Color(0xFFFF4757))  { viewModel.power() }
            TvSystemButton(Icons.Outlined.Input,             "Input",  GlowCyan)          { viewModel.inputSource() }
            TvSystemButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back",  GlowPink)      { viewModel.back() }
            TvSystemButton(Icons.Outlined.Home,              "Home",   GlowPrimary)       { viewModel.home() }
            TvSystemButton(Icons.Outlined.Menu,              "Menu",   MaterialTheme.colorScheme.onSurfaceVariant) { viewModel.menu() }
        }

        // ── D-Pad ring ──
        DPadRing(viewModel)

        // ── Volume + Channel row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TvActionButton(Icons.Outlined.VolumeDown, "Vol –", Modifier.weight(1f)) { viewModel.volumeDown() }
            TvActionButton(Icons.Outlined.VolumeOff,  "Mute",  Modifier.weight(1f)) { viewModel.mute() }
            TvActionButton(Icons.Outlined.VolumeUp,   "Vol +", Modifier.weight(1f)) { viewModel.volumeUp() }
            Spacer(Modifier.width(1.dp))
            TvActionButton(Icons.Outlined.ArrowUpward,   "CH+", Modifier.weight(1f)) { viewModel.channelUp() }
            TvActionButton(Icons.Outlined.ArrowDownward, "CH–", Modifier.weight(1f)) { viewModel.channelDown() }
        }

        // ── Playback row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvActionButton(Icons.Outlined.ChevronLeft, "Prev", Modifier.size(56.dp)) { viewModel.prevTrack() }
            TvPlayPause(isPlaying) {
                isPlaying = !isPlaying
                viewModel.playPause()
            }
            TvActionButton(Icons.Outlined.ChevronRight, "Next", Modifier.size(56.dp)) { viewModel.nextTrack() }
        }

        // ── Colour buttons ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Red"    to Color(0xFFFF4757),
                "Green"  to Color(0xFF00E5A0),
                "Yellow" to Color(0xFFFFD93D),
                "Blue"   to Color(0xFF06B6D4)
            ).forEachIndexed { idx, (label, color) ->
                val action: () -> Unit = when (idx) {
                    0 -> { { viewModel.colorRed() } }
                    1 -> { { viewModel.colorGreen() } }
                    2 -> { { viewModel.colorYellow() } }
                    else -> { { viewModel.colorBlue() } }
                }
                ColorButton(label = label, color = color, modifier = Modifier.weight(1f), onClick = action)
            }
        }

        // ── Number pad ──
        NumberPad(viewModel)

        Spacer(Modifier.height(4.dp))
    }
}

// ── D-Pad ─────────────────────────────────────────────────────────────────────

@Composable
private fun DPadRing(viewModel: TvRemoteViewModel) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            GlowPrimary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(1.dp, GlowPrimary.copy(alpha = 0.3f), CircleShape)
        )

        // Up
        DPadArrow(
            icon = Icons.Outlined.ArrowUpward, label = "Up",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
        ) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.dpadUp() }

        // Down
        DPadArrow(
            icon = Icons.Outlined.ArrowDownward, label = "Down",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        ) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.dpadDown() }

        // Left
        DPadArrow(
            icon = Icons.Outlined.ChevronLeft, label = "Left",
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)
        ) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.dpadLeft() }

        // Right
        DPadArrow(
            icon = Icons.Outlined.ChevronRight, label = "Right",
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
        ) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.dpadRight() }

        // Center OK
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.dpadOk()
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "OK",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun DPadArrow(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f), modifier = Modifier.size(28.dp))
    }
}

// ── TV Play/Pause button ──────────────────────────────────────────────────────

@Composable
private fun TvPlayPause(isPlaying: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(GlowCyan.copy(alpha = 0.35f), GlowCyan.copy(alpha = 0.10f))
                )
            )
            .border(1.dp, GlowCyan.copy(alpha = 0.7f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = "Play/Pause",
            tint = GlowCyan,
            modifier = Modifier.size(32.dp)
        )
    }
}

// ── Reusable TV buttons ───────────────────────────────────────────────────────

@Composable
private fun TvSystemButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = accentColor, modifier = Modifier.size(22.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TvActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ColorButton(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.22f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun NumberPad(viewModel: TvRemoteViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
            listOf(-1, 0, -1) // -1 = empty spacer
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { num ->
                    if (num < 0) {
                        Spacer(Modifier.weight(1f).height(48.dp))
                    } else {
                        NumberKey(num = num, modifier = Modifier.weight(1f)) { viewModel.numKey(num) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberKey(num: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = num.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
    }
}
