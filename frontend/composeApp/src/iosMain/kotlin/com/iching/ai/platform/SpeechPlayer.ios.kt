package com.iching.ai.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.iching.ai.domain.oracle.stripMarkup
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
    private val _speaking = MutableStateFlow(false)
    override val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    private val synthesizer = AVSpeechSynthesizer()

    // The synthesizer only holds its delegate weakly, so this property keeps it alive.
    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) {
            _speaking.value = false
        }
    }

    init {
        synthesizer.delegate = delegate
    }

    override fun speak(text: String, onUnavailable: () -> Unit) {
        val voice = AVSpeechSynthesisVoice.voiceWithLanguage("th-TH")
        val cleaned = stripMarkup(text)
        if (voice == null || cleaned.isEmpty()) {
            onUnavailable()
            return
        }
        val utterance = AVSpeechUtterance.speechUtteranceWithString(cleaned)
        utterance.voice = voice
        _speaking.value = true
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        _speaking.value = false
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
