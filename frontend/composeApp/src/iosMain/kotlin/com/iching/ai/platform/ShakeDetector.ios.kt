package com.iching.ai.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Not wired to CoreMotion yet (RELEASE.md section 9); on iOS the cylinder shakes while held. */
private object NoShakeDetector : ShakeDetector {
    override fun start(onSample: (magnitudeG: Float) -> Unit) = Unit
    override fun stop() = Unit
}

@Composable
actual fun rememberShakeDetector(): ShakeDetector = remember { NoShakeDetector }
