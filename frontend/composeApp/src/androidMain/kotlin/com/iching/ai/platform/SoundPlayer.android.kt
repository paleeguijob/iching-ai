package com.iching.ai.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.iching.ai.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

private class AndroidSoundPlayer(private val context: Context) : SoundPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val loaded = mutableMapOf<ZenSound, Int>()
    private val ready = mutableSetOf<Int>()

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) synchronized(ready) { ready += id } }
        ZenSound.entries.forEach { sound ->
            scope.launch {
                runCatching {
                    // SoundPool needs a file path; copy the bundled resource to cache once.
                    val file = File(context.cacheDir, sound.resourcePath.substringAfterLast('/'))
                    if (!file.exists()) file.writeBytes(Res.readBytes(sound.resourcePath))
                    synchronized(loaded) { loaded[sound] = pool.load(file.absolutePath, 1) }
                }
            }
        }
    }

    override fun play(sound: ZenSound) {
        val id = synchronized(loaded) { loaded[sound] } ?: return
        if (synchronized(ready) { id in ready }) pool.play(id, 0.9f, 0.9f, 1, 0, 1f)
    }

    override fun release() {
        scope.cancel()
        pool.release()
    }
}

@Composable
actual fun rememberSoundPlayer(): SoundPlayer {
    val context = LocalContext.current.applicationContext
    val player = remember(context) { AndroidSoundPlayer(context) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
