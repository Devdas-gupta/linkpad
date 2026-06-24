package com.btremote.app.ui.screens.touchpad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btremote.app.bluetooth.ConsumerUsage
import com.btremote.app.bluetooth.MouseButtonMask
import com.btremote.app.ui.components.MouseButton
import com.btremote.app.ui.components.MouseButtonStyle
import com.btremote.app.ui.theme.TouchpadGradientEnd
import com.btremote.app.ui.theme.TouchpadGradientStart
import kotlin.math.abs

@Composable
fun TouchpadScreen(
    modifier: Modifier = Modifier,
    viewModel: TouchpadViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val pointerSpeed = (prefs?.pointerSpeed ?: 8).coerceIn(1, 20)
    val scrollSpeed = (prefs?.scrollSpeed ?: 5).coerceIn(1, 10)
    val layout = prefs?.mouseButtonLayout ?: "left_right"
    val buttonsAtBottom = (prefs?.mouseButtonsPosition ?: "bottom") == "bottom"
    val scrollBarOnLeft = (prefs?.scrollBarPosition ?: "right") == "left"
    val showAndroidNav = prefs?.showAndroidNavButtons ?: false

    val pointerMultiplier = pointerSpeed * 0.5f
    val scrollMultiplier = scrollSpeed * 0.4f

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (!buttonsAtBottom) {
            ButtonsRow(layout, viewModel)
            Spacer(Modifier.height(12.dp))
        }
        TouchSurface(
            pointerMultiplier = pointerMultiplier,
            scrollMultiplier = scrollMultiplier,
            viewModel = viewModel,
            scrollBarOnLeft = scrollBarOnLeft,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        if (buttonsAtBottom) {
            Spacer(Modifier.height(14.dp))
            ButtonsRow(layout, viewModel)
        }
        if (showAndroidNav) {
            Spacer(Modifier.height(12.dp))
            AndroidNavRow(viewModel)
        }
    }
}

@Composable
private fun TouchSurface(
    pointerMultiplier: Float,
    scrollMultiplier: Float,
    viewModel: TouchpadViewModel,
    scrollBarOnLeft: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val touchpadColors = if (isLight) {
        listOf(Color(0xFFE8EEFA), Color(0xFFF4F7FC))
    } else {
        listOf(TouchpadGradientStart, TouchpadGradientEnd)
    }

    Row(modifier = modifier) {
        if (scrollBarOnLeft) {
            ScrollStrip(
                scrollMultiplier = scrollMultiplier,
                viewModel = viewModel,
                modifier = Modifier.width(40.dp).fillMaxHeight()
            )
            Spacer(Modifier.width(8.dp))
        }

        // Main touch surface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        colors = touchpadColors,
                        center = Offset.Unspecified,
                        radius = 900f
                    )
                )
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .pointerInput(pointerMultiplier, scrollMultiplier) {
                    var lastTapEndTime = 0L
                    awaitEachGesture {
                        val first = awaitFirstDown(requireUnconsumed = false)
                        val downTime = first.uptimeMillis  // monotonic ms since boot
                        var residualX = 0f
                        var residualY = 0f
                        var residualScroll = 0f
                        var residualHScroll = 0f
                        var totalDistance = 0f
                        var pointersSeen = 1
                        var lastEventTime = downTime

                        // Tap-to-drag: second touch within 300 ms of last tap end → hold LEFT
                        val tapDragWindowMs = 300L
                        val isDragMode = lastTapEndTime != 0L && (downTime - lastTapEndTime) < tapDragWindowMs
                        if (isDragMode) {
                            viewModel.buttonPress(MouseButtonMask.LEFT, true)
                        }

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val activePointers = event.changes.count { it.pressed }
                            if (activePointers == 0) {
                                lastEventTime = event.changes.firstOrNull()?.uptimeMillis ?: lastEventTime
                                break
                            }
                            lastEventTime = event.changes.firstOrNull()?.uptimeMillis ?: lastEventTime
                            if (activePointers > pointersSeen) pointersSeen = activePointers

                            if (activePointers >= 2 && !isDragMode) {
                                val delta = event.changes
                                    .filter { it.pressed }
                                    .map { it.positionChange() }
                                    .reduceOrNull { a, b -> Offset(a.x + b.x, a.y + b.y) }
                                    ?: Offset.Zero
                                val avgY = delta.y / activePointers.toFloat()
                                val avgX = delta.x / activePointers.toFloat()
                                residualScroll  += -avgY * scrollMultiplier * 0.06f
                                residualHScroll += avgX * scrollMultiplier * 0.06f
                                val s = residualScroll.toInt()
                                val h = residualHScroll.toInt()
                                if (s != 0) { viewModel.scroll(s); residualScroll  -= s }
                                if (h != 0) { viewModel.hScroll(h); residualHScroll -= h }
                                totalDistance += abs(delta.x) + abs(delta.y)
                                event.changes.forEach { it.consume() }
                            } else {
                                val change = event.changes.firstOrNull { it.pressed }
                                if (change != null) {
                                    val dx = change.positionChange().x
                                    val dy = change.positionChange().y
                                    totalDistance += abs(dx) + abs(dy)
                                    if (isDragMode || totalDistance > 6f) {
                                        residualX += dx * pointerMultiplier
                                        residualY += dy * pointerMultiplier
                                        val ix = residualX.toInt()
                                        val iy = residualY.toInt()
                                        if (ix != 0 || iy != 0) {
                                            viewModel.move(ix, iy)
                                            residualX -= ix
                                            residualY -= iy
                                        }
                                        change.consume()
                                    }
                                }
                            }
                        }

                        val durationMs = lastEventTime - downTime

                        if (isDragMode) {
                            viewModel.buttonPress(MouseButtonMask.LEFT, false)
                            lastTapEndTime = 0L
                        } else {
                            // Tap = quick + low-distance. Generous thresholds match laptop trackpad feel.
                            val isTap = totalDistance < 18f && durationMs < 280L
                            if (isTap) {
                                when (pointersSeen) {
                                    1 -> {
                                        viewModel.tapClick(MouseButtonMask.LEFT)
                                        lastTapEndTime = lastEventTime
                                    }
                                    else -> {
                                        viewModel.tapClick(MouseButtonMask.RIGHT)
                                        lastTapEndTime = 0L
                                    }
                                }
                            } else {
                                lastTapEndTime = 0L
                            }
                        }
                    }
                }
        ) {
            // Subtle hint overlays on the touchpad surface
            Text(
                "TOUCHPAD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.TopStart).padding(20.dp)
            )
            Icon(
                imageVector = Icons.Outlined.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp).align(Alignment.Center)
            )
            Text(
                "Use like touchpad",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
            )
        }

        if (!scrollBarOnLeft) {
            Spacer(Modifier.width(8.dp))
            // Right-edge scroll strip
            ScrollStrip(
                scrollMultiplier = scrollMultiplier,
                viewModel = viewModel,
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
            )
        }
    }
}

