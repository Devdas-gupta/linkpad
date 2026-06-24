package com.btremote.app.ui.screens.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btremote.app.bluetooth.ConsumerUsage
import com.btremote.app.bluetooth.HidKeyCode
import com.btremote.app.bluetooth.HidServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class TvRemoteViewModel @Inject constructor(
    private val controller: HidServiceController
) : ViewModel() {

    private fun sender() = controller.reportSender

    fun dpadUp()     = viewModelScope.launch { sender().pressAndRelease(HidKeyCode.UP_ARROW) }
    fun dpadDown()   = viewModelScope.launch { sender().pressAndRelease(HidKeyCode.DOWN_ARROW) }
    fun dpadLeft()   = viewModelScope.launch { sender().pressAndRelease(HidKeyCode.LEFT_ARROW) }
    fun dpadRight()  = viewModelScope.launch { sender().pressAndRelease(HidKeyCode.RIGHT_ARROW) }
    fun dpadOk()     = viewModelScope.launch { sender().pressAndRelease(HidKeyCode.ENTER) }
    fun back()       = viewModelScope.launch { sender().pressAndRelease(HidKeyCode.BACKSPACE) }
    fun home()       = viewModelScope.launch { sender().pressAndRelease(HidKeyCode.HOME) }
    fun menu()       = viewModelScope.launch { sender().pressAndRelease(HidKeyCode.ESCAPE) }
    fun volumeUp()   = viewModelScope.launch { sender().sendConsumerKey(ConsumerUsage.VOLUME_UP) }
    fun volumeDown() = viewModelScope.launch { sender().sendConsumerKey(ConsumerUsage.VOLUME_DOWN) }
    fun mute()       = viewModelScope.launch { sender().sendConsumerKey(ConsumerUsage.MUTE) }
    fun playPause()  = viewModelScope.launch { sender().sendConsumerKey(ConsumerUsage.PLAY_PAUSE) }
    fun nextTrack()  = viewModelScope.launch { sender().sendConsumerKey(ConsumerUsage.NEXT_TRACK) }
    fun prevTrack()  = viewModelScope.launch { sender().sendConsumerKey(ConsumerUsage.PREV_TRACK) }
    fun channelUp()  = viewModelScope.launch { sender().pressAndRelease(HidKeyCode.PAGE_UP) }
    fun channelDown()= viewModelScope.launch { sender().pressAndRelease(HidKeyCode.PAGE_DOWN) }

    // Smart TV colour buttons (standard keys mapped to HID)
    fun colorRed()    = viewModelScope.launch { sender().pressAndRelease(HidKeyCode.F1) }
    fun colorGreen()  = viewModelScope.launch { sender().pressAndRelease(HidKeyCode.F2) }
    fun colorYellow() = viewModelScope.launch { sender().pressAndRelease(HidKeyCode.F3) }
    fun colorBlue()   = viewModelScope.launch { sender().pressAndRelease(HidKeyCode.F4) }

    // Number keys
    fun numKey(n: Int) = viewModelScope.launch {
        val code = when (n) {
            1 -> HidKeyCode.NUM_1; 2 -> HidKeyCode.NUM_2; 3 -> HidKeyCode.NUM_3
            4 -> HidKeyCode.NUM_4; 5 -> HidKeyCode.NUM_5; 6 -> HidKeyCode.NUM_6
            7 -> HidKeyCode.NUM_7; 8 -> HidKeyCode.NUM_8; 9 -> HidKeyCode.NUM_9
            else -> HidKeyCode.NUM_0
        }
        sender().pressAndRelease(code)
    }

    fun isReady() = sender().isReady()

    /** HID Consumer Control: Power (0x030) */
    fun power() = viewModelScope.launch { sender().sendConsumerKey(ConsumerUsage.POWER) }

    /** HID Consumer Control: Input Select (0x060) */
    fun inputSource() = viewModelScope.launch { sender().sendConsumerKey(ConsumerUsage.INPUT_SELECT) }
}
