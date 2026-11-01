package com.iching.ai.platform

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.iching.ai.domain.oracle.splitForSpeech
import com.iching.ai.domain.oracle.stripMarkup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private class AndroidSpeechPlayer(private val context: Context) : SpeechPlayer {
    private val _speaking = MutableStateFlow(false)
    override val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    private var engine: TextToSpeech? = null
    private var engineReady = false
    private var pending: (() -> Unit)? = null
    private var lastUtteranceId: String? = null

    override fun speak(text: String, onUnavailable: () -> Unit) {
        _speaking.value = true
        val start = { start(text, onUnavailable) }
        if (engineReady) {
            start()
            return
        }
        pending = start
        // Created on first use: binding the engine is slow and most readings are never listened to.
        if (engine == null) {
            // The callback can run inside the constructor when the device has no engine at all.
            var initFailed = false
            val created = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    engineReady = true
                    pending?.invoke()
                    pending = null
                } else {
                    initFailed = true
                    val waiting = pending != null
                    release()
                    if (waiting) onUnavailable()
                }
            }
            if (!initFailed) {
                created.setOnUtteranceProgressListener(progressListener)
                engine = created
            }
        }
    }

    private fun start(text: String, onUnavailable: () -> Unit) {
        val tts = engine ?: return
        val language = tts.setLanguage(Locale.forLanguageTag("th-TH"))
        val parts = splitForSpeech(stripMarkup(text), TextToSpeech.getMaxSpeechInputLength())
        if (language < TextToSpeech.LANG_AVAILABLE || parts.isEmpty()) {
            _speaking.value = false
            onUnavailable()
            return
        }
        lastUtteranceId = "reading-${parts.lastIndex}"
        parts.forEachIndexed { index, part ->
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            if (tts.speak(part, mode, null, "reading-$index") == TextToSpeech.ERROR) {
                tts.stop()
                _speaking.value = false
                onUnavailable()
                return
            }
        }
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit
        override fun onDone(utteranceId: String?) {
            if (utteranceId == lastUtteranceId) _speaking.value = false
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            _speaking.value = false
        }
    }

    override fun stop() {
        pending = null
        engine?.stop()
        _speaking.value = false
    }

    override fun openVoiceSettings() {
        // Not every manufacturer's Settings has this screen; fall back to the Settings home.
        listOf("com.android.settings.TTS_SETTINGS", Settings.ACTION_SETTINGS).firstOrNull { action ->
            runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        }
    }

    override fun release() {
        pending = null
        engine?.shutdown()
        engine = null
        engineReady = false
        _speaking.value = false
    }
}

@Composable
actual fun rememberSpeechPlayer(): SpeechPlayer {
    val context = LocalContext.current
    val player = remember(context) { AndroidSpeechPlayer(context) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
