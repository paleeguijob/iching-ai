package com.iching.ai.feature.home

import com.iching.ai.ui.theme.ctaBottomPadding
import com.iching.ai.ui.theme.LocalTabBarHeight
import androidx.compose.foundation.layout.ime
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.components.ZenSecondaryButton
import com.iching.ai.ui.components.ZenChip
import com.iching.ai.ui.components.ZenCard
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
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
import com.iching.ai.domain.siamsi.DivinationMode
import com.iching.ai.feature.siamsi.DrawButton
import com.iching.ai.feature.siamsi.SiamSiHomeViewModel
import com.iching.ai.feature.siamsi.SiamSiNav
import com.iching.ai.feature.siamsi.SiamSiSetSheet
import com.iching.ai.feature.siamsi.shortLabel
import com.iching.ai.navigation.SiamSiRoute
import com.iching.ai.ui.components.ZenSegmented
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
    siamsi: SiamSiHomeViewModel,
    onSiamSiDraw: (SiamSiRoute) -> Unit,
) {
    val daily by viewModel.dailyHexagram.collectAsStateWithLifecycle()
    val mode by siamsi.mode.collectAsStateWithLifecycle()
    val siamsiEnabled by siamsi.enabled.collectAsStateWithLifecycle()
    val siamsiSets by siamsi.siamsiSets.collectAsStateWithLifecycle()
    val selectedSet by siamsi.selectedSet.collectAsStateWithLifecycle()
    val drawGate by siamsi.drawGate.collectAsStateWithLifecycle()
    var setSheetOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(siamsi) {
        siamsi.navigation.collect {
            when (it) {
                is SiamSiNav.Draw -> onSiamSiDraw(it.route)
                SiamSiNav.Paywall -> onOpenPaywall()
            }
        }
    }
    if (setSheetOpen && siamsiSets.isNotEmpty()) {
        SiamSiSetSheet(siamsiSets, selectedSet?.id, onSelect = siamsi::onSetSelected, onDismiss = { setSheetOpen = false })
    }
    val isSiamSi = mode == DivinationMode.SIAMSI
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
        siamsi.onResumed()
        onPauseOrDispose { }
    }
    var question by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }

    val pickable = QuestionCategory.entries.filter { it.pickable }
    PaperBackground(showMountains = true) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ZenSpacing.screen),
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CinnabarSeal(text = "易", size = 24.dp, tilt = 0f)
                    Spacer(Modifier.width(10.dp))
                    Text("อี้จิง", style = MaterialTheme.typography.titleMedium, color = ZenColors.InkBlack)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "ตั้งค่า", tint = ZenColors.InkSoft)
                    }
                }

                Spacer(Modifier.height(ZenSpacing.md))
                daily?.let {
                    DailyHexagramCard(it, streakDays, onClick = onOpenDaily)
                    Spacer(Modifier.height(ZenSpacing.md))
                }
                val card = campaign
                if (card != CampaignCard.Hidden) {
                    LaunchedEffect(card::class) { viewModel.onCampaignCardShown(card) }
                    CampaignHomeCard(card, onClick = {
                        viewModel.onCampaignCardTapped(card)
                        onOpenYear()
                    })
                    Spacer(Modifier.height(ZenSpacing.md))
                }

                if (siamsiEnabled) {
                    ZenSegmented(
                        options = listOf("อี้จิง", "เซียมซี"),
                        selected = if (isSiamSi) 1 else 0,
                        onSelect = { siamsi.onModeSelected(if (it == 1) DivinationMode.SIAMSI else DivinationMode.ICHING) },
                    )
                    Spacer(Modifier.height(ZenSpacing.md))
                }
                if (isSiamSi) {
                    selectedSet?.let { set -> SetRow(set.shortLabel(), onClick = { setSheetOpen = true }) }
                }

                Spacer(Modifier.height(ZenSpacing.lg))
                Text(
                    if (isSiamSi) "อธิษฐานแล้วเสี่ยงเซียมซี" else "ถามทางแห่งเต๋า",
                    style = MaterialTheme.typography.displaySmall,
                    color = ZenColors.InkBlack,
                )
                Text(
                    if (isSiamSi) "ตั้งจิตถึงสิ่งที่อยากรู้ แล้วเขย่าเบา ๆ" else "สงบจิต ตั้งคำถามด้วยใจที่เปิดกว้าง",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZenColors.InkSoft,
                )

                Spacer(Modifier.height(ZenSpacing.lg))
                if (isSiamSi) {
                    QuestionField(question, onChange = { question = it }, placeholder = "เรื่องที่อยากถาม…")
                    Text(
                        "ไม่บังคับ — เว้นว่างได้",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZenColors.InkFaint,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                } else {
                    QuestionField(question, onChange = { question = it })
                }
                Spacer(Modifier.height(ZenSpacing.md))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pickable.forEach { c ->
                        val selected = category == c.wire
                        ZenChip(c.labelTh, selected, onClick = {
                            if (selected) {
                                category = null
                            } else {
                                category = c.wire
                                if (question.isBlank() || pickable.any { it.sampleTh == question }) question = c.sampleTh
                            }
                        })
                    }
                }
                Spacer(Modifier.height(ZenSpacing.lg))
                if (isSiamSi) {
                    Text(
                        drawGate.line,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (drawGate.button == DrawButton.PAYWALL || drawGate.button == DrawButton.DISABLED) ZenColors.CinnabarRed else ZenColors.InkSoft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    GuidanceQuotaBar(guidanceQuota(status))
                }
                Spacer(Modifier.height(ZenSpacing.md))
            }
            // Pinned outside the scroll, a gap above the tab bar or the keyboard, whichever is taller; the
            // max() keeps it from dipping behind the bar while the keyboard animates in or out.
            val bottom = ctaBottomPadding(
                imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding(),
                tabBarHeight = LocalTabBarHeight.current,
            )
            ZenPrimaryButton(
                text = if (isSiamSi) siamsiButtonText(drawGate.button) else startButtonText(deciding),
                onClick = {
                    when {
                        !isSiamSi -> viewModel.onStart(question.trim(), QuestionCategory.fromWire(category))
                        // The paywall tap goes through the gate too: it re-checks the day and the crisis words.
                        else -> siamsi.onSiamSiStart(question, QuestionCategory.fromWire(category))
                    }
                },
                enabled = if (isSiamSi) drawGate.button != DrawButton.DISABLED else !deciding,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ZenSpacing.screen, end = ZenSpacing.screen, top = ZenSpacing.sm, bottom = bottom),
            )
        }
    }
}

