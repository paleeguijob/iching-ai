package com.iching.ai.platform

import androidx.compose.runtime.Composable

enum class ZenSound(val resourcePath: String) {
    COIN_TOSS("files/coin_toss.wav"),
    WIND_CHIME("files/wind_chime.wav"),
}

/** Plays short bundled sound effects; silently does nothing if audio is unavailable. */
interface SoundPlayer {
    fun play(sound: ZenSound)
    fun release()
}

/** Remembers a platform player for the current composition and releases it on dispose. */
@Composable
expect fun rememberSoundPlayer(): SoundPlayer
