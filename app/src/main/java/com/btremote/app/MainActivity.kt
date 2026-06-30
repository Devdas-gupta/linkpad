package com.btremote.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.btremote.app.bluetooth.ConnectionState
import com.btremote.app.bluetooth.ConsumerUsage
import com.btremote.app.bluetooth.HidServiceController
import com.btremote.app.data.PreferencesRepository
import com.btremote.app.ui.navigation.BTRemoteNavGraph
import com.btremote.app.ui.theme.BTRemoteTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var controller: HidServiceController
    @Inject lateinit var preferencesRepository: PreferencesRepository

    @Volatile
    private var serviceStarted: Boolean = false

    // BUG 19 — Cache volumeButtonAction as a StateFlow so handleVolumeKey reads synchronously
    private lateinit var volumeActionFlow: StateFlow<String>

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.BLUETOOTH_CONNECT] == true) {
            startServiceOnce()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // BUG 19 — Initialize the StateFlow before any UI setup
        volumeActionFlow = preferencesRepository.preferences
            .map { it.volumeButtonAction }
            .stateIn(lifecycleScope, SharingStarted.Eagerly, "remote")
        ensurePermissionsAndStart()

        setContent {
            val prefs by preferencesRepository.preferences.collectAsState(initial = null)
            val themeMode = prefs?.themeMode ?: "system"

            LaunchedEffect(prefs?.keepScreenOn) {
                if (prefs?.keepScreenOn == true) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            LaunchedEffect(prefs?.showOverLockScreen) {
                applyOverLockScreen(prefs?.showOverLockScreen ?: false)
            }
            LaunchedEffect(prefs?.fullscreenMode) {
                applyFullscreen(prefs?.fullscreenMode ?: false)
            }

            val onboardingDone by preferencesRepository.preferences
                .map { it.onboardingComplete }
                .collectAsState(initial = null)

            BTRemoteTheme(themeMode = themeMode) {
                val start = when (onboardingDone) {
                    false -> com.btremote.app.ui.navigation.Routes.ONBOARDING
                    true  -> com.btremote.app.ui.navigation.Routes.HOME
                    null  -> null
                }
                if (start != null) {
                    androidx.compose.runtime.key(start) {
                        BTRemoteNavGraph(startDestination = start)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!serviceStarted && hasBluetoothPermissions()) {
            startServiceOnce()
        }
    }

    private fun ensurePermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (hasBluetoothPermissions()) {
                startServiceOnce()
            } else {
                // BUG 14 — BLUETOOTH_ADVERTISE removed: unrelated to HID scanning
                val toRequest = mutableListOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
                permLauncher.launch(toRequest.toTypedArray())
            }
        } else {
            startServiceOnce()
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val connect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        return connect == PackageManager.PERMISSION_GRANTED
    }

    private fun startServiceOnce() {
        if (serviceStarted) return
        serviceStarted = true
        lifecycleScope.launch {
            val bgRun = preferencesRepository.preferences.first().backgroundServiceNotification
            controller.start(bgRun)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && serviceStarted) {
            controller.stop()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (handleVolumeKey(keyCode, true)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (controller.connectionState.value is ConnectionState.Connected) {
                val action = lastVolumeAction
                if (action != "system") return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    @Volatile
    private var lastVolumeAction: String = "remote"

    private fun handleVolumeKey(keyCode: Int, isDown: Boolean): Boolean {
        val isVolume = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!isVolume) return false

        val state = controller.connectionState.value
        if (state !is ConnectionState.Connected) return false

        // BUG 19 — Read volumeButtonAction synchronously from cached StateFlow (no async race)
        val action = volumeActionFlow.value
        lastVolumeAction = action
        if (isDown) {
            when (action) {
                "remote" -> {
                    val usage = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) ConsumerUsage.VOLUME_UP else ConsumerUsage.VOLUME_DOWN
                    lifecycleScope.launch { controller.reportSender.sendConsumerKey(usage) }
                }
                "disabled" -> { }
                else -> { }
            }
        }
        return action != "system"
    }

    private fun applyOverLockScreen(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(enable)
            setTurnScreenOn(enable)
        } else {
            @Suppress("DEPRECATION")
            if (enable) {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            } else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        }
    }

    private fun applyFullscreen(enable: Boolean) {
        @Suppress("DEPRECATION")
        if (enable) {
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        } else {
            window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
}
