package com.btremote.app.ui.screens.airmouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btremote.app.bluetooth.HidServiceController
import com.btremote.app.bluetooth.MouseButtonMask
import com.btremote.app.data.AppPreferences
import com.btremote.app.data.PreferencesRepository
import com.btremote.app.sensor.AirMouseController
import com.btremote.app.sensor.AirMouseSensor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    private val airMouseController: AirMouseController,
    private val prefs: PreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<AppPreferences?> = prefs.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Sensor availability for UI display
    private val _sensorAvailable = MutableStateFlow(sensor.isAvailable())
    val sensorAvailable: StateFlow<Boolean> = _sensorAvailable.asStateFlow()

    /** Whether the game rotation vector sensor is available on this device */
    val isGameModeAvailable: Boolean get() = airMouseController.isGameModeAvailable

    /**
     * Live tilt from the global AirMouseController.
     * The crosshair in AirMouseScreen reads from here — the actual mouse
     * movement is already being sent globally so no need to duplicate it.
     */
    val tilt: StateFlow<Pair<Float, Float>> = airMouseController.tilt

    fun calibrate() {
        airMouseController.calibrate()
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
    }

    fun setSensitivity(value: Int) {
        viewModelScope.launch { prefs.setAirMouseSensitivity(value) }
    }

    fun setInvert(value: Boolean) {
        viewModelScope.launch { prefs.setAirMouseInvert(value) }
    }

    fun setGameMode(value: Boolean) {
        viewModelScope.launch { prefs.setAirMouseGameMode(value) }
    }
}
