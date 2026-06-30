package com.btremote.app.ui.screens.keyboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardCommandKey
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.btremote.app.bluetooth.HidKeyCode
import com.btremote.app.bluetooth.MODIFIER_LEFT_ALT
import com.btremote.app.bluetooth.MODIFIER_LEFT_CTRL
import com.btremote.app.bluetooth.MODIFIER_LEFT_GUI
import com.btremote.app.bluetooth.MODIFIER_LEFT_SHIFT
import com.btremote.app.data.CustomShortcut
import com.btremote.app.ui.theme.GlowCyan
import com.btremote.app.ui.theme.GlowPink
import com.btremote.app.ui.theme.GlowPrimary
import com.btremote.app.ui.theme.NeonGreen
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
//  Helper — build modifier label list for a given OS
// ─────────────────────────────────────────────────────────────────────────────

private data class ModKey(val label: String, val bit: Int, val macOnly: Boolean = false, val winOnly: Boolean = false)

private val MOD_KEYS_MAC = listOf(
    ModKey("⌘ Cmd",    MODIFIER_LEFT_GUI),
    ModKey("⌃ Ctrl",   MODIFIER_LEFT_CTRL),
    ModKey("⌥ Option", MODIFIER_LEFT_ALT),
    ModKey("⇧ Shift",  MODIFIER_LEFT_SHIFT)
)
private val MOD_KEYS_WIN = listOf(
    ModKey("Ctrl",   MODIFIER_LEFT_CTRL),
    ModKey("Shift",  MODIFIER_LEFT_SHIFT),
    ModKey("Alt",    MODIFIER_LEFT_ALT),
    ModKey("Win ⊞",  MODIFIER_LEFT_GUI)
)

private fun modLabelsFor(modifiers: Int, os: String): String {
    val keys = if (os == "mac") MOD_KEYS_MAC else MOD_KEYS_WIN
    return keys.filter { modifiers and it.bit != 0 }.joinToString(" + ") { it.label }
}

