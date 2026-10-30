package com.iching.ai.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.iching.ai.resources.Res
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
private class IosSoundPlayer : SoundPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val data = mutableMapOf<ZenSound, NSData>()
    // Keep players alive until they finish; AVAudioPlayer stops when deallocated.
    private val active = ArrayDeque<AVAudioPlayer>()

    init {
        // Ambient: mixes with other audio and respects the silent switch.
        runCatching { AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryAmbient, null) }
        ZenSound.entries.forEach { sound ->
            scope.launch { runCatching { data[sound] = Res.readBytes(sound.resourcePath).toNSData() } }
        }
    }

    override fun play(sound: ZenSound) {
        val bytes = data[sound] ?: return
        val player = AVAudioPlayer(data = bytes, error = null)
        player.volume = 0.9f
        player.prepareToPlay()
        player.play()
        active.addLast(player)
        while (active.size > 6) active.removeFirst()
    }

    override fun release() {
        scope.cancel()
        active.forEach { it.stop() }
        active.clear()
    }
}

@Composable
actual fun rememberSoundPlayer(): SoundPlayer {
    val player = remember { IosSoundPlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
