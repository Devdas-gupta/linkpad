package com.btremote.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btremote.app.data.AppPreferences
import com.btremote.app.data.HostProfile
import com.btremote.app.data.HostProfilesState

private const val GITHUB_URL = "https://github.com/Devdas-gupta/linkpad"
private const val DEVELOPER_NAME = "Devdas Kumar"

private val tabs = listOf(
    "Controls" to Icons.Outlined.Tune,
    "Mouse" to Icons.Outlined.Mouse,
    "Keyboard" to Icons.Outlined.Keyboard,
    "Profiles" to Icons.Outlined.Devices,
    "Display" to Icons.Outlined.Brightness6,
    "About" to Icons.Outlined.Info
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val profilesState by viewModel.profilesState.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        ScrollableTabRow(
            selectedTabIndex = selected,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { idx, (label, icon) ->
                Tab(
                    selected = selected == idx,
                    onClick = { selected = idx },
                    text = { Text(label, style = MaterialTheme.typography.labelLarge) },
                    icon = { Icon(icon, label, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        val current = prefs
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally { -it } + fadeOut(tween(180)))
                    } else {
                        (slideInHorizontally { -it } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally { it } + fadeOut(tween(180)))
                    }
                },
                label = "settingsTabContent"
            ) { sel ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp, bottom = 24.dp)
                ) {
                    when (sel) {
                        0 -> if (current != null) ControlsTab(current, viewModel)
                        1 -> if (current != null) MouseTab(current, viewModel)
                        2 -> if (current != null) KeyboardTab(current, viewModel)
                        3 -> ProfilesTab(profilesState, viewModel)
                        4 -> if (current != null) DisplayTab(current, viewModel)
                        5 -> AboutTab()
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlsTab(p: AppPreferences, vm: SettingsViewModel) {
    SectionLabel("VISIBLE CONTROLS")
    SettingRow(icon = Icons.Outlined.Tune, title = "Show media controls",
        trailing = { Toggle(p.showMediaButtons, vm::setShowMediaButtons) })
    DividerThin()
    SettingRow(icon = Icons.Outlined.Tune, title = "Show Windows shortcuts",
        trailing = { Toggle(p.showShortcutsWin, vm::setShowShortcutsWin) })
    DividerThin()
    SettingRow(icon = Icons.Outlined.Tune, title = "Show macOS shortcuts",
        trailing = { Toggle(p.showShortcutsMac, vm::setShowShortcutsMac) })
    DividerThin()
    SettingRow(icon = Icons.Outlined.Tune, title = "Show Android nav buttons",
        subtitle = "Back, Home, Recents row in Touchpad tab",
        trailing = { Toggle(p.showAndroidNavButtons, vm::setShowAndroidNavButtons) })

    SectionLabel("VOLUME BUTTONS")
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SegmentedRow(
            options = listOf("System" to "system", "Remote" to "remote", "Off" to "disabled"),
            selectedValue = p.volumeButtonAction,
            onSelect = vm::setVolumeButtonAction
        )
    }
}

@Composable
private fun MouseTab(p: AppPreferences, vm: SettingsViewModel) {
    SectionLabel("POINTER")
    SliderSetting(icon = Icons.Outlined.Mouse, title = "Pointer speed",
        value = p.pointerSpeed, range = 1..20, onChange = vm::setPointerSpeed)
    DividerThin()
    SliderSetting(icon = Icons.Outlined.Mouse, title = "Scroll speed",
        value = p.scrollSpeed, range = 1..10, onChange = vm::setScrollSpeed)

    SectionLabel("BUTTON LAYOUT")
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SegmentedRow(
            options = listOf("Left+Right" to "left_right", "L+M+R" to "left_middle_right"),
            selectedValue = p.mouseButtonLayout,
            onSelect = vm::setMouseButtonLayout
        )
    }
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SegmentedRow(
            options = listOf("Top" to "top", "Bottom" to "bottom"),
            selectedValue = p.mouseButtonsPosition,
            onSelect = vm::setMouseButtonsPosition
        )
    }

    SectionLabel("SCROLL STRIP SIDE")
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SegmentedRow(
            options = listOf("Left" to "left", "Right" to "right"),
            selectedValue = p.scrollBarPosition,
            onSelect = vm::setScrollBarPosition
        )
    }

    SectionLabel("AIR MOUSE")
    SliderSetting(icon = Icons.Outlined.Mouse, title = "Sensitivity",
        value = p.airMouseSensitivity, range = 1..20, onChange = vm::setAirMouseSensitivity)
    DividerThin()
    SettingRow(icon = Icons.Outlined.Mouse, title = "Air mouse enabled",
        subtitle = "Use phone tilt to move pointer",
        trailing = { Toggle(p.airMouseEnabled, vm::setAirMouseEnabled) })
    DividerThin()
    SettingRow(icon = Icons.Outlined.Mouse, title = "Reverse direction",
        subtitle = "Invert tilt for natural feel",
        trailing = { Toggle(p.airMouseInvert, vm::setAirMouseInvert) })

    SectionLabel("AIR MOUSE BUTTONS")
    SettingRow(icon = Icons.Outlined.Mouse, title = "Show LEFT click",
        trailing = { Toggle(p.airMouseShowLeft, vm::setAirMouseShowLeft) })
    DividerThin()
    SettingRow(icon = Icons.Outlined.Mouse, title = "Show RIGHT click",
        trailing = { Toggle(p.airMouseShowRight, vm::setAirMouseShowRight) })
    DividerThin()
    SettingRow(icon = Icons.Outlined.Mouse, title = "Show MIDDLE click",
        trailing = { Toggle(p.airMouseShowMiddle, vm::setAirMouseShowMiddle) })
    DividerThin()
    SettingRow(icon = Icons.Outlined.Mouse, title = "Show RESET",
        trailing = { Toggle(p.airMouseShowReset, vm::setAirMouseShowReset) })
}

