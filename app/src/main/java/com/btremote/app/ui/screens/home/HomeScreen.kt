package com.btremote.app.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btremote.app.ui.components.ConnectionBar
import com.btremote.app.ui.screens.airmouse.AirMouseScreen
import com.btremote.app.ui.screens.connect.ConnectScreen
import com.btremote.app.ui.screens.keyboard.KeyboardScreen
import com.btremote.app.ui.screens.media.MediaScreen
import com.btremote.app.ui.screens.touchpad.TouchpadScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    Touchpad("Touchpad", Icons.Outlined.Mouse),
    Keyboard("Keyboard", Icons.Outlined.Keyboard),
    AirMouse("Air mouse", Icons.Outlined.Navigation),
    Media("Media", Icons.Outlined.PlayCircleOutline),
    Connect("Connect", Icons.Outlined.Bluetooth)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    val quick by viewModel.quickPairTarget.collectAsStateWithLifecycle()
    var current by remember { mutableStateOf(Tab.Touchpad) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "LINKPAD",
                        style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 3.2.sp)
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            PremiumNavBar(current = current, onSelect = { current = it })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ConnectionBar(
                state = state,
                onClick = { current = Tab.Connect },
                quickPairLabel = quick?.name,
                onQuickPair = if (quick != null) { { viewModel.quickPair() } } else null
            )
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = current,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "tabContent"
                ) { tab ->
                    when (tab) {
                        Tab.Touchpad -> TouchpadScreen()
                        Tab.Keyboard -> KeyboardScreen()
                        Tab.AirMouse -> AirMouseScreen()
                        Tab.Media -> MediaScreen()
                        Tab.Connect -> ConnectScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumNavBar(current: Tab, onSelect: (Tab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tab.entries.forEach { t ->
            NavItem(tab = t, selected = current == t, onClick = { onSelect(t) })
        }
    }
}

@Composable
private fun NavItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .pointerInput(tab) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = if (selected) 14.dp else 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(tab.icon, tab.label, tint = tint, modifier = Modifier.size(20.dp))
        if (selected) {
            Spacer(Modifier.size(6.dp))
            Text(
                tab.label,
                color = tint,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
