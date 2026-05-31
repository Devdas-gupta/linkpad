package com.btremote.app.ui.screens.airmouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btremote.app.bluetooth.HidServiceController
import com.btremote.app.bluetooth.MouseButtonMask
import com.btremote.app.data.AppPreferences
import com.btremote.app.data.PreferencesRepository
import com.btremote.app.sensor.AirMouseSensor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AirMouseViewModel @Inject constructor(
    private val controller: HidServiceController,
    private val sensor: AirMouseSensor,
    private val prefs: PreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<AppPreferences?> = prefs.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sensorAvailable: Boolean = sensor.isAvailable()

    private val _tilt = MutableStateFlow(0f to 0f)
    val tilt: StateFlow<Pair<Float, Float>> = _tilt.asStateFlow()

    private var job: Job? = null

    fun start(sensitivity: Int, invert: Boolean = false) {
        stop()
        if (!sensor.isAvailable()) return
        job = viewModelScope.launch {
            sensor.deltas(sensitivity, invert).collect { (dx, dy) ->
                _tilt.value = dx to dy
                val sender = controller.reportSender
                if (sender.isReady()) {
                    sender.queueMouseMove(dx.toInt(), dy.toInt())
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _tilt.value = 0f to 0f
    }

    fun calibrate() {
        _tilt.value = 0f to 0f
    }

    fun leftClick() {
        val sender = controller.reportSender
        if (!sender.isReady()) return
        viewModelScope.launch { sender.tapMouseClick(MouseButtonMask.LEFT.mask) }
    }

    fun rightClick() {
        val sender = controller.reportSender
        if (!sender.isReady()) return
        viewModelScope.launch { sender.tapMouseClick(MouseButtonMask.RIGHT.mask) }
    }

    fun middleClick() {
        val sender = controller.reportSender
        if (!sender.isReady()) return
        viewModelScope.launch { sender.tapMouseClick(MouseButtonMask.MIDDLE.mask) }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setAirMouseEnabled(enabled) }
        if (!enabled) stop()
    }

    fun setSensitivity(value: Int) {
        viewModelScope.launch { prefs.setAirMouseSensitivity(value) }
    }

    fun setInvert(value: Boolean) {
        viewModelScope.launch { prefs.setAirMouseInvert(value) }
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
