package com.iching.ai.feature.divination

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.platform.rememberSpeechPlayer
import com.iching.ai.ui.components.ZenSecondaryButton

enum class ListenAction { OPEN_PAYWALL, SPEAK, STOP }

/** Stopping comes first: a subscription lapsing mid-reading must not leave the voice unstoppable. */
fun listenAction(premium: Boolean, speaking: Boolean): ListenAction = when {
    speaking -> ListenAction.STOP
    premium -> ListenAction.SPEAK
    else -> ListenAction.OPEN_PAYWALL
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
    val player = rememberSpeechPlayer()
    val speaking by player.speaking.collectAsStateWithLifecycle()
    var noVoice by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.stop() }

    ZenSecondaryButton(
        text = when {
            speaking -> "หยุดอ่าน"
            premium -> "ฟังคำทำนาย"
            else -> "ฟังคำทำนาย · Premium"
        },
        modifier = modifier,
        onClick = {
            when (listenAction(premium, speaking)) {
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
            title = { Text("ยังไม่มีเสียงภาษาไทยในเครื่อง") },
            text = { Text("ติดตั้งเสียงอ่านภาษาไทยในการตั้งค่าของเครื่องก่อน แล้วกลับมาฟังอีกครั้ง") },
            confirmButton = {
                TextButton(onClick = {
                    noVoice = false
                    player.openVoiceSettings()
                }) { Text("เปิดการตั้งค่า") }
            },
            dismissButton = { TextButton(onClick = { noVoice = false }) { Text("ไว้ก่อน") } },
        )
    }
}
