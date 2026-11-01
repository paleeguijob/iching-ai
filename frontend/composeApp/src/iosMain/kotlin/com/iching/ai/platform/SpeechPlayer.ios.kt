package com.iching.ai.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.iching.ai.domain.oracle.stripMarkup
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject

private class IosSpeechPlayer : SpeechPlayer {
    private val _state = MutableStateFlow(SpeechState.IDLE)
    override val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val synthesizer = AVSpeechSynthesizer()

    // The synthesizer only holds its delegate weakly, so this property keeps it alive.
    @OptIn(ExperimentalForeignApi::class)
    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didStartSpeechUtterance: AVSpeechUtterance) {
            _state.compareAndSet(SpeechState.PREPARING, SpeechState.SPEAKING)
        }

        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) {
            _state.value = SpeechState.IDLE
        }
    }

    init {
        synthesizer.delegate = delegate
    }

    /** iOS loads the voice when it first speaks; there is no engine to start ahead of time. */
    override fun prepare() = Unit

    override fun speak(text: String, onUnavailable: () -> Unit) {
        val voice = AVSpeechSynthesisVoice.voiceWithLanguage("th-TH")
        val cleaned = stripMarkup(text)
        if (voice == null || cleaned.isEmpty()) {
            onUnavailable()
            return
        }
        val utterance = AVSpeechUtterance.speechUtteranceWithString(cleaned)
        utterance.voice = voice
        _state.value = SpeechState.PREPARING
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        _state.value = SpeechState.IDLE
    }

    override fun openVoiceSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
    }

    override fun release() = stop()
}

@Composable
actual fun rememberSpeechPlayer(): SpeechPlayer {
    val player = remember { IosSpeechPlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
