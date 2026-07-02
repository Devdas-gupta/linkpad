package com.btremote.app.sensor

import com.btremote.app.bluetooth.HidReportSender
import com.btremote.app.data.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application-scoped air mouse controller (Singleton).
 *
 * Keeps the gyro sensor running regardless of which screen is active,
 * so the user can use air mouse tilt-to-cursor while on the keyboard,
 * touchpad, or any other screen — not just the Air Mouse tab.
 *
 * Started once from HidServiceController and lives for the whole app lifetime.
 */
@Singleton
class AirMouseController @Inject constructor(
    private val sensor: AirMouseSensor,
    private val reportSender: HidReportSender,
    private val prefs: PreferencesRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sensorJob: Job? = null
    private var prefWatchJob: Job? = null

    private val _tilt = MutableStateFlow(0f to 0f)
    /** Live (dx, dy) tilt for the crosshair display in AirMouseScreen. */
    val tilt: StateFlow<Pair<Float, Float>> = _tilt.asStateFlow()

    val isGameModeAvailable: Boolean get() = sensor.isGameModeAvailable()
    val isSensorAvailable: Boolean get() = sensor.isAvailable()

    /**
     * Call once at app start. Watches prefs and automatically
     * starts/stops/reconfigures the gyro as preferences change.
     */
    fun startWatching() {
        if (prefWatchJob?.isActive == true) return
        prefWatchJob = scope.launch {
            prefs.preferences.collect { p ->
                if (p.airMouseEnabled && sensor.isAvailable()) {
                    startSensor(p.airMouseSensitivity, p.airMouseInvert, p.airMouseGameMode)
                } else {
                    stopSensor()
                }
            }
        }
    }

    private fun startSensor(sensitivity: Int, invert: Boolean, gameMode: Boolean) {
        sensorJob?.cancel()
        _tilt.value = 0f to 0f
        sensorJob = scope.launch {
            val flow = if (gameMode && sensor.isGameModeAvailable()) {
                sensor.gameDeltas(sensitivity, invert)
            } else {
                sensor.deltas(sensitivity, invert)
            }
            flow.collect { (dx, dy) ->
                _tilt.value = dx to dy
                val sender = reportSender
                if (sender.isReady()) {
                    sender.queueMouseMove(dx.toInt(), dy.toInt())
                }
            }
        }
    }

    private fun stopSensor() {
        sensorJob?.cancel()
        sensorJob = null
        _tilt.value = 0f to 0f
    }

    /** Re-zero the tilt origin (gyro bias calibration). */
    fun calibrate() {
        scope.launch {
            stopSensor()
            val p = prefs.preferences.first()
            if (p.airMouseEnabled && sensor.isAvailable()) {
                startSensor(p.airMouseSensitivity, p.airMouseInvert, p.airMouseGameMode)
            }
        }
    }
}
