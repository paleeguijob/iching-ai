package com.iching.ai.feature.divination

import com.iching.ai.ui.strings.LocalStrings
import com.iching.ai.ui.strings.DivinationStrings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.platform.SpeechState
import com.iching.ai.platform.rememberSpeechPlayer
import com.iching.ai.ui.components.ZenSecondaryButton

enum class ListenAction { OPEN_PAYWALL, SPEAK, STOP }

/** Stopping comes first: a subscription lapsing mid-reading must not leave the voice unstoppable. */
fun listenAction(premium: Boolean, state: SpeechState): ListenAction = when {
    state != SpeechState.IDLE -> ListenAction.STOP
    premium -> ListenAction.SPEAK
    else -> ListenAction.OPEN_PAYWALL
}

fun listenLabel(premium: Boolean, state: SpeechState, strings: DivinationStrings): String = when {
    state == SpeechState.PREPARING -> strings.listenPreparing
    state == SpeechState.SPEAKING -> strings.listenStop
    premium -> strings.listen
    else -> strings.listenPremium
}

/** Reads the hermit's words aloud with the device's voice. A Premium perk. */
@Composable
fun ListenButton(
    text: String,
    premium: Boolean,
    onOpenPaywall: () -> Unit,
    log: (String, Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.divination
    val player = rememberSpeechPlayer()
    val state by player.state.collectAsStateWithLifecycle()
    var noVoice by remember { mutableStateOf(false) }

    // Only subscribers can get past the paywall, so only they pay for starting the engine early.
    LaunchedEffect(player, premium) { if (premium) player.prepare() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.stop() }

    ZenSecondaryButton(
        text = listenLabel(premium, state, strings),
        modifier = modifier,
        onClick = {
            when (listenAction(premium, state)) {
                ListenAction.OPEN_PAYWALL -> onOpenPaywall()
                ListenAction.STOP -> player.stop()
                ListenAction.SPEAK -> {
                    log("tts_play", emptyMap())
                    player.speak(text, onUnavailable = { noVoice = true })
                }
            }
        },
    )

    if (noVoice) {
        AlertDialog(
            onDismissRequest = { noVoice = false },
            title = { Text(strings.noVoiceTitle) },
            text = { Text(strings.noVoiceBody) },
            confirmButton = {
                TextButton(onClick = {
                    noVoice = false
                    player.openVoiceSettings()
                }) { Text(strings.openSettings) }
            },
            dismissButton = { TextButton(onClick = { noVoice = false }) { Text(strings.notNow) } },
        )
    }
}
