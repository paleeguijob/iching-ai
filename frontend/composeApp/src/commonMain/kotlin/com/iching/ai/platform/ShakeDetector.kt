package com.iching.ai.platform

import androidx.compose.runtime.Composable

/**
 * Adds up shaking from accelerometer samples (or from a held press) and says when there has been enough.
 * Samples at or above [thresholdG] count; a pause of more than [maxGapMillis] starts the count over.
 */
class ShakeAccumulator(
    private val thresholdG: Float = 1.6f,
    private val needMillis: Long = 1_500,
    private val maxGapMillis: Long = 600,
) {
    private var startedAt: Long? = null
    private var lastShakeAt: Long = 0
    private var fired = false

    /** True exactly once, on the sample that completes [needMillis] of shaking. */
    fun onSample(magnitudeG: Float, atMillis: Long): Boolean {
        if (fired || magnitudeG < thresholdG) return false
        val start = startedAt
        if (start == null || atMillis - lastShakeAt > maxGapMillis) startedAt = atMillis
        lastShakeAt = atMillis
        if (atMillis - startedAt!! < needMillis) return false
        fired = true
        return true
    }

    /** Ready for the next shake, e.g. after a failed pua. */
    fun reset() {
        startedAt = null
        fired = false
    }
}

/** Reports accelerometer magnitude in g while started. Platforms without a sensor never call back. */
interface ShakeDetector {
    fun start(onSample: (magnitudeG: Float) -> Unit)
    fun stop()
}

/** Remembers a platform detector for the current composition and stops it on dispose. */
@Composable
expect fun rememberShakeDetector(): ShakeDetector
