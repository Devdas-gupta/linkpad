package com.btremote.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mouse
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    val shape = RoundedCornerShape(22.dp)
    val minHeight = when (style) {
        MouseButtonStyle.COMPACT -> 52.dp
        else -> 80.dp   // bigger, easy to press
    }

    val baseBg = when (style) {
        MouseButtonStyle.PRIMARY -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val pressedBg = when (style) {
        MouseButtonStyle.PRIMARY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.80f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    }
    val bg = if (pressed) pressedBg else baseBg
    val fgBase = if (style == MouseButtonStyle.PRIMARY) Color.White else MaterialTheme.colorScheme.onSurface
    val fg = if (pressed && style != MouseButtonStyle.PRIMARY) MaterialTheme.colorScheme.primary else fgBase

    val borderColor = when {
        style == MouseButtonStyle.PRIMARY -> Color.Transparent
        pressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outline
    }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "btnScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = minHeight)
            .shadow(
                elevation = if (pressed) 0.dp else 3.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPressChange(true)
                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Main)
                        if (ev.changes.none { it.pressed }) break
                    }
                    pressed = false
                    onPressChange(false)
                }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Mouse,
                contentDescription = label,
                tint = fg.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                color = fg,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp
                )
            )
        }
    }
}
