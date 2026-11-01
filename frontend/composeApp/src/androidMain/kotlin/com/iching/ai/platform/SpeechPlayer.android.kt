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

/** Short enough that the first piece starts quickly, long enough not to chop up every clause. */
private const val SPEECH_PIECE_CHARS = 200

private class AndroidSpeechPlayer(private val context: Context) : SpeechPlayer {
    private val _state = MutableStateFlow(SpeechState.IDLE)
    override val state: StateFlow<SpeechState> = _state.asStateFlow()

    private var engine: TextToSpeech? = null
    private var engineReady = false
    private var thaiAvailable = false
    private var pending: (() -> Unit)? = null
    private var pendingUnavailable: (() -> Unit)? = null
    private var lastUtteranceId: String? = null

    override fun prepare() = ensureEngine()

    private fun ensureEngine() {
        if (engine != null) return
        // The callback can run inside the constructor when the device has no engine at all.
        var initFailed = false
        val created = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Loading the voice here rather than on the first speak() is part of the head start.
                thaiAvailable = engine?.setLanguage(Locale.forLanguageTag("th-TH"))?.let { it >= TextToSpeech.LANG_AVAILABLE } == true
                engineReady = true
                pending?.invoke()
                clearPending()
            } else {
                initFailed = true
                val unavailable = pendingUnavailable
                release()
                unavailable?.invoke()
            }
        }
        if (!initFailed) {
            created.setOnUtteranceProgressListener(progressListener)
            engine = created
        }
    }

    override fun speak(text: String, onUnavailable: () -> Unit) {
        _state.value = SpeechState.PREPARING
        if (engineReady) {
            start(text, onUnavailable)
            return
        }
        pending = { start(text, onUnavailable) }
        pendingUnavailable = onUnavailable
        ensureEngine()
    }

    private fun start(text: String, onUnavailable: () -> Unit) {
        val tts = engine ?: return
        val parts = splitForSpeech(stripMarkup(text), minOf(SPEECH_PIECE_CHARS, TextToSpeech.getMaxSpeechInputLength()))
        if (!thaiAvailable || parts.isEmpty()) {
            _state.value = SpeechState.IDLE
            onUnavailable()
            return
        }
        lastUtteranceId = "reading-${parts.lastIndex}"
        parts.forEachIndexed { index, part ->
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            if (tts.speak(part, mode, null, "reading-$index") == TextToSpeech.ERROR) {
                tts.stop()
                _state.value = SpeechState.IDLE
                onUnavailable()
                return
            }
        }
    }

    // These arrive on a background thread, possibly just after stop(): only a wait that is still on may become speech.
    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            _state.compareAndSet(SpeechState.PREPARING, SpeechState.SPEAKING)
        }

        override fun onDone(utteranceId: String?) {
            if (utteranceId == lastUtteranceId) _state.value = SpeechState.IDLE
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            _state.value = SpeechState.IDLE
        }
    }

    private fun clearPending() {
        pending = null
        pendingUnavailable = null
    }

    override fun stop() {
        clearPending()
        engine?.stop()
        _state.value = SpeechState.IDLE
    }

    override fun openVoiceSettings() {
        // Not every manufacturer's Settings has this screen; fall back to the Settings home.
        listOf("com.android.settings.TTS_SETTINGS", Settings.ACTION_SETTINGS).firstOrNull { action ->
            runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        }
    }

    override fun release() {
        clearPending()
        engine?.shutdown()
        engine = null
        engineReady = false
        thaiAvailable = false
        _state.value = SpeechState.IDLE
    }
}

@Composable
actual fun rememberSpeechPlayer(): SpeechPlayer {
    val context = LocalContext.current
    val player = remember(context) { AndroidSpeechPlayer(context) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
