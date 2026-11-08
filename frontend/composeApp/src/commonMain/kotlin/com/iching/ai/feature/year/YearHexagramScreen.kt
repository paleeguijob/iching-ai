package com.iching.ai.feature.year

import com.iching.ai.ui.theme.ZenSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.domain.model.CoinFace
import com.iching.ai.domain.model.LineType
import com.iching.ai.feature.divination.TossStage
import com.iching.ai.platform.ZenSound
import com.iching.ai.platform.rememberSoundPlayer
import com.iching.ai.ui.components.AncientCoin
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.InkDivider
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenSecondaryButton
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.toThaiNumerals
import kotlinx.coroutines.delay

private const val THEME_NOTICE = "ท่านได้รับธีมชาดทอง เลือกใช้ได้ในหน้าตั้งค่า"

/**
 * The year hexagram: the ritual first, then the result. [onShare] and [onAskHermit] show their button
 * only when the caller passes them.
 */
@Composable
fun YearHexagramScreen(
    viewModel: YearViewModel,
    onBack: () -> Unit,
    onOpenHexagram: (Int) -> Unit,
    onShare: (() -> Unit)? = null,
    onAskHermit: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val sound = rememberSoundPlayer()
    val ui = state

    LaunchedEffect(ui is YearUi.Ritual) {
        // A chime to settle the mind before the first toss.
        if (ui is YearUi.Ritual && soundEnabled) {
            delay(500)
            sound.play(ZenSound.WIND_CHIME)
        }
    }
    val notice = (ui as? YearUi.Result)?.firstTime == true
    LaunchedEffect(notice) {
        if (notice) {
            if (soundEnabled) sound.play(ZenSound.WIND_CHIME)
            delay(4000)
            viewModel.onThemeNoticeShown()
        }
    }

    PaperBackground(showMountains = ui !is YearUi.Result) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "กลับ", tint = ZenColors.InkBlack)
                }
                if (ui is YearUi.Ritual) {
                    Text("ตั้งจิตอธิษฐานรับปีใหม่", style = MaterialTheme.typography.headlineSmall, color = ZenColors.InkBlack)
                }
            }
            when (ui) {
                YearUi.Loading -> Message("กำลังตั้งจิต...")
                is YearUi.Ritual -> RitualStage(ui, soundEnabled, sound::play, viewModel::onToss)
                is YearUi.Result -> ResultStage(ui, onOpenHexagram, onShare, onAskHermit)
                YearUi.Offline -> Message("ต้องเชื่อมต่ออินเทอร์เน็ตเพื่อรับกวาประจำปี", retry = viewModel::retry)
                YearUi.RateLimited -> Message("รอสักครู่แล้วลองโยนอีกครั้ง", retry = viewModel::retry)
                YearUi.Inactive -> Message("ช่วงเสี่ยงทายกวาประจำปีสิ้นสุดแล้ว")
            }
        }
        if (notice) {
            Text(
                THEME_NOTICE,
                style = MaterialTheme.typography.bodySmall,
                color = ZenColors.PaperCream,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ZenColors.InkBlack.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun Message(text: String, retry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = if (retry == null) FontStyle.Italic else FontStyle.Normal),
            color = ZenColors.InkSoft,
            textAlign = TextAlign.Center,
        )
        if (retry != null) {
            Spacer(Modifier.height(16.dp))
            ZenPrimaryButton(text = "ลองใหม่", onClick = retry)
        }
    }
}

/** The reading's coin-toss stage, minus the question and the "toss until done" action. */
@Composable
private fun RitualStage(ui: YearUi.Ritual, soundEnabled: Boolean, play: (ZenSound) -> Unit, onToss: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(ui.tossKey) {
        if (ui.tossKey > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (soundEnabled) {
                delay(380)
                play(ZenSound.COIN_TOSS)
            }
        }
    }
    TossStage(
        prompt = "ตั้งจิตให้สงบ แล้วโยนเหรียญ",
        tosses = ui.tosses,
        currentToss = ui.currentToss,
        tossKey = ui.tossKey,
        isTossing = ui.isTossing,
        awaitingCast = ui.awaitingCast,
        canToss = ui.canToss,
        onToss = onToss,
    )
}

@Composable
private fun ResultStage(
    ui: YearUi.Result,
    onOpenHexagram: (Int) -> Unit,
    onShare: (() -> Unit)?,
    onAskHermit: (() -> Unit)?,
) {
    val hexagram = ui.hexagram
    val message = hexagram.yearMessage
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = ZenSpacing.screen, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 1. Header
        Text(
            "กวาประจำปี${campaignYearLabel(ui.year)}",
            style = MaterialTheme.typography.headlineSmall,
            color = ZenColors.InkBlack,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        HexagramFigure(
            pattern = null,
            lines = ui.lines.map(LineType::fromValue),
            animateReveal = true,
            lineWidth = 110.dp,
            lineHeight = 11.dp,
            gap = 9.dp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "${hexagram.nameCn}  ${hexagram.id.toThaiNumerals()} · ${hexagram.shortNameTh}",
            style = MaterialTheme.typography.titleLarge,
            color = ZenColors.InkBlack,
            textAlign = TextAlign.Center,
        )
        Text(hexagram.meaningTh, style = MaterialTheme.typography.bodyMedium, color = ZenColors.BambooDeep, textAlign = TextAlign.Center)

        if (message != null) {
            Spacer(Modifier.height(24.dp))
            // 2. Overview
            Text(message.overview, style = MaterialTheme.typography.bodyLarge, color = ZenColors.InkBlack, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            InkDivider()
            Spacer(Modifier.height(20.dp))
            // 3. Work, love, money
            Aspect("การงาน", message.work)
            Spacer(Modifier.height(14.dp))
            Aspect("ความรัก", message.love)
            Spacer(Modifier.height(14.dp))
            Aspect("การเงิน", message.money)
            Spacer(Modifier.height(24.dp))
            // 4. Blessing in a seal frame
            Text(
                message.blessing,
                style = MaterialTheme.typography.titleMedium,
                color = ZenColors.CinnabarRed,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, ZenColors.CinnabarRed.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            )
        }

        // 5. Buttons, only when the caller wires them
        if (onShare != null || onAskHermit != null) {
            Spacer(Modifier.height(24.dp))
            onShare?.let { ZenPrimaryButton(text = "แชร์การ์ดปีใหม่", onClick = it, modifier = Modifier.fillMaxWidth()) }
            if (onShare != null && onAskHermit != null) Spacer(Modifier.height(10.dp))
            onAskHermit?.let { ZenSecondaryButton(text = "ถามนักพรตเรื่องปีนี้", onClick = it, modifier = Modifier.fillMaxWidth()) }
        }

        // 6. Scripture link
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { onOpenHexagram(hexagram.id) }) {
            Text("อ่านตัวบทคัมภีร์ของกวานี้", color = ZenColors.CinnabarRed, style = MaterialTheme.typography.labelLarge)
        }

        // 7. Footnote
        Spacer(Modifier.height(8.dp))
        Text(
            "ข้อความประจำปีเขียนไว้ล่วงหน้าจากความหมายของกวา ใช้เพื่อการสะท้อนคิด",
            style = MaterialTheme.typography.labelSmall,
            color = ZenColors.InkFaint,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Aspect(label: String, text: String) {
    Column(Modifier.fillMaxWidth()) {
        SectionLabel(label)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
    }
}
