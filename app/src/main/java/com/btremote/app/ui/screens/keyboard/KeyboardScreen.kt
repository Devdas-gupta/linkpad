package com.btremote.app.ui.screens.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardHide
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btremote.app.bluetooth.HidKeyCode
import com.btremote.app.bluetooth.MODIFIER_LEFT_ALT
import com.btremote.app.bluetooth.MODIFIER_LEFT_CTRL
import com.btremote.app.bluetooth.MODIFIER_LEFT_GUI
import com.btremote.app.bluetooth.MODIFIER_LEFT_SHIFT

private const val SENTINEL = "​" // zero-width space anchor

@Composable
fun KeyboardScreen(
    modifier: Modifier = Modifier,
    viewModel: KeyboardViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val showShortcuts = prefs?.showShortcuts ?: false
    val showFKeys = prefs?.showFKeys ?: false
    val showArrows = prefs?.showArrows ?: false
    val showEdit = prefs?.showEdit ?: false
    val targetOs by viewModel.effectiveTargetOs.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LiveTypingField(viewModel)

        if (showShortcuts) {
            SectionLabel("SHORTCUTS")
            ShortcutChips(viewModel, targetOs)
        }
        if (showFKeys) {
            SectionLabel("FUNCTION KEYS")
            FKeyChips(viewModel)
        }

        if (showEdit) {
            SectionLabel("EDIT")
            EditChips(viewModel)
        }

        if (showArrows) {
            SectionLabel("NAVIGATION")
            ArrowCluster(viewModel)
        }
    }
}

@Composable
private fun LiveTypingField(viewModel: KeyboardViewModel) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var value by remember { mutableStateOf(TextFieldValue(SENTINEL, androidx.compose.ui.text.TextRange(SENTINEL.length))) }
    var prev by remember { mutableStateOf(SENTINEL) }
    var hasFocus by remember { mutableStateOf(false) }

    fun clearLocal() {
        val reset = TextFieldValue(SENTINEL, androidx.compose.ui.text.TextRange(SENTINEL.length))
        prev = SENTINEL
        value = reset
    }

    fun showKb() {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun hideKb() {
        keyboardController?.hide()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = { new ->
                    val newText = new.text
                    val oldText = prev

                    if (oldText == newText) {
                        value = new
                        return@BasicTextField
                    }

                    val oldClean = oldText.removePrefix(SENTINEL)
                    val newClean = newText.removePrefix(SENTINEL)

                    if (newText.isEmpty() || !newText.startsWith(SENTINEL)) {
                        val delete = oldClean.length + 1
                        repeat(delete) { viewModel.pressKey(HidKeyCode.BACKSPACE) }
                        clearLocal()
                        return@BasicTextField
                    }

                    var common = 0
                    val min = minOf(oldClean.length, newClean.length)
                    while (common < min && oldClean[common] == newClean[common]) common++

                    val deletions = oldClean.length - common
                    val toType = newClean.substring(common)

                    repeat(deletions) { viewModel.pressKey(HidKeyCode.BACKSPACE) }
                    for (c in toType) viewModel.typeChar(c)

                    value = new
                    prev = newText
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { hasFocus = it.isFocused }
                    .onPreviewKeyEvent { ev ->
                        if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (ev.key) {
                            Key.Enter, Key.NumPadEnter -> {
                                viewModel.pressKey(HidKeyCode.ENTER); true
                            }
                            Key.Tab -> { viewModel.pressKey(HidKeyCode.TAB); true }
                            Key.DirectionUp -> { viewModel.pressKey(HidKeyCode.UP_ARROW); true }
                            Key.DirectionDown -> { viewModel.pressKey(HidKeyCode.DOWN_ARROW); true }
                            Key.DirectionLeft -> { viewModel.pressKey(HidKeyCode.LEFT_ARROW); true }
                            Key.DirectionRight -> { viewModel.pressKey(HidKeyCode.RIGHT_ARROW); true }
                            else -> {
                                val code = ev.utf16CodePoint
                                if (code in 32..126) {
                                    viewModel.typeChar(code.toChar()); true
                                } else false
                            }
                        }
                    },
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                ),
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.None
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionPill(
                icon = Icons.Outlined.Clear,
                label = "CLEAR",
                modifier = Modifier.weight(1f),
                onClick = { clearLocal() }
            )
            ActionPill(
                icon = if (hasFocus) Icons.Outlined.KeyboardHide else Icons.Outlined.Keyboard,
                label = if (hasFocus) "HIDE KEYBOARD" else "SHOW KEYBOARD",
                modifier = Modifier.weight(1.4f),
                primary = !hasFocus,
                onClick = { if (hasFocus) hideKb() else showKb() }
            )
        }
    }
}

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (primary) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun ShortcutChips(viewModel: KeyboardViewModel, targetOs: String) {
    val isMac = targetOs == "mac"
    val primaryMod = if (isMac) MODIFIER_LEFT_GUI else MODIFIER_LEFT_CTRL
    val modLabel = if (isMac) "⌘" else "Ctrl"
    val items = listOf(
        "Esc" to { viewModel.pressKey(HidKeyCode.ESCAPE) },
        "Tab" to { viewModel.pressKey(HidKeyCode.TAB) },
        "Enter" to { viewModel.pressKey(HidKeyCode.ENTER) },
        "$modLabel+C" to { viewModel.pressKey(HidKeyCode.C, primaryMod) },
        "$modLabel+V" to { viewModel.pressKey(HidKeyCode.V, primaryMod) },
        "$modLabel+X" to { viewModel.pressKey(HidKeyCode.X, primaryMod) },
        "$modLabel+Z" to { viewModel.pressKey(HidKeyCode.Z, primaryMod) },
        "$modLabel+A" to { viewModel.pressKey(HidKeyCode.A, primaryMod) },
        "Alt+Tab" to { viewModel.pressKey(HidKeyCode.TAB, MODIFIER_LEFT_ALT) },
        (if (isMac) "⌘" else "Win") to { viewModel.pressKey(HidKeyCode.NONE, MODIFIER_LEFT_GUI) },
        "Shift" to { viewModel.pressKey(HidKeyCode.NONE, MODIFIER_LEFT_SHIFT) }
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { (label, action) -> Chip(label, onClick = action) }
    }
}

