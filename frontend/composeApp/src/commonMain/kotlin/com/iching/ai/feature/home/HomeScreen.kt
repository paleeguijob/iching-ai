package com.iching.ai.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.siamsi.DivinationMode
import com.iching.ai.feature.siamsi.DrawButton
import com.iching.ai.feature.siamsi.SiamSiHomeViewModel
import com.iching.ai.feature.siamsi.SiamSiNav
import com.iching.ai.feature.siamsi.SiamSiSetSheet
import com.iching.ai.feature.siamsi.shortLabel
import com.iching.ai.feature.year.campaignYearLabel
import com.iching.ai.navigation.SiamSiRoute
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.components.ZenCard
import com.iching.ai.ui.components.ZenChip
import com.iching.ai.ui.components.ZenSegmented
import com.iching.ai.ui.theme.LocalTabBarHeight
import com.iching.ai.domain.model.PosterLink
import com.iching.ai.feature.poster.PosterPopupDialog
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.theme.ctaBottomPadding
import com.iching.ai.ui.toThaiNumerals

/**
 * Home: today's hexagram leads; asking a question is the quieter bar pinned at the bottom.
 */
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
    onOpenLink: (PosterLink) -> Unit,
) {
    val daily by viewModel.dailyHexagram.collectAsStateWithLifecycle()
    val mode by siamsi.mode.collectAsStateWithLifecycle()
    val siamsiEnabled by siamsi.enabled.collectAsStateWithLifecycle()
    val siamsiSets by siamsi.siamsiSets.collectAsStateWithLifecycle()
    val selectedSet by siamsi.selectedSet.collectAsStateWithLifecycle()
    val drawGate by siamsi.drawGate.collectAsStateWithLifecycle()
    var setSheetOpen by rememberSaveable { mutableStateOf(false) }
    var question by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    // Once a reading opens, its question is asked: home comes back empty for the next one. The paywall and a
    // skipped ad keep it, since nothing was asked yet.
    val clearQuestion = {
        question = ""
        category = null
    }
    LaunchedEffect(siamsi) {
        siamsi.navigation.collect {
            when (it) {
                is SiamSiNav.Draw -> {
                    onSiamSiDraw(it.route)
                    clearQuestion()
                }
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
        viewModel.castRequests.collect {
            onCast(it.question, it.category)
            clearQuestion()
        }
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
    val popup by viewModel.popup.collectAsStateWithLifecycle()
    // Never over another sheet: the popup waits (still held by the view model) until that one closes.
    if (sheet == null && !setSheetOpen) {
        popup?.let { p ->
            PosterPopupDialog(
                p,
                onTap = { viewModel.onPopupTapped()?.let(onOpenLink) },
                onClose = viewModel::onPopupClosed,
            )
        }
    }
    val campaign by viewModel.campaignCard.collectAsStateWithLifecycle()
    val week by viewModel.streakWeek.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onResumed()
        siamsi.onResumed()
        onPauseOrDispose { }
    }

    val start = {
        if (isSiamSi) {
            // The paywall tap goes through the gate too: it re-checks the day and the crisis words.
            siamsi.onSiamSiStart(question, QuestionCategory.fromWire(category))
        } else {
            viewModel.onStart(question.trim(), QuestionCategory.fromWire(category))
        }
    }

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

                Spacer(Modifier.height(ZenSpacing.sm))
                DailyHeroCard(daily, onClick = onOpenDaily)
                Spacer(Modifier.height(ZenSpacing.md))
                StreakWeekRow(week, onClick = onOpenDaily)

                val card = campaign
                campaignLine(card)?.let { line ->
                    LaunchedEffect(card::class) { viewModel.onCampaignCardShown(card) }
                    CampaignLineRow(line, onClick = {
                        viewModel.onCampaignCardTapped(card)
                        onOpenYear()
                    })
                }
                Spacer(Modifier.height(ZenSpacing.md))
            }
            // Pinned outside the scroll, a gap above the tab bar or the keyboard, whichever is taller; the
            // max() keeps it from dipping behind the bar while the keyboard animates in or out.
            val bottom = ctaBottomPadding(
                imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding(),
                tabBarHeight = LocalTabBarHeight.current,
            )
            AskBar(
                question = question,
                onQuestionChange = { question = it },
                category = category,
                onCategoryChange = { picked ->
                    category = picked?.wire
                    val pickable = QuestionCategory.entries.filter { it.pickable }
                    if (picked != null && (question.isBlank() || pickable.any { it.sampleTh == question })) {
                        question = picked.sampleTh
                    }
                },
                siamsiEnabled = siamsiEnabled,
                isSiamSi = isSiamSi,
                onModeSelect = { siamsi.onModeSelected(if (it) DivinationMode.SIAMSI else DivinationMode.ICHING) },
                send = if (isSiamSi) siamsiSend(drawGate.button) else ichingSend(deciding),
                onSend = start,
                footer = if (isSiamSi) {
                    AskFooter(
                        text = drawGate.line,
                        alert = drawGate.button == DrawButton.PAYWALL || drawGate.button == DrawButton.DISABLED,
                    )
                } else {
                    ichingFooter(guidanceQuota(status))
                },
                setLabel = if (isSiamSi) selectedSet?.shortLabel() else null,
                onOpenSet = { setSheetOpen = true },
                showPremium = isSiamSi && drawGate.button == DrawButton.PAYWALL,
                modifier = Modifier.padding(start = ZenSpacing.screen, end = ZenSpacing.screen, top = ZenSpacing.sm, bottom = bottom),
            )
        }
    }
}

