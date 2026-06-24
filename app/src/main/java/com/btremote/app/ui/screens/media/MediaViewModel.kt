package com.btremote.app.ui.screens.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btremote.app.bluetooth.ConsumerUsage
import com.btremote.app.bluetooth.HidKeyCode
import com.btremote.app.bluetooth.HidServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val controller: HidServiceController
) : ViewModel() {

    private var seekJob: Job? = null

    fun send(usage: ConsumerUsage) {
        val sender = controller.reportSender
        if (!sender.isReady()) return
        viewModelScope.launch { sender.sendConsumerKey(usage) }
    }

    /**
     * Start holding the seek-forward arrow key at ~100 ms repeat rate.
     * Call seekStop() on pointer release.
     */
    fun seekForwardStart() {
        seekStop()
        val sender = controller.reportSender
        if (!sender.isReady()) return
        seekJob = viewModelScope.launch {
            while (true) {
                sender.pressAndRelease(HidKeyCode.RIGHT_ARROW, holdMs = 60L)
                delay(40L)
            }
        }
    }

    /**
     * Start holding the seek-backward arrow key at ~100 ms repeat rate.
     * Call seekStop() on pointer release.
     */
    fun seekBackwardStart() {
        seekStop()
        val sender = controller.reportSender
        if (!sender.isReady()) return
        seekJob = viewModelScope.launch {
            while (true) {
                sender.pressAndRelease(HidKeyCode.LEFT_ARROW, holdMs = 60L)
                delay(40L)
            }
        }
    }

    /** Stop any ongoing seek. Call on pointer up. */
    fun seekStop() {
        seekJob?.cancel()
        seekJob = null
    }

    /** Single-click seek forward (sends right arrow) */
    fun seekForward() {
        val sender = controller.reportSender
        if (!sender.isReady()) return
        viewModelScope.launch {
            sender.pressAndRelease(HidKeyCode.RIGHT_ARROW)
        }
    }

    /** Single-click seek backward (sends left arrow) */
    fun seekBackward() {
        val sender = controller.reportSender
        if (!sender.isReady()) return
        viewModelScope.launch {
            sender.pressAndRelease(HidKeyCode.LEFT_ARROW)
        }
    }

    override fun onCleared() {
        super.onCleared()
        seekStop()
    }
}
