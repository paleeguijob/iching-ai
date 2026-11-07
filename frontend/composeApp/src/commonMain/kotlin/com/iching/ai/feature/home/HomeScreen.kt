package com.iching.ai.feature.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.ui.toThaiNumerals
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.feature.year.campaignYearLabel
import com.iching.ai.ui.theme.ZenColors
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCast: (String, QuestionCategory?) -> Unit,
    onOpenDaily: () -> Unit,
    onOpenYear: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val daily by viewModel.dailyHexagram.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val deciding by viewModel.deciding.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.castRequests.collect { onCast(it.question, it.category) }
    }
    sheet?.let { ui ->
        PreCastSheet(
            ui = ui,
            onWatchAd = viewModel::onWatchAd,
            onBuyCoins = viewModel::onBuyCoins,
            onScriptureOnly = viewModel::onScriptureOnly,
            onPremium = {
                viewModel.onPremium()
                onOpenPaywall()
            },
            onCastFromReady = viewModel::onCastFromReady,
            onDismiss = viewModel::onSheetDismissed,
        )
    }
    val campaign by viewModel.campaignCard.collectAsStateWithLifecycle()
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onResumed()
        onPauseOrDispose { }
    }
    var question by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }

    PaperBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CinnabarSeal(text = "易", size = 34.dp, tilt = 0f)
                Spacer(Modifier.width(10.dp))
                Text("อี้จิง", style = MaterialTheme.typography.headlineSmall, color = ZenColors.InkBlack)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "ตั้งค่า", tint = ZenColors.InkSoft)
                }
            }

            Spacer(Modifier.height(36.dp))
            BreathingTitle()
            Spacer(Modifier.height(10.dp))
            Text(
                "สงบจิต ตั้งคำถามด้วยใจที่เปิดกว้าง\nแล้วให้คัมภีร์แห่งความเปลี่ยนแปลงชี้ทาง",
                style = MaterialTheme.typography.bodyMedium,
                color = ZenColors.InkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = question,
                onValueChange = { if (it.length <= 300) question = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                placeholder = {
                    Text("พิมพ์สิ่งที่ค้างคาใจ หรือเว้นว่างไว้เพื่ออธิษฐานในใจ...", color = ZenColors.InkFaint)
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZenColors.BambooGreen,
                    unfocusedBorderColor = ZenColors.InkBlack.copy(alpha = 0.15f),
                    focusedContainerColor = ZenColors.PaperCream.copy(alpha = 0.85f),
                    unfocusedContainerColor = ZenColors.PaperCream.copy(alpha = 0.6f),
                    cursorColor = ZenColors.CinnabarRed,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuestionCategory.entries.filter { it.pickable }.forEach { c ->
                    val selected = category == c.wire
                    Text(
                        c.labelTh,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) ZenColors.PaperCream else ZenColors.BambooDeep,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) ZenColors.BambooDeep else ZenColors.PaperCream.copy(alpha = 0f))
                            .border(1.dp, ZenColors.BambooGreen.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .clickable {
                                if (selected) {
                                    category = null
                                } else {
                                    category = c.wire
                                    if (question.isBlank() || QuestionCategory.entries.filter { it.pickable }.any { it.sampleTh == question }) question = c.sampleTh
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            GuidanceQuotaBar(guidanceQuota(status))
            Spacer(Modifier.height(12.dp))
            ZenPrimaryButton(
                text = startButtonText(deciding),
                onClick = { viewModel.onStart(question.trim(), QuestionCategory.fromWire(category)) },
                enabled = !deciding,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(36.dp))
            val card = campaign
            if (card != CampaignCard.Hidden) {
                LaunchedEffect(card::class) { viewModel.onCampaignCardShown(card) }
                CampaignHomeCard(card, onClick = {
                    viewModel.onCampaignCardTapped(card)
                    onOpenYear()
                })
                Spacer(Modifier.height(16.dp))
            }
            daily?.let { DailyHexagramCard(it, streakDays, onClick = onOpenDaily) }
            Spacer(Modifier.height(120.dp))
        }
    }
}

/** The Start button waits, with a word, while the status is checked before casting. */
fun startButtonText(deciding: Boolean): String = if (deciding) "กำลังตั้งจิต..." else "เริ่มเสี่ยงทาย"

/** What the home screen says about today's guidance; [count] is null when no count applies. */
data class GuidanceQuota(val label: String, val count: Int?, val note: String?)

private const val FREE_OUT_OF_GUIDANCE_NOTE = "สมัคร Premium หรือใช้เหรียญอี้จิงเพื่อรับคำชี้แนะต่อ · ยังเสี่ยงทายและอ่านตัวบทได้"

fun guidanceQuota(status: AccountStatus): GuidanceQuota {
    if (!status.aiEnabled) {
        return GuidanceQuota("นักพรตปลีกวิเวกชั่วคราว — ยังเสี่ยงทายและอ่านตัวบทคัมภีร์ได้", count = null, note = null)
    }
    val prefix = if (status.premium) "Premium · " else ""
    val label = "${prefix}วันนี้เหลือคำชี้แนะจากนักพรต"
    val count = status.readingsLeftToday
    val coins = status.coins
    if (count > 0) {
        val note = if (coins > 0) "และมีเหรียญอี้จิงอีก ${coins.toThaiNumerals()} เหรียญ ใช้ได้ทุกวัน ไม่มีวันหมดอายุ" else null
        return GuidanceQuota(label, count, note)
    }
    if (coins > 0) return GuidanceQuota("${prefix}เหรียญอี้จิงใช้ทำนายได้อีก", coins, note = "ใช้ได้ทุกวัน ไม่มีวันหมดอายุ")
    // A zero here would read as "no more today", so point to the ads while any are left.
    if (!status.premium && status.adsWatchableLeft > 0) {
        return GuidanceQuota(
            "ชมโฆษณา รับคำชี้แนะเพิ่มได้อีก",
            count = status.adsWatchableLeft,
            note = "กดเริ่มเสี่ยงทาย แล้วชมโฆษณาสั้นก่อนโยนเหรียญ",
        )
    }
    val note = if (status.premium) "ยังเสี่ยงทายและอ่านตัวบทได้" else FREE_OUT_OF_GUIDANCE_NOTE
    return GuidanceQuota(label, count, note)
}

@Composable
private fun GuidanceQuotaBar(quota: GuidanceQuota) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ZenColors.PaperCream.copy(alpha = 0.75f))
            .border(1.dp, ZenColors.InkBlack.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                quota.label,
                style = MaterialTheme.typography.titleSmall,
                color = if (quota.count == null) ZenColors.InkSoft else ZenColors.InkBlack,
            )
            quota.note?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkSoft)
            }
        }
        quota.count?.let { count ->
            val countColor = if (count > 0) ZenColors.BambooDeep else ZenColors.CinnabarRed
            Spacer(Modifier.width(12.dp))
            Row {
                Text(
                    count.toThaiNumerals(),
                    style = MaterialTheme.typography.displayMedium.copy(lineHeight = 44.sp),
                    color = countColor,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "ครั้ง",
                    style = MaterialTheme.typography.labelLarge,
                    color = countColor,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

private const val BREATH_STEPS = 128f

@Composable
private fun BreathingTitle() {
    val transition = rememberInfiniteTransition(label = "breath")
    val alpha by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse),
        label = "alpha",
    )
    // Steps too small to see, so the screen redraws ~10 times a second instead of on every vsync, forever.
    val steppedAlpha by remember { derivedStateOf { (alpha * BREATH_STEPS).roundToInt() / BREATH_STEPS } }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("問道", style = MaterialTheme.typography.displayMedium, color = ZenColors.InkBlack.copy(alpha = 0.12f))
        Text(
            "ถามทางแห่งเต๋า",
            style = MaterialTheme.typography.displaySmall,
            color = ZenColors.InkBlack,
            modifier = Modifier.graphicsLayer { this.alpha = steppedAlpha },
        )
    }
}