/** The seeker's question box, used by the year question screen. */
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
            colors = quietFieldColors(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
        )
    }
}

@Composable
private fun quietFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTextColor = ZenColors.InkBlack,
    unfocusedTextColor = ZenColors.InkBlack,
    cursorColor = ZenColors.CinnabarRed,
)

/** Spoken label for the Siam Si send button, per spec table 2.2. */
fun siamsiButtonText(button: DrawButton): String = when (button) {
    DrawButton.WATCH_AD -> "▶ ดูโฆษณาแล้วเขย่าเซียมซี"
    DrawButton.SHAKE, DrawButton.DISABLED -> "เขย่าเซียมซี"
    DrawButton.PAYWALL -> "สมัคร Premium เสี่ยงได้ ๑๐ ครั้ง/วัน ไม่มีโฆษณา"
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

/** The year hexagram as one quiet line under today's card; [pending] marks a year not cast yet. */
data class CampaignLine(val text: String, val action: String, val pending: Boolean)

fun campaignLine(card: CampaignCard): CampaignLine? = when (card) {
    CampaignCard.Hidden -> null
    is CampaignCard.New -> CampaignLine("กวาประจำปี${campaignYearLabel(card.year)} · ยังไม่ได้เสี่ยง", action = "เสี่ยง", pending = true)
    is CampaignCard.Cast -> CampaignLine("กวาประจำปีของท่าน: ${card.hexagram.shortNameTh}", action = "ดู", pending = false)
}

@Composable
private fun CampaignLineRow(line: CampaignLine, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (line.pending) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(ZenColors.CinnabarRed))
            Spacer(Modifier.width(ZenSpacing.sm))
        }
        Text(
            line.text,
            style = MaterialTheme.typography.bodySmall,
            color = ZenColors.InkSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text("${line.action} ›", style = MaterialTheme.typography.labelLarge, color = ZenColors.CinnabarRed)
    }
}

/** Today's hexagram, the first thing on home. An empty card of the same size holds its place while it loads. */
@Composable
private fun DailyHeroCard(hexagram: Hexagram?, onClick: () -> Unit) {
    ZenCard(onClick = hexagram?.let { onClick }, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        SectionLabel("กวาประจำวัน", color = ZenColors.CinnabarRed)
        if (hexagram == null) {
            Spacer(Modifier.height(240.dp))
            return@ZenCard
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(ZenSpacing.lg))
            HexagramFigure(hexagram.pattern, lineWidth = 72.dp, lineHeight = 8.dp, gap = 6.dp)
            Spacer(Modifier.height(ZenSpacing.lg))
            Text(
                "${hexagram.id.toThaiNumerals()} · ${hexagram.shortNameTh}",
                style = MaterialTheme.typography.headlineMedium,
                color = ZenColors.InkBlack,
                textAlign = TextAlign.Center,
            )
            Text(hexagram.meaningTh, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft, textAlign = TextAlign.Center)
            if (hexagram.dailyMessage.isNotBlank()) {
                Spacer(Modifier.height(ZenSpacing.md))
                Text(
                    "“${hexagram.dailyMessage}”",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ZenColors.InkBlack,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(ZenSpacing.md))
            Text("อ่านกวาวันนี้ ›", style = MaterialTheme.typography.labelLarge, color = ZenColors.CinnabarRed)
        }
    }
}

/**
 * Seven numbered circles of the current run: ticked once counted, today ringed while it is still waiting, a gift
 * where a theme unlocks. Tapping it opens today's hexagram, which is what counts the day.
 */
