package com.btremote.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

enum class MouseButtonStyle { PRIMARY, NEUTRAL, COMPACT }

@Composable
fun MouseButton(
    label: String,
    modifier: Modifier = Modifier,
    style: MouseButtonStyle = MouseButtonStyle.NEUTRAL,
    onPressChange: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val baseBg = when (style) {
        MouseButtonStyle.PRIMARY -> MaterialTheme.colorScheme.primary
        MouseButtonStyle.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
        MouseButtonStyle.COMPACT -> MaterialTheme.colorScheme.surfaceVariant
    }
    val pressedBg = when (style) {
        MouseButtonStyle.PRIMARY -> MaterialTheme.colorScheme.primary
        MouseButtonStyle.NEUTRAL -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        MouseButtonStyle.COMPACT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    }
    val bg = if (pressed) pressedBg else baseBg
    val fg = when {
        style == MouseButtonStyle.PRIMARY -> Color.White
        pressed -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    val scale by animateFloatAsState(targetValue = if (pressed) 0.94f else 1f, animationSpec = tween(120), label = "btnScale")
    val shape = if (style == MouseButtonStyle.COMPACT) RoundedCornerShape(50) else RoundedCornerShape(20.dp)
    val minH = if (style == MouseButtonStyle.COMPACT) 48.dp else 64.dp

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = minH)
            .clip(shape)
            .background(bg)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (style == MouseButtonStyle.PRIMARY) 0f else 1f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyDown = event.changes.any { it.pressed }
                        if (anyDown && !pressed) {
                            pressed = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPressChange(true)
                        } else if (!anyDown && pressed) {
                            pressed = false
                            onPressChange(false)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = fg,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
