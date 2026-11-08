package com.iching.ai.feature.siamsi

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.domain.siamsi.MAX_PUA_ROUNDS
import com.iching.ai.domain.siamsi.PuaResult
import com.iching.ai.feature.divination.SafetyStage
import com.iching.ai.feature.home.PreCastSheet
import com.iching.ai.feature.home.SIAMSI_PRECAST_COPY
import com.iching.ai.platform.ShakeAccumulator
import com.iching.ai.platform.ZenSound
import com.iching.ai.platform.rememberShakeDetector
import com.iching.ai.platform.rememberSoundPlayer
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenSecondaryButton
import com.iching.ai.feature.share.SiamSiShareSheet
import com.iching.ai.feature.share.siamsiShareContent
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.toThaiNumerals
import kotlinx.coroutines.delay
import kotlin.time.Clock

private const val RATTLE_EVERY_MILLIS = 420L
private const val SHAKE_VISIBLE_MILLIS = 300L
private const val CONFIRM_HOLD_MILLIS = 1_400L

/** One Siam Si draw: shake the cylinder, toss the pua, read the slip. */
@Composable
fun SiamSiScreen(viewModel: SiamSiViewModel, onBack: () -> Unit, onOpenPaywall: () -> Unit) {
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val interpret by viewModel.interpret.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(stage) { if (stage == SiamSiStage.Unpaid) onBack() }
    LaunchedEffect(interpret.message) {
        if (interpret.message != null) {
            delay(4_000)
            viewModel.onMessageShown()
        }
    }
    interpret.sheet?.let { ui ->
        PreCastSheet(
            ui = ui,
            onWatchAd = viewModel::onWatchAd,
            onBuyCoins = viewModel::onBuyCoins,
            onScriptureOnly = viewModel::onSheetDismissed,
            onPremium = {
                viewModel.onPremium()
                onOpenPaywall()
            },
            onCastFromReady = viewModel::onInterpretFromReady,
            onDismiss = viewModel::onSheetDismissed,
            copy = SIAMSI_PRECAST_COPY,
        )
    }

    var sharing by remember { mutableStateOf(false) }

    // A confirmed slip waits a moment under the landed blocks before it opens.
    var previous by remember { mutableStateOf<SiamSiStage?>(null) }
    var holding by remember { mutableStateOf(false) }
    val holdingConfirm = stage is SiamSiStage.Slip && (previous is SiamSiStage.Fallen || holding)
    LaunchedEffect(stage) {
        val s = stage
        if (s is SiamSiStage.Slip && previous is SiamSiStage.Fallen) {
            holding = true
            delay(CONFIRM_HOLD_MILLIS)
            holding = false
        }
        previous = s
    }

    PaperBackground(showMountains = stage !is SiamSiStage.Slip) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "กลับ", tint = ZenColors.InkBlack)
                }
                Text(
                    if (stage is SiamSiStage.Slip && !holdingConfirm) "ใบเซียมซี" else "เซียมซี",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ZenColors.InkBlack,
                )
            }
            val set = viewModel.set
            when (val s = stage) {
                SiamSiStage.Loading, SiamSiStage.Unpaid -> Unit
                is SiamSiStage.Safety -> SafetyStage(s.message, onDone = onBack)
                SiamSiStage.Shaking -> ShakeStage(viewModel, soundEnabled)
                is SiamSiStage.Fallen -> PuaStep(
                    viewModel.question,
                    fallen = s.number,
                    result = null,
                    round = 0,
                    button = "เสี่ยงไม้ปัวยืนยัน",
                    onButton = viewModel::onTossPua,
                    onToss = viewModel::onTossPua,
                )
                is SiamSiStage.Pua -> PuaStep(
                    viewModel.question,
                    fallen = s.progress.rounds.last().stick,
                    result = s.progress.rounds.last().result,
                    round = s.progress.rounds.size,
                    button = "เขย่าใหม่",
                    onButton = viewModel::onReshake,
                    onToss = {},
                )
                is SiamSiStage.Slip -> if (holdingConfirm) {
                    PuaStep(
                        viewModel.question,
                        fallen = s.stick.number,
                        result = viewModel.lastToss,
                        round = 0,
                        button = null,
                        onButton = {},
                        onToss = {},
                    )
                } else if (set != null) {
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = ZenSpacing.screen, vertical = ZenSpacing.md),
                    ) {
                        SiamSiSlipContent(
                            set = set,
                            stick = s.stick,
                            autoConfirmed = s.autoConfirmed,
                            question = viewModel.question,
                            category = viewModel.category,
                            interpret = interpret,
                            status = status,
                            onInterpret = viewModel::onInterpret,
                            onSendFollowUp = viewModel::onSendFollowUp,
                            onOpenPaywall = onOpenPaywall,
                            onReport = viewModel::onReport,
                        )
                        Spacer(Modifier.height(ZenSpacing.md))
                        ZenSecondaryButton("แชร์ใบเซียมซี", onClick = { sharing = true }, modifier = Modifier.fillMaxWidth())
                        if (sharing) {
                            SiamSiShareSheet(siamsiShareContent(set, s.stick), log = viewModel::logEvent, onDismiss = { sharing = false })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionLine(question: String) {
    Text(
        question.ifBlank { "อธิษฐานในใจ" }.let { if (question.isBlank()) it else "“$it”" },
        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
        color = ZenColors.InkSoft,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ZenSpacing.screen),
    )
}

@Composable
private fun ShakeStage(viewModel: SiamSiViewModel, soundEnabled: Boolean) {
    val detector = rememberShakeDetector()
    val sound = rememberSoundPlayer()
    val accumulator = remember { ShakeAccumulator() }
    var held by remember { mutableStateOf(false) }
    var lastShakeAt by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(0L) }
    val onShaken by rememberUpdatedState(viewModel::onShaken)
    fun clock() = Clock.System.now().toEpochMilliseconds()
    fun sample(g: Float) {
        val at = clock()
        if (g >= 1.6f) {
            if (lastShakeAt == 0L) viewModel.onShakeStarted()
            lastShakeAt = at
        }
        if (accumulator.onSample(g, at)) onShaken()
    }
    DisposableEffect(detector) {
        detector.start { g -> sample(g) }
        onDispose { detector.stop() }
    }
    LaunchedEffect(held) {
        while (held) {
            sample(2f)
            delay(100)
        }
    }
    // Ticks the clock so the cylinder settles once the shaking stops.
    LaunchedEffect(Unit) {
        while (true) {
            now = clock()
            delay(100)
        }
    }
    val shaking = held || now - lastShakeAt < SHAKE_VISIBLE_MILLIS
    LaunchedEffect(shaking, soundEnabled) {
        while (shaking && soundEnabled) {
            sound.play(ZenSound.STICK_RATTLE)
            delay(RATTLE_EVERY_MILLIS)
        }
    }
    Column(
        Modifier.fillMaxSize().navigationBarsPadding().padding(vertical = ZenSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZenSpacing.lg),
    ) {
        QuestionLine(viewModel.question)
        Text("เขย่าโทรศัพท์ หรือ กดค้างที่กระบอก", style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
        Spacer(Modifier.weight(1f))
        SiamSiCylinder(shaking = shaking, fallenNumber = null, onHoldChange = { held = it })
        Spacer(Modifier.weight(1f))
        Text("กดค้างเพื่อเขย่า", style = MaterialTheme.typography.labelMedium, color = ZenColors.InkFaint)
    }
}

@Composable
private fun PuaStep(
    question: String,
    fallen: Int,
    result: PuaResult?,
    round: Int,
    button: String?,
    onButton: () -> Unit,
    onToss: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = ZenSpacing.screen, vertical = ZenSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZenSpacing.md),
    ) {
        QuestionLine(question)
        Spacer(Modifier.weight(1f))
        SiamSiCylinder(shaking = false, fallenNumber = fallen, onHoldChange = {})
        Text("ไม้เซียมซีที่ ${fallen.toThaiNumerals()}", style = MaterialTheme.typography.titleMedium, color = ZenColors.InkBlack)
        Spacer(Modifier.height(ZenSpacing.sm))
        PuaBlocks(result = result, onToss = onToss)
        if (round > 0 && result != PuaResult.SAK) {
            Text(
                "รอบที่ ${round.toThaiNumerals()} จาก ${MAX_PUA_ROUNDS.toThaiNumerals()} · เขย่าใหม่ไม่เสียสิทธิ์",
                style = MaterialTheme.typography.bodySmall,
                color = ZenColors.InkFaint,
            )
        }
        Spacer(Modifier.weight(1f))
        if (button != null) {
            ZenPrimaryButton(button, onClick = onButton, modifier = Modifier.fillMaxWidth())
        } else {
            Box(Modifier.height(48.dp))
        }
    }
}
