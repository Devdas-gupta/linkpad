package com.btremote.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class AirMouseSensor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyro: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    fun isAvailable(): Boolean = gyro != null

    /**
     * Emits (dx, dy) pixel deltas derived from filtered gyroscope angular velocity.
     * sensitivity range 1..20 (default 8). Higher = faster cursor.
     * invert flips both axes for "reverse mouse" mode.
     */
    fun deltas(sensitivity: Int, invert: Boolean = false): Flow<Pair<Float, Float>> = callbackFlow {
        val sm = sensorManager
        val sensor = gyro
        if (sm == null || sensor == null) {
            close()
            return@callbackFlow
        }

        val alpha = 0.8f
        var prevX = 0f
        var prevY = 0f
        var initialized = false
        val multiplier = sensitivity.coerceIn(1, 20).toFloat() * 4f
        val sign = if (invert) 1f else -1f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return
                val rawX = event.values[1]
                val rawY = event.values[0]
                if (!initialized) {
                    prevX = rawX
                    prevY = rawY
                    initialized = true
                }
                val fx = alpha * rawX + (1 - alpha) * prevX
                val fy = alpha * rawY + (1 - alpha) * prevY
                prevX = fx
                prevY = fy

                val dx = sign * fx * multiplier
                val dy = sign * fy * multiplier
                trySend(dx to dy)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sm.unregisterListener(listener) }
    }
}