@Composable
private fun KeyboardTab(p: AppPreferences, vm: SettingsViewModel) {
    SectionLabel("DEFAULT TARGET OS")
    Text(
        "Used when active profile is set to Auto",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp)
    )
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SegmentedRow(
            options = listOf("Auto" to "auto", "Mac" to "mac", "Win" to "windows", "Linux" to "linux"),
            selectedValue = p.targetOs,
            onSelect = vm::setTargetOs
        )
    }

    SectionLabel("INPUT")
    SettingRow(icon = Icons.Outlined.Keyboard, title = "Direct input mode",
        subtitle = "Send physical keys as HID",
        trailing = { Toggle(p.directInputMode, vm::setDirectInputMode) })

    SectionLabel("KEYBOARD SECTIONS")
    SettingRow(icon = Icons.Outlined.Keyboard, title = "Custom shortcuts",
        subtitle = "My Shortcuts row",
        trailing = { Toggle(p.showCustomShortcuts, vm::setShowCustomShortcuts) })
    DividerThin()
    SettingRow(icon = Icons.Outlined.Keyboard, title = "Function keys",
        subtitle = "F1 through F12",
        trailing = { Toggle(p.showFKeys, vm::setShowFKeys) })
    DividerThin()
    SettingRow(icon = Icons.Outlined.Keyboard, title = "Edit keys",
        subtitle = "Backspace, Del, Home, End, PgUp, PgDn, Insert",
        trailing = { Toggle(p.showEdit, vm::setShowEdit) })
    DividerThin()
    SettingRow(icon = Icons.Outlined.Keyboard, title = "Navigation arrows",
        subtitle = "Arrow cluster",
        trailing = { Toggle(p.showArrows, vm::setShowArrows) })
}