// ── Android Nav Buttons Row ──────────────────────────────────────────────────

@Composable
private fun AndroidNavRow(viewModel: TouchpadViewModel) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Back
        AndroidNavButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            label = "Back",
            modifier = Modifier.weight(1f)
        ) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.sendConsumer(ConsumerUsage.AC_BACK) }
        // Home
        AndroidNavButton(
            icon = Icons.Outlined.Home,
            label = "Home",
            modifier = Modifier.weight(1f)
        ) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.sendConsumer(ConsumerUsage.AC_HOME) }
        // Recents (App Switcher) — sent as Super+Tab (common HID mapping)
        AndroidNavButton(
            icon = Icons.Outlined.ViewCarousel,
            label = "Recents",
            modifier = Modifier.weight(1f)
        ) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.recentApps() }
    }
}

@Composable
private fun AndroidNavButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val ev = awaitPointerEvent(PointerEventPass.Main)
                    } while (ev.changes.any { it.pressed })
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScrollStrip(
    scrollMultiplier: Float,
    viewModel: TouchpadViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .pointerInput(scrollMultiplier) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var residualScroll = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.changes.none { it.pressed }) break
                        val change = event.changes.firstOrNull { it.pressed } ?: continue
                        val dy = change.positionChange().y
                        residualScroll += -dy * scrollMultiplier * 0.06f
                        val s = residualScroll.toInt()
                        if (s != 0) {
                            viewModel.scroll(s)
                            residualScroll -= s
                        }
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.KeyboardArrowUp,
                "Scroll up",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(60.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                "Scroll down",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ButtonsRow(layout: String, viewModel: TouchpadViewModel) {
    val showMiddle = layout == "left_middle_right"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MouseButton(
            label = "LEFT",
            style = MouseButtonStyle.PRIMARY,
            modifier = Modifier.weight(1f),
            onPressChange = { viewModel.buttonPress(MouseButtonMask.LEFT, it) }
        )
        if (showMiddle) {
            MiddleButton(viewModel)
        }
        MouseButton(
            label = "RIGHT",
            style = MouseButtonStyle.NEUTRAL,
            modifier = Modifier.weight(1f),
            onPressChange = { viewModel.buttonPress(MouseButtonMask.RIGHT, it) }
        )
    }
}

@Composable
private fun MiddleButton(viewModel: TouchpadViewModel) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    viewModel.buttonPress(MouseButtonMask.MIDDLE, true)
                    while (true) {
                        val ev = awaitPointerEvent()
                        if (ev.changes.none { it.pressed }) break
                    }
                    viewModel.buttonPress(MouseButtonMask.MIDDLE, false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "·",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
