package com.iching.ai.feature.siamsi

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.IosShare
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.domain.siamsi.PuaResult
import com.iching.ai.feature.divination.SafetyStage
import com.iching.ai.feature.home.PreCastSheet
import com.iching.ai.feature.home.SIAMSI_PRECAST_COPY
import com.iching.ai.feature.share.SiamSiShareSheet
import com.iching.ai.feature.share.siamsiShareContent
import com.iching.ai.platform.ShakeAccumulator
import com.iching.ai.platform.ZenSound
import com.iching.ai.platform.rememberShakeDetector
import com.iching.ai.platform.rememberSoundPlayer
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.toThaiNumerals
import kotlinx.coroutines.delay
import kotlin.time.Clock

private const val RATTLE_EVERY_MILLIS = 420L
private const val SHAKE_VISIBLE_MILLIS = 300L

/** One Siam Si draw: shake the cylinder, see the stick, toss the pua, open the slip. */
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
    val set = viewModel.set
    val slip = stage as? SiamSiStage.Slip
    if (sharing && set != null && slip != null) {
        SiamSiShareSheet(siamsiShareContent(set, slip.stick), log = viewModel::logEvent, onDismiss = { sharing = false })
    }

    PaperBackground(showMountains = stage !is SiamSiStage.Slip) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            val puaNumber = when (val s = stage) {
                is SiamSiStage.Tossing -> s.number
                is SiamSiStage.Pua -> s.progress.rounds.last().stick
                is SiamSiStage.Confirmed -> s.stick.number
                else -> null
            }
            TopBar(
                title = when {
                    slip != null -> "ใบเซียมซี"
                    puaNumber != null -> "เสี่ยงไม้ปัว"
                    else -> "เซียมซี"
                },
                onBack = onBack,
                trailing = {
                    when {
                        slip != null && set != null -> IconButton(onClick = { sharing = true }) {
                            Icon(Icons.Outlined.IosShare, contentDescription = "แชร์ใบเซียมซี", tint = ZenColors.InkSoft)
                        }
                        puaNumber != null -> Text(
                            "ไม้ที่ ${puaNumber.toThaiNumerals()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZenColors.InkSoft,
                            modifier = Modifier.padding(end = ZenSpacing.screen),
                        )
                    }
                },
            )
            when (val s = stage) {
                SiamSiStage.Loading, SiamSiStage.Unpaid -> Unit
                is SiamSiStage.Safety -> SafetyStage(s.message, onDone = onBack)
                SiamSiStage.Shaking -> ShakeStage(viewModel, soundEnabled)
                is SiamSiStage.Fallen -> DrawStep(progress = 0.75f, button = "เสี่ยงไม้ปัวยืนยัน", onButton = viewModel::onStartPua) {
                    Spacer(Modifier.weight(1f))
                    FallenStage(s.number)
                    Spacer(Modifier.weight(1f))
                }
                is SiamSiStage.Tossing -> PuaStep(
                    results = s.results,
                    shown = null,
                    autoConfirmed = false,
                    button = "โยนไม้ปัว",
                    onButton = viewModel::onTossPua,
                    onToss = viewModel::onTossPua,
                )
                is SiamSiStage.Pua -> PuaStep(
                    results = s.progress.rounds.mapNotNull { it.result },
                    shown = s.progress.rounds.last().result,
                    autoConfirmed = false,
                    button = "เขย่าใหม่",
                    onButton = viewModel::onReshake,
                    onToss = null,
                )
                is SiamSiStage.Confirmed -> PuaStep(
                    results = s.progress.rounds.mapNotNull { it.result },
                    shown = s.progress.rounds.last().result,
                    autoConfirmed = s.autoConfirmed,
                    button = "เปิดใบเซียมซี",
                    onButton = viewModel::onOpenSlip,
                    onToss = null,
                )
                is SiamSiStage.Slip -> if (set != null) {
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = ZenSpacing.screen, vertical = ZenSpacing.sm),
                    ) {
                        SiamSiSlipContent(
                            set = set,
                            stick = s.stick,
                            question = viewModel.question,
                            category = viewModel.category,
                            interpret = interpret,
                            status = status,
                            onInterpret = viewModel::onInterpret,
                            onSendFollowUp = viewModel::onSendFollowUp,
                            onOpenPaywall = onOpenPaywall,
                            onReport = viewModel::onReport,
                            onShare = { sharing = true },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "กลับ", tint = ZenColors.InkBlack)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = ZenColors.InkBlack, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** The thin ink line under the bar: how far through the ritual the seeker is. */
@Composable
private fun DrawProgress(fraction: Float) {
    Box(
        Modifier
            .padding(horizontal = ZenSpacing.screen)
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(ZenColors.current.outlineVariant),
    ) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(ZenColors.InkBlack))
    }
}