@Composable
private fun ProfilesTab(state: HostProfilesState?, vm: SettingsViewModel) {
    SectionLabel("ACTIVE PROFILE")
    Text(
        "Switch profile to remember a different host (Mac / PC / iPad / TV). Each profile keeps its own target OS and last device for one-tap reconnect.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )

    val s = state ?: return
    s.profiles.forEach { profile ->
        ProfileRow(
            profile = profile,
            isActive = profile.id == s.activeId,
            canDelete = s.profiles.size > 1,
            onSelect = { vm.setActiveProfile(profile.id) },
            onRename = { vm.renameProfile(profile.id, it) },
            onSetOs = { vm.setProfileTargetOs(profile.id, it) },
            onDelete = { vm.removeProfile(profile.id) }
        )
        DividerThin()
    }

    AddProfileBlock(onAdd = { name, os -> vm.addProfile(name, os) })
}

@Composable
private fun ProfileRow(
    profile: HostProfile,
    isActive: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onRename: (String) -> Unit,
    onSetOs: (String) -> Unit,
    onDelete: () -> Unit
) {
    var editing by remember(profile.id) { mutableStateOf(false) }
    var nameDraft by remember(profile.id) { mutableStateOf(profile.name) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .padding(end = 0.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Spacer(Modifier.width(12.dp))
            if (editing) {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (nameDraft.isNotBlank()) onRename(nameDraft.trim())
                    editing = false
                }) {
                    Icon(Icons.Outlined.Add, "Save", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    val subtitle = buildString {
                        append("OS: ${profile.targetOs}")
                        if (profile.lastDeviceName.isNotBlank()) append("  ·  Last: ${profile.lastDeviceName}")
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!isActive) {
                    Button(
                        onClick = onSelect,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("USE", style = MaterialTheme.typography.labelLarge, color = Color.White) }
                } else {
                    Text("ACTIVE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { editing = true }) {
                    Icon(Icons.Outlined.Tune, "Rename", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SegmentedRow(
            options = listOf("Auto" to "auto", "Mac" to "mac", "Win" to "windows", "Linux" to "linux"),
            selectedValue = profile.targetOs,
            onSelect = onSetOs
        )
    }
}

@Composable
private fun AddProfileBlock(onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var os by remember { mutableStateOf("auto") }

    SectionLabel("ADD PROFILE")
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            label = { Text("Profile name (e.g. Living Room TV)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        SegmentedRow(
            options = listOf("Auto" to "auto", "Mac" to "mac", "Win" to "windows", "Linux" to "linux"),
            selectedValue = os,
            onSelect = { os = it }
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onAdd(name.trim(), os)
                    name = ""
                    os = "auto"
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Outlined.Add, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("ADD PROFILE", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

@Composable
private fun DisplayTab(p: AppPreferences, vm: SettingsViewModel) {
    SectionLabel("THEME")
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SegmentedRow(
            options = listOf("Light" to "light", "Dark" to "dark", "System" to "system"),
            selectedValue = p.themeMode,
            onSelect = vm::setThemeMode
        )
    }

    SectionLabel("BEHAVIOUR")
    SettingRow(icon = Icons.Outlined.Brightness6, title = "Keep screen on",
        trailing = { Toggle(p.keepScreenOn, vm::setKeepScreenOn) })
    DividerThin()
    SettingRow(icon = Icons.Outlined.Brightness6, title = "Show over lock screen",
        trailing = { Toggle(p.showOverLockScreen, vm::setShowOverLockScreen) })
    DividerThin()
    SettingRow(icon = Icons.Outlined.Brightness6, title = "Touch vibrations",
        trailing = { Toggle(p.touchVibrations, vm::setTouchVibrations) })
    DividerThin()
    SettingRow(icon = Icons.Outlined.Brightness6, title = "Fullscreen mode",
        trailing = { Toggle(p.fullscreenMode, vm::setFullscreenMode) })
    DividerThin()
    SettingRow(
        icon = Icons.Outlined.Brightness6,
        title = "Run in background",
        subtitle = "Keep connected with a status bar notification",
        trailing = { Toggle(p.backgroundServiceNotification, vm::setBackgroundServiceNotification) }
    )
}

@Composable
private fun AboutTab() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(20.dp)
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "LINKPAD",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                // Show version number from PackageInfo
                val versionName = remember {
                    runCatching {
                        val info = context.packageManager.getPackageInfo(context.packageName, 0)
                        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                            info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
                        "v${info.versionName} (Build $code)"
                    }.getOrDefault("Unknown version")
                }
                Text(
                    versionName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Wireless keyboard, mouse and media remote over Bluetooth.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        SectionLabel("DEVELOPER")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(DEVELOPER_NAME, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Author and maintainer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SectionLabel("SOURCE")
        Text(
            "View the source on GitHub. Open issues for bugs in Touchpad, Home, Media or any other screen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 0.dp)
        )

        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            GitHubMark(tint = Color.White, size = 22.dp)
            Spacer(Modifier.width(10.dp))
            Text("VIEW ON GITHUB", style = MaterialTheme.typography.labelLarge, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.OpenInNew, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
private fun SliderSetting(
    icon: ImageVector,
    title: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    var localValue by remember(value) { androidx.compose.runtime.mutableFloatStateOf(value.toFloat()) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(
                "${localValue.toInt()}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = localValue,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onChange(localValue.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.padding(start = 32.dp)
        )
    }
}

@Composable
private fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.surface,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
private fun DividerThin() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 48.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedRow(
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelect: (String) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { idx, (label, value) ->
            SegmentedButton(
                selected = selectedValue == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(idx, options.size),
                label = { Text(label, style = MaterialTheme.typography.labelLarge) }
            )
        }
    }
}

private val GitHubMarkVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "GitHubMark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(12f, 0.297f)
            curveTo(5.373f, 0.297f, 0f, 5.67f, 0f, 12.297f)
            curveToRelative(0f, 5.302f, 3.438f, 9.8f, 8.205f, 11.387f)
            curveToRelative(0.6f, 0.111f, 0.82f, -0.26f, 0.82f, -0.577f)
            curveToRelative(0f, -0.286f, -0.011f, -1.231f, -0.017f, -2.234f)
            curveToRelative(-3.338f, 0.726f, -4.042f, -1.416f, -4.042f, -1.416f)
            curveToRelative(-0.546f, -1.387f, -1.333f, -1.756f, -1.333f, -1.756f)
            curveToRelative(-1.089f, -0.745f, 0.083f, -0.729f, 0.083f, -0.729f)
            curveToRelative(1.205f, 0.084f, 1.838f, 1.236f, 1.838f, 1.236f)
            curveToRelative(1.07f, 1.835f, 2.807f, 1.305f, 3.492f, 0.998f)
            curveToRelative(0.108f, -0.776f, 0.418f, -1.305f, 0.762f, -1.604f)
            curveToRelative(-2.665f, -0.303f, -5.466f, -1.332f, -5.466f, -5.93f)
            curveToRelative(0f, -1.31f, 0.469f, -2.381f, 1.236f, -3.221f)
            curveToRelative(-0.124f, -0.303f, -0.535f, -1.524f, 0.117f, -3.176f)
            curveToRelative(0f, 0f, 1.008f, -0.322f, 3.301f, 1.23f)
            curveToRelative(0.957f, -0.266f, 1.983f, -0.399f, 3.003f, -0.404f)
            curveToRelative(1.02f, 0.005f, 2.047f, 0.138f, 3.006f, 0.404f)
            curveToRelative(2.291f, -1.552f, 3.297f, -1.23f, 3.297f, -1.23f)
            curveToRelative(0.653f, 1.653f, 0.242f, 2.874f, 0.118f, 3.176f)
            curveToRelative(0.77f, 0.84f, 1.235f, 1.911f, 1.235f, 3.221f)
            curveToRelative(0f, 4.609f, -2.806f, 5.624f, -5.479f, 5.921f)
            curveToRelative(0.43f, 0.372f, 0.814f, 1.103f, 0.814f, 2.222f)
            curveToRelative(0f, 1.606f, -0.014f, 2.898f, -0.014f, 3.293f)
            curveToRelative(0f, 0.319f, 0.216f, 0.694f, 0.825f, 0.576f)
            curveToRelative(4.765f, -1.589f, 8.199f, -6.086f, 8.199f, -11.386f)
            curveToRelative(0f, -6.627f, -5.373f, -12f, -12f, -12f)
            close()
        }
    }.build()
}

@Composable
private fun GitHubMark(tint: Color, size: androidx.compose.ui.unit.Dp) {
    Icon(
        imageVector = GitHubMarkVector,
        contentDescription = "GitHub",
        tint = tint,
        modifier = Modifier.size(size)
    )
}
