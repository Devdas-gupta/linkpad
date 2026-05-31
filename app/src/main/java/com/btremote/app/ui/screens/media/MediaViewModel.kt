package com.btremote.app.ui.screens.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btremote.app.bluetooth.ConsumerUsage
import com.btremote.app.bluetooth.HidKeyCode
import com.btremote.app.bluetooth.HidServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val controller: HidServiceController
) : ViewModel() {

    fun send(usage: ConsumerUsage) {
        val sender = controller.reportSender
        if (!sender.isReady()) return
        viewModelScope.launch { sender.sendConsumerKey(usage) }
    }

    /**
     * Skip 10s in most media apps (YouTube/Netflix/Spotify/VLC). Sends arrow key —
     * works cross-platform unlike consumer FF code which is host-app dependent.
     */
    fun seekForward() {
        val sender = controller.reportSender
        if (!sender.isReady()) return
        viewModelScope.launch { sender.pressAndRelease(HidKeyCode.RIGHT_ARROW) }
    }

    fun seekBackward() {
        val sender = controller.reportSender
        if (!sender.isReady()) return
        viewModelScope.launch { sender.pressAndRelease(HidKeyCode.LEFT_ARROW) }
    }
}
