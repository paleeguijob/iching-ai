package com.iching.ai.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt

private class AndroidShakeDetector(context: Context) : ShakeDetector {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var listener: SensorEventListener? = null

    override fun start(onSample: (magnitudeG: Float) -> Unit) {
        stop()
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                onSample(sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = l
        manager.registerListener(l, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun stop() {
        listener?.let { manager?.unregisterListener(it) }
        listener = null
    }
}

@Composable
actual fun rememberShakeDetector(): ShakeDetector {
    val context = LocalContext.current
    val detector = remember(context) { AndroidShakeDetector(context) }
    DisposableEffect(detector) { onDispose { detector.stop() } }
    return detector
}
