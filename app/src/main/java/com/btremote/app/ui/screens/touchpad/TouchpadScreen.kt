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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        if (buttonsAtBottom) {
            Spacer(Modifier.height(16.dp))
            ButtonsRow(layout, viewModel)
        }
    }
}

@Composable
private fun TouchSurface(
    pointerMultiplier: Float,
    scrollMultiplier: Float,
    viewModel: TouchpadViewModel,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        // Main touch surface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(TouchpadGradientStart, TouchpadGradientEnd),
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
                                residualScroll += -avgY * scrollMultiplier * 0.06f
                                residualHScroll += avgX * scrollMultiplier * 0.06f
                                val s = residualScroll.toInt()
                                val h = residualHScroll.toInt()
                                if (s != 0) {
                                    viewModel.scroll(s)
                                    residualScroll -= s
                                }
                                if (h != 0) {
                                    viewModel.hScroll(h)
                                    residualHScroll -= h
                                }
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
            Text(
                "TOUCHPAD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.TopStart).padding(20.dp)
            )
            Icon(
                imageVector = Icons.Outlined.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(72.dp).align(Alignment.Center)
            )
            Text(
                "drag • tap • 2-finger tap = right • tap-and-hold = drag",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
            )
        }

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
            label = "LEFT CLICK",
            style = MouseButtonStyle.PRIMARY,
            modifier = Modifier.weight(1f),
            onPressChange = { viewModel.buttonPress(MouseButtonMask.LEFT, it) }
        )
        if (showMiddle) {
            Box(
                modifier = Modifier
                    .size(56.dp)
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
                Text("·", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
            }
        }
        MouseButton(
            label = "RIGHT CLICK",
            style = MouseButtonStyle.NEUTRAL,
            modifier = Modifier.weight(1f),
            onPressChange = { viewModel.buttonPress(MouseButtonMask.RIGHT, it) }
        )
    }
}