@Composable
private fun CampaignHomeCard(card: CampaignCard, onClick: () -> Unit) {
    val (title, subtitle, button) = when (card) {
        is CampaignCard.New -> Triple(
            "เสี่ยงทายกวาประจำปี${campaignYearLabel(card.year)}",
            "ตั้งจิตรับปีใหม่ ฟ้าประทานหนึ่งกวาให้ท่านทั้งปี",
            "เสี่ยงทายรับปีใหม่",
        )
        is CampaignCard.Cast -> Triple(
            "กวาประจำปีของท่าน: ${card.hexagram.shortNameTh}",
            card.hexagram.yearMessage?.blessing.orEmpty(),
            "ดูกวาประจำปี",
        )
        CampaignCard.Hidden -> return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ZenColors.PaperCream.copy(alpha = 0.75f))
            .border(1.dp, ZenColors.CinnabarRed.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = ZenColors.InkBlack)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
        }
        Spacer(Modifier.height(14.dp))
        ZenPrimaryButton(text = button, onClick = onClick, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DailyHexagramCard(hexagram: Hexagram, streakDays: Int, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ZenColors.PaperCream.copy(alpha = 0.75f))
            .border(1.dp, ZenColors.InkBlack.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        SectionLabel("กวาประจำวัน")
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HexagramFigure(hexagram.pattern, lineWidth = 52.dp, lineHeight = 7.dp, gap = 5.dp)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(hexagram.nameCn, style = MaterialTheme.typography.headlineMedium, color = ZenColors.InkBlack)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${hexagram.id} · ${hexagram.shortNameTh}",
                        style = MaterialTheme.typography.titleSmall,
                        color = ZenColors.InkSoft,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Text(hexagram.meaningTh, style = MaterialTheme.typography.bodyMedium, color = ZenColors.BambooDeep)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            hexagram.dailyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = ZenColors.InkSoft,
            maxLines = 4,
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (streakDays > 0) {
                Text(
                    "ต่อเนื่อง ${streakDays.toThaiNumerals()} วัน",
                    style = MaterialTheme.typography.labelMedium,
                    color = ZenColors.BambooDeep,
                )
            }
            Spacer(Modifier.weight(1f))
            Text("เปิดกวาประจำวัน →", style = MaterialTheme.typography.labelMedium, color = ZenColors.CinnabarRed)
        }
    }
    Spacer(Modifier.size(0.dp))
}
