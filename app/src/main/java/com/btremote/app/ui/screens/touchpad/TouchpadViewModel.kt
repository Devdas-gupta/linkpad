package com.btremote.app.ui.screens.touchpad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btremote.app.bluetooth.HidServiceController
import com.btremote.app.bluetooth.MouseButtonMask
import com.btremote.app.data.AppPreferences
import com.btremote.app.data.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TouchpadViewModel @Inject constructor(
    private val controller: HidServiceController,
    prefs: PreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<AppPreferences?> = prefs.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // BUG 8 — Guard move() with isReady() to match buttonPress()/tapClick() behavior
    fun move(dx: Int, dy: Int) {
        if (!controller.reportSender.isReady()) return
        controller.reportSender.queueMouseMove(dx, dy)
    }

    fun scroll(delta: Int) {
        controller.reportSender.queueScroll(delta)
    }

    fun hScroll(delta: Int) {
        controller.reportSender.queueHScroll(delta)
    }

    fun buttonPress(mask: MouseButtonMask, down: Boolean) {
        val sender = controller.reportSender
        if (!sender.isReady()) return
        viewModelScope.launch { sender.sendMouseClick(mask.mask, down) }
    }

    fun tapClick(mask: MouseButtonMask) {
        val sender = controller.reportSender
        if (!sender.isReady()) return
        viewModelScope.launch { sender.tapMouseClick(mask.mask) }
    }

    fun sendConsumer(usage: com.btremote.app.bluetooth.ConsumerUsage) {
        val sender = controller.reportSender
        if (!sender.isReady()) return
        viewModelScope.launch { sender.sendConsumerKey(usage) }
    }

    /** Sends Super+Tab — standard HID mapping for app-switcher / Recents. */
    fun recentApps() {
        val sender = controller.reportSender
        if (!sender.isReady()) return
        viewModelScope.launch {
            sender.pressAndRelease(
                com.btremote.app.bluetooth.HidKeyCode.TAB,
                com.btremote.app.bluetooth.MODIFIER_LEFT_GUI
            )
        }
    }
}