private fun keyLabel(code: Int): String? {
    if (code == 0) return null
    val k = HidKeyCode.values().firstOrNull { it.code.toInt() and 0xFF == code } ?: return null
    return when (k) {
        HidKeyCode.SPACE -> "Space"
        HidKeyCode.ENTER -> "Enter"
        HidKeyCode.ESCAPE -> "Esc"
        HidKeyCode.BACKSPACE -> "⌫"
        HidKeyCode.DELETE -> "Del"
        HidKeyCode.TAB -> "Tab"
        HidKeyCode.UP_ARROW -> "↑"
        HidKeyCode.DOWN_ARROW -> "↓"
        HidKeyCode.LEFT_ARROW -> "←"
        HidKeyCode.RIGHT_ARROW -> "→"
        HidKeyCode.PAGE_UP -> "PgUp"
        HidKeyCode.PAGE_DOWN -> "PgDn"
        HidKeyCode.CAPS_LOCK -> "Caps"
        HidKeyCode.PRINT_SCREEN -> "PrtSc"
        HidKeyCode.HOME -> "Home"
        HidKeyCode.END -> "End"
        HidKeyCode.INSERT -> "Ins"
        else -> when {
            k.name.length == 1 -> k.name
            k.name.startsWith("NUM_") -> k.name.removePrefix("NUM_")
            k.name.startsWith("F") && k.name.drop(1).all { it.isDigit() } -> k.name
            else -> k.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}

private fun comboLabel(sc: CustomShortcut): String {
    val mods = modLabelsFor(sc.modifiers, sc.os.takeIf { it != "both" } ?: "win")
    val key  = keyLabel(sc.keyCode)
    return listOfNotNull(mods.ifBlank { null }, key).joinToString(" + ")
}

// ─────────────────────────────────────────────────────────────────────────────
//  Public chip row — shown inside KeyboardScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CustomShortcutRow(
    shortcuts: List<CustomShortcut>,
    targetOs: String,
    onFire: (CustomShortcut) -> Unit,
    onAdd: () -> Unit,
    onEdit: (CustomShortcut) -> Unit,
    onDelete: (CustomShortcut) -> Unit
) {
    val visible = shortcuts.filter {
        it.os == "both" || it.os == targetOs || targetOs == "auto"
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(visible, key = { it.id }) { sc ->
            CustomShortcutChip(
                shortcut = sc,
                onFire   = { onFire(sc) },
                onEdit   = { onEdit(sc) },
                onDelete = { onDelete(sc) }
            )
        }
        item {
            // + Add button
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlowPrimary.copy(alpha = 0.10f))
                    .border(1.dp, GlowPrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) { detectTapGestures(onTap = { onAdd() }) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Add, "Add shortcut", tint = GlowPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Individual chip with press-scale + long-press actions
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CustomShortcutChip(
    shortcut: CustomShortcut,
    onFire: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var pressed      by remember { mutableStateOf(false) }
    var showActions  by remember { mutableStateOf(false) }

    val accent = when (shortcut.os) {
        "mac"  -> GlowCyan
        "win"  -> GlowPrimary
        else   -> NeonGreen
    }
    val scale  by animateFloatAsState(if (pressed) 0.93f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "cs")
    val bgAlpha by animateFloatAsState(if (pressed) 0.22f else 0.09f, tween(80), label = "csbg")

    Box(
        modifier = Modifier
            .scale(scale)
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = bgAlpha))
            .border(1.dp, if (pressed) accent else accent.copy(alpha = 0.38f), RoundedCornerShape(12.dp))
            .pointerInput(shortcut.id) {
                detectTapGestures(
                    onPress      = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap        = { onFire() },
                    onLongPress  = { showActions = true }
                )
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            // Tiny OS badge
            Text(
                text = when (shortcut.os) { "mac" -> "⌘" ; "win" -> "⊞" ; else -> "✦" },
                fontSize = 9.sp,
                color = accent.copy(alpha = 0.65f),
                fontWeight = FontWeight.Bold
            )
            Text(
                shortcut.label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (pressed) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showActions) {
        ShortcutActionsDialog(
            shortcut = shortcut,
            onEdit = onEdit,
            onDelete = onDelete,
            onDismiss = { showActions = false }
        )
    }
}

@Composable
private fun ShortcutActionsDialog(
    shortcut: CustomShortcut,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = when (shortcut.os) {
        "mac"  -> GlowCyan
        "win"  -> GlowPrimary
        else   -> NeonGreen
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header (Shortcut name + Combo preview)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = shortcut.label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = comboLabel(shortcut),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = accent
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Actions List
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Edit Button
                    ActionRowButton(
                        label = "Edit Shortcut",
                        icon = Icons.Outlined.Edit,
                        color = accent
                    ) {
                        onDismiss()
                        onEdit()
                    }

                    // Delete Button
                    ActionRowButton(
                        label = "Delete Shortcut",
                        icon = Icons.Outlined.Delete,
                        color = GlowPink
                    ) {
                        onDismiss()
                        onDelete()
                    }
                }

                // Dismiss Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRowButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Add / Edit dialog
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomShortcutEditorDialog(
    existing: CustomShortcut? = null,
    onSave: (CustomShortcut) -> Unit,
    onDismiss: () -> Unit
) {
    var label      by remember { mutableStateOf(existing?.label ?: "") }
    var selectedOs by remember { mutableStateOf(existing?.os ?: "both") }
    var modifiers  by remember { mutableIntStateOf(existing?.modifiers ?: 0) }
    var keyCode    by remember { mutableIntStateOf(existing?.keyCode ?: 0) }
    var keyGroup   by remember { mutableStateOf("Letters") }

    // ── Key data computed once at dialog root — NOT inside nested lambdas ──
    // This ensures Compose tracks keyGroup reads at the right recomposition scope.
    val letters = remember { HidKeyCode.values().filter { it.name.length == 1 && it.name[0].isLetter() }.sortedBy { it.name } }
    val numbers = remember { HidKeyCode.values().filter { it.name.startsWith("NUM_") }.sortedBy { it.name } }
    val fkeys   = remember { HidKeyCode.values().filter { it.name.length in 2..3 && it.name[0] == 'F' && it.name.drop(1).all { c -> c.isDigit() } }.sortedBy { it.code } }
    val special = remember {
        listOf(
            HidKeyCode.SPACE, HidKeyCode.ENTER, HidKeyCode.TAB, HidKeyCode.ESCAPE,
            HidKeyCode.BACKSPACE, HidKeyCode.DELETE,
            HidKeyCode.HOME, HidKeyCode.END, HidKeyCode.INSERT,
            HidKeyCode.PAGE_UP, HidKeyCode.PAGE_DOWN,
            HidKeyCode.UP_ARROW, HidKeyCode.DOWN_ARROW,
            HidKeyCode.LEFT_ARROW, HidKeyCode.RIGHT_ARROW,
            HidKeyCode.PRINT_SCREEN, HidKeyCode.SCROLL_LOCK,
            HidKeyCode.PAUSE, HidKeyCode.CAPS_LOCK
        )
    }
    // Recomputed every time keyGroup state changes — at dialog root scope
    val activeKeys: List<HidKeyCode> = when (keyGroup) {
        "Numbers" -> numbers
        "F-Keys"  -> fkeys
        "Special" -> special
        else      -> letters
    }

    val isMac      = selectedOs == "mac"
    val modKeys    = if (isMac) MOD_KEYS_MAC else MOD_KEYS_WIN
    val labelError = label.isBlank()
    val comboError = modifiers == 0 && keyCode == 0
    val accentColor = when (selectedOs) { "mac" -> GlowCyan ; "win" -> GlowPrimary ; else -> NeonGreen }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {

                // ── Header ──────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.KeyboardCommandKey, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            if (existing == null) "New Shortcut" else "Edit Shortcut",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (modifiers != 0 || keyCode != 0) {
                            // Live combo preview in header
                            AnimatedContent(
                                targetState = comboLabel(
                                    CustomShortcut("", label.ifBlank { "…" }, selectedOs, modifiers, keyCode)
                                ),
                                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(100)) },
                                label = "preview"
                            ) { combo ->
                                Text(combo, style = MaterialTheme.typography.labelSmall, color = accentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Outlined.Close, "Cancel", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // ── Button name ─────────────────────────────────────────────
                DialogSection("BUTTON NAME") {
                    BasicTextField(
                        value = label,
                        onValueChange = { if (it.length <= 20) label = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(accentColor),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .border(
                                        1.dp,
                                        if (labelError && label.isNotEmpty()) GlowPink.copy(alpha = 0.6f)
                                        else accentColor.copy(alpha = 0.25f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 11.dp)
                            ) {
                                if (label.isEmpty()) Text(
                                    "e.g.  Screenshot, Mission Control, Snap Left…",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    fontSize = 13.sp
                                )
                                inner()
                            }
                        }
                    )
                }

                // ── Platform ────────────────────────────────────────────────
                DialogSection("PLATFORM") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("mac",  "macOS ⌘", GlowCyan),
                            Triple("win",  "Windows ⊞", GlowPrimary),
                            Triple("both", "Both ✦", NeonGreen)
                        ).forEach { (os, lbl, col) ->
                            PlatformToggle(lbl, col, selectedOs == os) { selectedOs = os }
                        }
                    }
                }

                // ── Modifier keys — 2×2 grid, large tap targets ─────────────
                DialogSection("MODIFIER KEYS") {
                    // Two rows of two keys each — avoids FlowRow touch bleeding
                    val rows = modKeys.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { rowKeys ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowKeys.forEach { mk ->
                                    val on = modifiers and mk.bit != 0
                                    ModifierButton(
                                        label    = mk.label,
                                        selected = on,
                                        color    = accentColor,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        modifiers = modifiers xor mk.bit
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Key picker ──────────────────────────────────────────────
                DialogSection("KEY  (optional for modifier-only shortcuts)") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                        // Tab row — plain Row, no LazyRow recycling bugs
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf("Letters", "Numbers", "F-Keys", "Special").forEach { group ->
                                val sel = keyGroup == group
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (sel) accentColor.copy(alpha = 0.16f) else Color.Transparent)
                                        .border(1.dp,
                                            if (sel) accentColor.copy(alpha = 0.65f)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                                            RoundedCornerShape(50))
                                        .pointerInput(group) {
                                            detectTapGestures(onTap = { keyGroup = group })
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        group,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (sel) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Key grid — reads activeKeys from dialog root scope
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement   = Arrangement.spacedBy(6.dp)
                        ) {
                            PillToggle("—", keyCode == 0, MaterialTheme.colorScheme.onSurfaceVariant) { keyCode = 0 }
                            activeKeys.forEach { k ->
                                val kv  = k.code.toInt() and 0xFF
                                val lbl = keyLabel(kv) ?: return@forEach
                                PillToggle(lbl, keyCode == kv, accentColor) { keyCode = kv }
                            }
                        }
                    }
                }

                // ── Validation messages ─────────────────────────────────────
                if (labelError) Text("⚠  Enter a button name", style = MaterialTheme.typography.labelSmall, color = GlowPink)
                if (comboError) Text("⚠  Choose at least one modifier key or key", style = MaterialTheme.typography.labelSmall, color = GlowPink)

                // ── Save button ─────────────────────────────────────────────
                val canSave = !labelError && !comboError
                var savePressState by remember { mutableStateOf(false) }
                val saveBg by animateColorAsState(
                    targetValue = when { !canSave -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f) ; savePressState -> accentColor.copy(alpha = 0.75f) ; else -> accentColor },
                    animationSpec = tween(120), label = "saveBg"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(saveBg)
                        .pointerInput(canSave) {
                            if (canSave) detectTapGestures(
                                onPress = { savePressState = true; tryAwaitRelease(); savePressState = false },
                                onTap   = {
                                    onSave(CustomShortcut(
                                        id        = existing?.id ?: UUID.randomUUID().toString(),
                                        label     = label.trim(),
                                        os        = selectedOs,
                                        modifiers = modifiers,
                                        keyCode   = keyCode
                                    ))
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (existing == null) "ADD SHORTCUT" else "SAVE CHANGES",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = if (canSave) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Reusable atoms
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DialogSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        content()
    }
}

@Composable
private fun PlatformToggle(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        if (selected) color.copy(alpha = 0.18f) else if (pressed) color.copy(alpha = 0.08f) else Color.Transparent,
        tween(100), label = "ptbg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, if (selected) color.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(50))
            .pointerInput(label, selected, onClick) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap   = { onClick() }
                )
            }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PillToggle(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        when { selected -> color.copy(alpha = 0.20f) ; pressed -> color.copy(alpha = 0.09f) ; else -> Color.Transparent },
        tween(80), label = "pillBg"
    )
    val border by animateColorAsState(
        if (selected) color.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        tween(80), label = "pillBorder"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(7.dp))
            .pointerInput(label, selected, onClick) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap   = { onClick() }
                )
            }
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            ),
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ModifierButton(
    label: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        targetValue = when {
            selected -> color.copy(alpha = 0.22f)
            pressed -> color.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        animationSpec = tween(80),
        label = "modBtnBg"
    )
    val borderCol by animateColorAsState(
        targetValue = if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        animationSpec = tween(80),
        label = "modBtnBorder"
    )
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(10.dp))
            .pointerInput(label, selected, onClick) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap   = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            ),
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