/** The seeker's question box, shared by the home screen and the year question screen. */
@Composable
internal fun QuestionField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "พิมพ์สิ่งที่ค้างคาใจ หรือเว้นว่างไว้เพื่ออธิษฐานในใจ...",
) {
    ZenCard(contentPadding = PaddingValues(4.dp)) {
        TextField(
            value = value,
            onValueChange = { if (it.length <= 300) onChange(it) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            placeholder = {
                Text(placeholder, color = ZenColors.InkFaint)
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = ZenColors.InkBlack,
                unfocusedTextColor = ZenColors.InkBlack,
                cursorColor = ZenColors.CinnabarRed,
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
        )
    }
}

/** The main button in Siam Si mode, per spec table 2.2. */
fun siamsiButtonText(button: DrawButton): String = when (button) {
    DrawButton.WATCH_AD -> "▶ ดูโฆษณาแล้วเขย่าเซียมซี"
    DrawButton.SHAKE, DrawButton.DISABLED -> "เขย่าเซียมซี"
    DrawButton.PAYWALL -> "สมัคร Premium เสี่ยงได้ ๑๐ ครั้ง/วัน ไม่มีโฆษณา"
}

/** "ชุด · เจ้าแม่กวนอิม · ๑๐๐ ใบ ›": opens the set sheet. */
@Composable
private fun SetRow(label: String, onClick: () -> Unit) {
    ZenCard(onClick = onClick, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("ชุด")
            Spacer(Modifier.width(ZenSpacing.sm))
            Text(label, style = MaterialTheme.typography.titleSmall, color = ZenColors.InkBlack, modifier = Modifier.weight(1f))
            Text("›", style = MaterialTheme.typography.titleLarge, color = ZenColors.InkFaint)
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

/** One line for the quota, e.g. "วันนี้เหลือคำชี้แนะจากนักพรต ๒ ครั้ง". */
internal fun quotaLine(quota: GuidanceQuota): String =
    quota.count?.let { "${quota.label} ${it.toThaiNumerals()} ครั้ง" } ?: quota.label

/** Quiet centred quota text above the Start button; the note, when there is one, goes underneath. */
@Composable
internal fun GuidanceQuotaBar(quota: GuidanceQuota) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            quotaLine(quota),
            style = MaterialTheme.typography.bodySmall,
            color = if (quota.count == 0) ZenColors.CinnabarRed else ZenColors.InkSoft,
            textAlign = TextAlign.Center,
        )
        quota.note?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkFaint, textAlign = TextAlign.Center)
        }
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
    ZenCard {
        Text(title, style = MaterialTheme.typography.titleMedium, color = ZenColors.InkBlack)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
        }
        Spacer(Modifier.height(ZenSpacing.md))
        ZenSecondaryButton(text = button, onClick = onClick, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DailyHexagramCard(hexagram: Hexagram, streakDays: Int, onClick: () -> Unit) {
    ZenCard(onClick = onClick, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HexagramFigure(hexagram.pattern, lineWidth = 30.dp, lineHeight = 4.dp, gap = 3.dp)
            Spacer(Modifier.width(ZenSpacing.md))
            Column(Modifier.weight(1f)) {
                SectionLabel(
                    if (streakDays > 0) "กวาประจำวัน · ต่อเนื่อง ${streakDays.toThaiNumerals()} วัน" else "กวาประจำวัน",
                )
                Text(
                    "${hexagram.id}. ${hexagram.shortNameTh} · ${hexagram.meaningTh}",
                    style = MaterialTheme.typography.titleSmall,
                    color = ZenColors.InkBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = ZenColors.InkFaint)
        }
    }
}
