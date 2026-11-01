package com.iching.ai.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

/** PREPARING covers the wait between asking to speak and the first sound. */
enum class SpeechState { IDLE, PREPARING, SPEAKING }

/** Reads Thai text aloud with the device's own speech engine, so it costs nothing per use. */
interface SpeechPlayer {
    val state: StateFlow<SpeechState>

    /** Starts the engine ahead of time, so that [speak] doesn't have to wait for it. Failures stay silent until then. */
    fun prepare()

    /** [onUnavailable] runs when the device has no Thai voice or no working engine. */
    fun speak(text: String, onUnavailable: () -> Unit)
    fun stop()
    fun openVoiceSettings()
    fun release()
}

/** Remembers a platform player for the current composition and releases it on dispose. */
@Composable
expect fun rememberSpeechPlayer(): SpeechPlayer
