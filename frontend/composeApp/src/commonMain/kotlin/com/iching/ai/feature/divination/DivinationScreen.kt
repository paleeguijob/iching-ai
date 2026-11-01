package com.iching.ai.feature.divination

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.iching.ai.domain.model.LineType
import com.iching.ai.feature.reading.CastSummary
import com.iching.ai.feature.share.ShareCardSheet
import com.iching.ai.feature.share.readingShareContent
import com.iching.ai.feature.reading.AiContentLabel
import com.iching.ai.feature.reading.OracleScroll
import com.iching.ai.feature.reading.ScriptureSection
import com.iching.ai.platform.ZenSound
import com.iching.ai.platform.rememberSoundPlayer
import com.iching.ai.ui.components.AncientCoin
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenSecondaryButton
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.toThaiNumerals
import kotlinx.coroutines.delay

@Composable
fun DivinationScreen(
    viewModel: DivinationViewModel,
    onBack: () -> Unit,
    onOpenHexagram: (Int) -> Unit,
    onOpenPaywall: () -> Unit,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sound = rememberSoundPlayer()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(state.tossKey) {
        if (state.tossKey > 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(3500)
            viewModel.onMessageShown()
        }
    }

    LaunchedEffect(state.tossKey) {
        if (state.tossKey > 0 && state.soundEnabled) {
            delay(380)
            sound.play(ZenSound.COIN_TOSS)
        }
    }
    LaunchedEffect(state.phase) {
        if (state.phase == DivinationPhase.REVEAL && state.soundEnabled) sound.play(ZenSound.WIND_CHIME)
    }
    LaunchedEffect(Unit) {
        // A chime to settle the mind before the first toss.
        if (state.soundEnabled) {
            delay(500)
            sound.play(ZenSound.WIND_CHIME)
        }
    }

    PaperBackground(showMountains = state.phase == DivinationPhase.CASTING) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "กลับ", tint = ZenColors.InkBlack)
                }
                Text(
                    when (state.phase) {
                        DivinationPhase.CASTING -> "เสี่ยงทาย"
                        DivinationPhase.REVEAL -> "ฉักกะปรากฏ"
                        DivinationPhase.SAFETY -> "หยุดพักก่อน"
                        else -> "คำทำนาย"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = ZenColors.InkBlack,
                )
            }
            AnimatedContent(
                targetState = state.phase,
                contentKey = { phase -> if (phase == DivinationPhase.CASTING) 0 else if (phase == DivinationPhase.SAFETY) 1 else 2 },
                transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(500)) },
                label = "phase",
                modifier = Modifier.weight(1f),
            ) { phase ->
                when (phase) {
                    DivinationPhase.CASTING -> CastingStage(state, viewModel::onToss, viewModel::onTossRemaining)
                    DivinationPhase.SAFETY -> SafetyStage(state.safetyMessage.orEmpty(), onDone)
                    else -> ReadingStage(state, viewModel, onOpenHexagram, onOpenPaywall, onDone)
                }
            }
        }
        state.message?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color = ZenColors.PaperCream,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ZenColors.InkBlack.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun CastingStage(state: DivinationUiState, onToss: () -> Unit, onTossRemaining: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = noRipple, indication = null, enabled = state.canToss, onClick = onToss)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            if (state.question.isBlank()) "ตั้งจิตให้สงบ แล้วโยนเหรียญ" else "“${state.question}”",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = ZenColors.InkSoft,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
        Spacer(Modifier.weight(0.6f))
        HexagramFigure(
            pattern = null,
            lines = state.tosses.map { it.line } + List(6 - state.tosses.size) { LineType.YOUNG_YANG },
            revealed = state.tosses.size,
            lineWidth = 150.dp,
            lineHeight = 14.dp,
            gap = 11.dp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "เส้นที่ ${(state.tossesDone + if (state.tossesDone < 6) 1 else 0).coerceAtMost(6).toThaiNumerals()} จาก ๖",
            style = MaterialTheme.typography.labelLarge,
            color = ZenColors.BambooDeep,
        )
        Spacer(Modifier.weight(0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            val faces = state.currentToss?.faces
            repeat(3) { i ->
                AncientCoin(
                    face = faces?.get(i),
                    tossKey = state.tossKey,
                    delayMillis = i * 90,
                    size = 78.dp,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.height(28.dp), contentAlignment = Alignment.Center) {
            val last = state.tosses.lastOrNull()
            if (state.awaitingCast) {
                Text("กำลังตั้งจิต...", style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), color = ZenColors.InkFaint)
            } else if (!state.isTossing && last != null) {
                Text(lineLabel(last.line), style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
            }
        }
        Spacer(Modifier.weight(0.4f))
        ZenPrimaryButton(
            text = if (state.tossesDone == 0) "โยนเหรียญ" else "โยนครั้งต่อไป",
            onClick = onToss,
            enabled = state.canToss,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onTossRemaining, enabled = state.canToss) {
            Text("โยนต่อเนื่องจนครบ", color = ZenColors.InkSoft, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun lineLabel(line: LineType): String = when (line) {
    LineType.OLD_YIN -> "หยินแก่ (๖) — เส้นแปร"
    LineType.YOUNG_YANG -> "หยางอ่อน (๗)"
    LineType.YOUNG_YIN -> "หยินอ่อน (๘)"
    LineType.OLD_YANG -> "หยางแก่ (๙) — เส้นแปร"
}

@Composable
private fun ReadingStage(
    state: DivinationUiState,
    viewModel: DivinationViewModel,
    onOpenHexagram: (Int) -> Unit,
    onOpenPaywall: () -> Unit,
    onDone: () -> Unit,
) {
    val cast = state.cast ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.error ?: "", color = ZenColors.CinnabarRed)
        }
        return
    }
    var showReport by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scroll)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CastSummary(
            question = state.question,
            lines = cast.lines,
            primary = cast.primary,
            relating = cast.relating,
            createdAtMillis = null,
            onHexagramClick = onOpenHexagram,
        )
        Spacer(Modifier.height(24.dp))
        if (state.phase != DivinationPhase.REVEAL) {
            if (state.phase == DivinationPhase.COMPLETE) {
                state.notice?.let {
                    ReadingNoticeCard(
                        notice = it,
                        status = state.status,
                        canRetry = state.serverReadingId != null,
                        isWatchingAd = state.isWatchingAd,
                        onRetry = viewModel::onInterpretAgain,
                        onWatchAd = viewModel::onWatchAd,
                        onOpenPaywall = onOpenPaywall,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
            OracleScroll(
                text = state.interpretation,
                isComplete = state.phase == DivinationPhase.COMPLETE,
                animate = true,
                source = state.source,
            )
            if (state.phase == DivinationPhase.COMPLETE) {
                AiContentLabel(
                    source = state.source,
                    onReport = if (state.isAiReading) ({ showReport = true }) else null,
                    reported = state.reported,
                )
                Spacer(Modifier.height(12.dp))
                if (state.isAiReading) {
                    ListenButton(
                        text = state.interpretation,
                        premium = state.status.premium,
                        onOpenPaywall = onOpenPaywall,
                        log = viewModel::logEvent,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                ZenSecondaryButton("แชร์การ์ดคำทำนาย", onClick = { sharing = true }, modifier = Modifier.fillMaxWidth())
                if (sharing) {
                    ShareCardSheet(
                        content = readingShareContent(cast.primary, state.interpretation.takeIf { state.isAiReading }),
                        log = viewModel::logEvent,
                        onDismiss = { sharing = false },
                    )
                }
                Spacer(Modifier.height(20.dp))
                if (state.isAiReading) {
                    FollowUpSection(state, viewModel::onSendFollowUp, onOpenPaywall)
                    Spacer(Modifier.height(24.dp))
                }
                ScriptureSection(cast.primary, cast.relating, cast.changingPositions)
                Spacer(Modifier.height(20.dp))
                Text(
                    "บันทึกลงในบันทึกชะตาแล้ว",
                    style = MaterialTheme.typography.labelMedium,
                    color = ZenColors.BambooDeep,
                )
                Spacer(Modifier.height(12.dp))
                ZenSecondaryButton("กลับสู่ความสงบ", onClick = onDone)
                Spacer(Modifier.height(28.dp))
            } else {
                Spacer(Modifier.height(24.dp))
            }
        } else {
            Text(
                "สูดลมหายใจลึก ๆ ... คัมภีร์กำลังเปิดออก",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = ZenColors.InkSoft,
            )
        }
    }
    if (showReport) {
        ReportDialog(
            onDismiss = { showReport = false },
            onSubmit = {
                showReport = false
                viewModel.onReport(it)
            },
        )
    }
    // Keep the newest words in view while the scroll unrolls.
    LaunchedEffect(state.interpretation.length / 120) {
        if (state.phase == DivinationPhase.INTERPRETING && scroll.maxValue > 0) scroll.animateScrollTo(scroll.maxValue)
    }
}