@Composable
private fun FKeyChips(viewModel: KeyboardViewModel) {
    val keys = listOf(
        "F1" to HidKeyCode.F1, "F2" to HidKeyCode.F2, "F3" to HidKeyCode.F3, "F4" to HidKeyCode.F4,
        "F5" to HidKeyCode.F5, "F6" to HidKeyCode.F6, "F7" to HidKeyCode.F7, "F8" to HidKeyCode.F8,
        "F9" to HidKeyCode.F9, "F10" to HidKeyCode.F10, "F11" to HidKeyCode.F11, "F12" to HidKeyCode.F12
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(keys) { (label, code) -> Chip(label, onClick = { viewModel.pressKey(code) }) }
    }
}

@Composable
private fun EditChips(viewModel: KeyboardViewModel) {
    val rows = listOf(
        listOf("Backspace" to HidKeyCode.BACKSPACE, "Del" to HidKeyCode.DELETE, "Home" to HidKeyCode.HOME, "End" to HidKeyCode.END),
        listOf("PgUp" to HidKeyCode.PAGE_UP, "PgDn" to HidKeyCode.PAGE_DOWN, "Insert" to HidKeyCode.INSERT)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, code) ->
                    Chip(label, modifier = Modifier.weight(1f), onClick = { viewModel.pressKey(code) })
                }
            }
        }
    }
}

@Composable
private fun ArrowCluster(viewModel: KeyboardViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Chip("↑", modifier = Modifier.size(72.dp, 56.dp), onClick = { viewModel.pressKey(HidKeyCode.UP_ARROW) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("←", modifier = Modifier.size(72.dp, 56.dp), onClick = { viewModel.pressKey(HidKeyCode.LEFT_ARROW) })
            Chip("↓", modifier = Modifier.size(72.dp, 56.dp), onClick = { viewModel.pressKey(HidKeyCode.DOWN_ARROW) })
            Chip("→", modifier = Modifier.size(72.dp, 56.dp), onClick = { viewModel.pressKey(HidKeyCode.RIGHT_ARROW) })
        }
    }
}

@Composable
private fun Chip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