/** A step of the draw: progress line on top, [content] in the middle, the main button pinned at the bottom. */
@Composable
private fun DrawStep(
    progress: Float,
    button: String?,
    onButton: () -> Unit,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        Spacer(Modifier.height(ZenSpacing.sm))
        DrawProgress(progress)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = ZenSpacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
        Box(Modifier.padding(horizontal = ZenSpacing.screen, vertical = ZenSpacing.lg)) {
            when {
                bottom != null -> bottom()
                button != null -> ZenPrimaryButton(button, onClick = onButton, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun QuestionLine(question: String) {
    Text(
        if (question.isBlank()) "อธิษฐานในใจ" else "“$question”",
        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
        color = ZenColors.InkSoft,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = ZenSpacing.lg, start = ZenSpacing.sm, end = ZenSpacing.sm),
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
    DrawStep(progress = 0.5f, button = null, onButton = {}, bottom = { HoldButton("กดค้างเพื่อเขย่า") { held = it } }) {
        QuestionLine(viewModel.question)
        Spacer(Modifier.weight(1f))
        SiamSiCylinder(shaking = shaking, onHoldChange = { held = it })
        Spacer(Modifier.height(ZenSpacing.md))
        Text("เขย่าโทรศัพท์ หรือ กดค้างที่กระบอก", style = MaterialTheme.typography.bodySmall, color = ZenColors.InkSoft)
        Spacer(Modifier.weight(1f))
    }
}

/** The main button's look, but it shakes while held instead of acting on a tap. */
@Composable
private fun HoldButton(text: String, onHoldChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ZenColors.InkBlack)
            .semantics { role = Role.Button }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    onHoldChange(true)
                    waitForUpOrCancellation()
                    onHoldChange(false)
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(ZenColors.CinnabarSoft, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = ZenColors.PaperCream)
    }
}

/**
 * The pua screen: the blocks (tap to toss while [onToss] is set), the result of the last toss with a dot per
 * round, the rules, and the next step.
 */
@Composable
private fun PuaStep(
    results: List<PuaResult>,
    shown: PuaResult?,
    autoConfirmed: Boolean,
    button: String,
    onButton: () -> Unit,
    onToss: (() -> Unit)?,
) {
    DrawStep(progress = 0.9f, button = button, onButton = onButton) {
        Text(
            "แตะเพื่อโยนไม้ปัว ถามว่าใบนี้ใช่หรือไม่",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = ZenColors.InkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = ZenSpacing.lg),
        )
        Spacer(Modifier.height(ZenSpacing.xl + ZenSpacing.lg))
        PuaBlocks(result = shown, onToss = onToss)
        Spacer(Modifier.height(ZenSpacing.xl))
        if (shown != null) {
            PuaResultCard(results, autoConfirmed)
        } else if (results.isNotEmpty()) {
            RoundDots(results)
        }
        Spacer(Modifier.height(ZenSpacing.lg))
        Text(
            "ศักดิ์ = ยืนยัน · หัวเราะ / ไม่ = เขย่าใหม่ (ไม่นับโควตา)\nรอบที่ ๓ ยืนยันใบสุดท้ายให้อัตโนมัติ",
            style = MaterialTheme.typography.labelSmall,
            color = ZenColors.InkFaint,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
    }
}
