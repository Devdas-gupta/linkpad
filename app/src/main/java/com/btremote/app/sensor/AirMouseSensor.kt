package com.btremote.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sign
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

    // Game Rotation Vector — fused gyro+accel, no magnetic interference, no drift
    private val gameRotVec: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    fun isAvailable(): Boolean = gyro != null || gameRotVec != null
    fun isGameModeAvailable(): Boolean = gameRotVec != null

    /**
     * Classic gyroscope mode — raw angular velocity with low-pass filter.
     * sensitivity range 1..20 (default 8). Higher = faster cursor.
     * invert flips both axes.
     * deadZone suppresses micro-tremors (0.0 = off, 0.05 = recommended).
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
        val deadZone = 0.05f  // rad/s — ignore micro-tremors

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return
                val rawX = event.values[1]
                val rawY = event.values[0]
                if (!initialized) {
                    prevX = rawX; prevY = rawY; initialized = true
                }
                val fx = alpha * rawX + (1 - alpha) * prevX
                val fy = alpha * rawY + (1 - alpha) * prevY
                prevX = fx; prevY = fy

                // Dead zone — suppress jitter when holding still
                val ax = if (abs(fx) < deadZone) 0f else fx
                val ay = if (abs(fy) < deadZone) 0f else fy

                // Non-linear (square-root boost) sensitivity curve:
                // slow = precise, fast = quick
                fun curve(v: Float): Float {
                    val a = abs(v)
                    return sign(v) * (a * multiplier + a * a * multiplier * 0.15f)
                }

                val dx = sign * curve(ax)
                val dy = sign * curve(ay)
                if (dx != 0f || dy != 0f) trySend(dx to dy)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sm.unregisterListener(listener) }
    }

    /**
     * Game-style gyro mode — uses TYPE_GAME_ROTATION_VECTOR (sensor fusion).
     * Derives yaw/pitch from quaternion orientation deltas → zero drift over time.
     * This is the same technique used in FPS gaming controllers.
     * sensitivity range 1..20. invert flips axes.
     */
    fun gameDeltas(sensitivity: Int, invert: Boolean = false): Flow<Pair<Float, Float>> = callbackFlow {
        val sm = sensorManager
        val sensor = gameRotVec
        if (sm == null || sensor == null) {
            // Fallback to classic gyro if game rotation vector unavailable
            deltas(sensitivity, invert).collect { trySend(it) }
            close()
            return@callbackFlow
        }

        val multiplier = sensitivity.coerceIn(1, 20).toFloat() * 60f
        val sign = if (invert) 1f else -1f
        val deadZone = 0.002f // quaternion delta dead zone
        val rotMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var prevYaw = Float.NaN
        var prevPitch = Float.NaN

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                if (event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR) return

                SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                SensorManager.getOrientation(rotMatrix, orientation)

                val yaw   = orientation[0]   // azimuth (horizontal tilt)
                val pitch = orientation[1]   // pitch (vertical tilt)

                if (prevYaw.isNaN() || prevPitch.isNaN()) {
                    prevYaw = yaw; prevPitch = pitch
                    return
                }

                // Compute angular deltas (handle wrap-around at ±π)
                var dYaw   = yaw - prevYaw
                var dPitch = pitch - prevPitch

                if (dYaw > Math.PI)       dYaw   -= (2 * Math.PI).toFloat()
                if (dYaw < -Math.PI)      dYaw   += (2 * Math.PI).toFloat()
                if (dPitch > Math.PI)     dPitch -= (2 * Math.PI).toFloat()
                if (dPitch < -Math.PI)    dPitch += (2 * Math.PI).toFloat()

                prevYaw   = yaw
                prevPitch = pitch

                // Dead zone to suppress sensor noise when holding still
                val ax = if (abs(dYaw)   < deadZone) 0f else dYaw
                val ay = if (abs(dPitch) < deadZone) 0f else dPitch

                // Non-linear curve for precision at slow speed
                fun curve(v: Float): Float {
                    val a = abs(v)
                    return sign(v) * (a * multiplier + a * a * multiplier * 0.3f)
                }

                val dx = sign * curve(ax)
                val dy = sign * curve(ay)
                if (dx != 0f || dy != 0f) trySend(dx to dy)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sm.unregisterListener(listener) }
    }
}