@Composable
private fun StreakWeekRow(week: StreakWeek, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            week.days.forEach { day ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val shape = CircleShape
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(shape)
                            .background(if (day.done) ZenColors.CinnabarSoft else Color.Transparent)
                            .border(
                                width = if (day.isToday && !day.done) 1.5.dp else 1.dp,
                                color = when {
                                    day.done -> ZenColors.CinnabarSoft
                                    day.isToday -> ZenColors.CinnabarRed
                                    else -> ZenColors.current.outline
                                },
                                shape = shape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            day.done -> Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            day.milestone -> Icon(
                                Icons.Outlined.CardGiftcard,
                                contentDescription = null,
                                tint = ZenColors.CinnabarRed,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        day.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day.isToday) ZenColors.InkBlack else ZenColors.InkFaint,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            week.caption,
            style = MaterialTheme.typography.bodySmall,
            color = ZenColors.InkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The round send button's look: its icon, spoken label, and whether it can be pressed or is waiting. */
private data class SendUi(val icon: ImageVector, val label: String, val enabled: Boolean, val busy: Boolean = false)

private fun ichingSend(deciding: Boolean) =
    SendUi(Icons.AutoMirrored.Outlined.ArrowForward, startButtonText(deciding), enabled = !deciding, busy = deciding)

private fun siamsiSend(button: DrawButton) = SendUi(
    icon = when (button) {
        DrawButton.WATCH_AD -> Icons.Outlined.PlayArrow
        DrawButton.PAYWALL -> Icons.Outlined.Lock
        DrawButton.SHAKE, DrawButton.DISABLED -> Icons.AutoMirrored.Outlined.ArrowForward
    },
    label = siamsiButtonText(button),
    enabled = button != DrawButton.DISABLED,
)

/** The one line under the ask field. */
private data class AskFooter(val text: String, val alert: Boolean, val note: String? = null)

/** The quota in one line; its note only when nothing is left, where it says what still works. */
private fun ichingFooter(quota: GuidanceQuota) =
    AskFooter(quotaLine(quota), alert = quota.count == 0, note = quota.note.takeIf { quota.count == 0 })

/** Asking, kept quiet: the mode, a short field with a round send button, and the quota. Categories show while typing. */
@Composable
private fun AskBar(
    question: String,
    onQuestionChange: (String) -> Unit,
    category: String?,
    onCategoryChange: (QuestionCategory?) -> Unit,
    siamsiEnabled: Boolean,
    isSiamSi: Boolean,
    onModeSelect: (siamsi: Boolean) -> Unit,
    send: SendUi,
    onSend: () -> Unit,
    footer: AskFooter,
    setLabel: String?,
    onOpenSet: () -> Unit,
    showPremium: Boolean,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("มีเรื่องอยากถาม?", style = MaterialTheme.typography.titleSmall, color = ZenColors.InkBlack, modifier = Modifier.weight(1f))
            if (siamsiEnabled) {
                ZenSegmented(
                    options = listOf("อี้จิง", "เซียมซี"),
                    selected = if (isSiamSi) 1 else 0,
                    onSelect = { onModeSelect(it == 1) },
                    modifier = Modifier.width(160.dp),
                )
            }
        }
        if (focused || category != null) {
            Spacer(Modifier.height(ZenSpacing.sm))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuestionCategory.entries.filter { it.pickable }.forEach { c ->
                    val selected = category == c.wire
                    ZenChip(c.labelTh, selected, onClick = { onCategoryChange(if (selected) null else c) })
                }
            }
        }
        Spacer(Modifier.height(ZenSpacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val shape = RoundedCornerShape(24.dp)
            Box(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(ZenColors.Card)
                    .border(1.dp, ZenColors.current.outlineVariant, shape),
            ) {
                TextField(
                    value = question,
                    onValueChange = { if (it.length <= 300) onQuestionChange(it) },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                    placeholder = {
                        Text(
                            if (isSiamSi) "เรื่องที่อยากถาม (ไม่บังคับ)…" else "พิมพ์คำถาม หรือเว้นว่างแล้วกดเสี่ยง",
                            color = ZenColors.InkFaint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = quietFieldColors(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
                )
            }
            Spacer(Modifier.width(ZenSpacing.sm))
            SendButton(send, onSend)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                footer.text,
                style = MaterialTheme.typography.bodySmall,
                color = if (footer.alert) ZenColors.CinnabarRed else ZenColors.InkSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            when {
                showPremium -> FooterLink("สมัคร Premium ›", onSend)
                setLabel != null -> FooterLink("ชุด: $setLabel ›", onOpenSet)
            }
        }
        footer.note?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = ZenColors.InkFaint,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = ZenColors.CinnabarRed,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(start = ZenSpacing.sm)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
    )
}

@Composable
private fun SendButton(send: SendUi, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (send.enabled || send.busy) ZenColors.InkBlack else ZenColors.InkFaint)
            .clickable(enabled = send.enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = send.label },
        contentAlignment = Alignment.Center,
    ) {
        if (send.busy) {
            CircularProgressIndicator(Modifier.size(20.dp), color = ZenColors.PaperCream, strokeWidth = 2.dp)
        } else {
            Icon(send.icon, contentDescription = null, tint = ZenColors.PaperCream)
        }
    }
}
