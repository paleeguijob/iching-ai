package com.iching.ai.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.PosterLink
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.siamsi.DivinationMode
import com.iching.ai.feature.poster.PosterPopupDialog
import com.iching.ai.feature.siamsi.DrawButton
import com.iching.ai.feature.siamsi.DrawGateUi
import com.iching.ai.feature.siamsi.SiamSiCylinder
import com.iching.ai.feature.siamsi.SiamSiHomeViewModel
import com.iching.ai.feature.siamsi.SiamSiNav
import com.iching.ai.feature.siamsi.SiamSiSetSheet
import com.iching.ai.feature.siamsi.shortLabel
import com.iching.ai.navigation.SiamSiRoute
import com.iching.ai.ui.components.AncientCoin
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.ZenCard
import com.iching.ai.ui.components.ZenChip
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenSegmented
import com.iching.ai.ui.theme.LocalTabBarHeight
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing
import kotlinx.coroutines.launch
import com.iching.ai.ui.theme.ctaBottomPadding
import com.iching.ai.ui.toThaiNumerals

/**
 * Home: asking leads. Three coins sit in the middle and start the cast, today's hexagram is one row above them,
 * and the question, what guidance is left and the Start button sit at the bottom. Typing hides the coins.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCast: (String, QuestionCategory?) -> Unit,
    onOpenDaily: () -> Unit,
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
            LaunchedEffect(p.id) { viewModel.onPopupDisplayed(p.id) }
            PosterPopupDialog(
                p,
                onTap = { viewModel.onPopupTapped()?.let(onOpenLink) },
                onClose = viewModel::onPopupClosed,
            )
        }
    }
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
    val action = if (isSiamSi) siamsiAction(drawGate.button) else ichingAction(deciding)
    var focused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    // Typing while the keyboard is up; a keyboard closed with Back leaves the field focused but brings the coins back.
    val typing = focused && imeBottom > 0.dp
    val showChips = !isSiamSi && (typing || category != null)

    PaperBackground(showMountains = true) {
        // The bottom group sits a gap above the tab bar or the keyboard, whichever is taller; the max() keeps
        // it from dipping behind the bar while the keyboard animates in or out.
        val bottom = ctaBottomPadding(imeBottom = imeBottom, tabBarHeight = LocalTabBarHeight.current)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = ZenSpacing.screen, end = ZenSpacing.screen, bottom = bottom),
        ) {
            HomeTopBar(
                siamsiEnabled = siamsiEnabled,
                isSiamSi = isSiamSi,
                onModeSelect = { siamsi.onModeSelected(if (it) DivinationMode.SIAMSI else DivinationMode.ICHING) },
                onOpenSettings = onOpenSettings,
            )
            if (!typing) {
                Spacer(Modifier.height(ZenSpacing.sm))
                DailyStrip(daily, pending = week.days.any { it.isToday && !it.done }, onClick = onOpenDaily)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (!typing) {
                    Ritual(
                        isSiamSi = isSiamSi,
                        action = action,
                        onStart = start,
                        setLabel = if (isSiamSi) selectedSet?.shortLabel() else null,
                        onOpenSet = { setSheetOpen = true },
                    )
                }
            }
            if (showChips) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuestionCategory.entries.filter { it.pickable }.forEach { c ->
                        val selected = category == c.wire
                        ZenChip(c.labelTh, selected, onClick = {
                            category = if (selected) null else c.wire
                            val pickable = QuestionCategory.entries.filter { it.pickable }
                            if (!selected && (question.isBlank() || pickable.any { it.sampleTh == question })) {
                                question = c.sampleTh
                            }
                        })
                    }
                }
                Spacer(Modifier.height(ZenSpacing.sm))
            }
            AskField(
                question = question,
                onQuestionChange = { question = it },
                placeholder = if (isSiamSi) "เรื่องที่อยากถาม (ไม่บังคับ)…" else "พิมพ์คำถาม หรือเว้นว่างไว้อธิษฐานในใจ",
                onFocusChange = { focused = it },
                expanded = typing,
            )
            Spacer(Modifier.height(ZenSpacing.sm))
            if (isSiamSi) {
                SiamSiQuotaBar(drawGate)
            } else {
                GuidanceQuotaBar(guidanceQuota(status), compact = typing)
            }
            Spacer(Modifier.height(ZenSpacing.sm))
            ZenPrimaryButton(
                text = action.label,
                onClick = start,
                enabled = action.enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The seal and name, the I Ching / Siam Si switch when Siam Si is on, and settings. */
@Composable
private fun HomeTopBar(
    siamsiEnabled: Boolean,
    isSiamSi: Boolean,
    onModeSelect: (siamsi: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        CinnabarSeal(text = "易", size = 24.dp, tilt = 0f)
        Spacer(Modifier.width(10.dp))
        Text(
            "อี้จิง",
            style = MaterialTheme.typography.titleMedium,
            color = ZenColors.InkBlack,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (siamsiEnabled) {
            ZenSegmented(
                options = listOf("อี้จิง", "เซียมซี"),
                selected = if (isSiamSi) 1 else 0,
                onSelect = { onModeSelect(it == 1) },
                modifier = Modifier.width(156.dp),
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = "ตั้งค่า", tint = ZenColors.InkSoft)
        }
    }
}

/**
 * Today's hexagram as one row. A red dot says it has not been opened today, which is what keeps the streak;
 * an empty row of the same height holds its place while it loads.
 */
@Composable
private fun DailyStrip(hexagram: Hexagram?, pending: Boolean, onClick: () -> Unit) {
    ZenCard(onClick = hexagram?.let { onClick }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            if (hexagram == null) {
                Box(Modifier.size(width = 28.dp, height = 36.dp).clip(RoundedCornerShape(4.dp)).background(ZenColors.PaperDeep))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.4f).height(10.dp).clip(CircleShape).background(ZenColors.PaperDeep))
                    Box(Modifier.fillMaxWidth(0.75f).height(10.dp).clip(CircleShape).background(ZenColors.PaperDeep))
                }
                return@Row
            }
            HexagramFigure(hexagram.pattern, lineWidth = 28.dp, lineHeight = 4.dp, gap = 3.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pending) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(ZenColors.CinnabarRed))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        if (pending) "กวาประจำวัน · ยังไม่ได้เปิดดู" else "กวาประจำวัน",
                        style = MaterialTheme.typography.labelMedium,
                        color = ZenColors.CinnabarRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "${hexagram.id.toThaiNumerals()} · ${hexagram.shortNameTh} ${hexagram.meaningTh}",
                    style = MaterialTheme.typography.titleSmall,
                    color = ZenColors.InkBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = ZenColors.InkFaint)
        }
    }
}

/**
 * The middle of home: a line asking what is on the seeker's mind, then the three coins (or the Siam Si cylinder)
 * that start the cast like the button does. On a short screen the caption goes first, then the coins shrink.
 */
@Composable
private fun Ritual(isSiamSi: Boolean, action: HomeAction, onStart: () -> Unit, setLabel: String?, onOpenSet: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val roomy = maxHeight >= 250.dp
        val tight = maxHeight < 190.dp
        val spacious = maxHeight >= 420.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ZenSpacing.md)) {
            Text(
                "มีเรื่องใดค้างใจ",
                style = if (tight) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                color = ZenColors.InkBlack,
                textAlign = TextAlign.Center,
            )
            // A tap on the art starts only what costs nothing. When the next step is an ad or the paywall it
            // nudges instead and points at the labelled button, so a stray tap never opens an ad.
            val hint = action.artHint
            val scope = rememberCoroutineScope()
            val wobble = remember { Animatable(0f) }
            var hintShown by remember { mutableStateOf(false) }
            LaunchedEffect(hint) { hintShown = false }
            val onArtTap: () -> Unit = {
                if (hint == null) {
                    onStart()
                } else {
                    hintShown = true
                    scope.launch {
                        wobble.snapTo(0f)
                        for (angle in listOf(-6f, 5f, -3f, 0f)) wobble.animateTo(angle, tween(70))
                    }
                }
            }
            val tapModifier = Modifier
                .clip(RoundedCornerShape(60.dp))
                .clickable(enabled = action.enabled, role = Role.Button, onClick = onArtTap)
                .semantics {
                    contentDescription = hint ?: action.label
                    role = Role.Button
                }
                .alpha(if (action.enabled) 1f else 0.45f)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .graphicsLayer { rotationZ = wobble.value }
            if (isSiamSi) {
                SmallCylinder(height = if (tight) 96.dp else if (spacious) 200.dp else 150.dp, modifier = tapModifier)
                if (setLabel != null) SetButton(setLabel, onOpenSet)
                ArtHint(hint, visible = hint != null && hintShown)
            } else {
                CoinTrio(size = if (tight) 52.dp else if (spacious) 88.dp else 72.dp, modifier = tapModifier)
                if (roomy) {
                    Text(
                        "ตั้งจิตถึงเรื่องนั้น แล้วแตะเหรียญเพื่อโยน ๖ ครั้ง",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZenColors.InkSoft,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** The line under the art after a tap that did not start: which button to press instead. */
@Composable
private fun ArtHint(hint: String?, visible: Boolean) {
    // Holds its height so the art does not jump when the line appears.
    Box(Modifier.heightIn(min = 20.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
            Text(hint.orEmpty(), style = MaterialTheme.typography.bodySmall, color = ZenColors.InkSoft, textAlign = TextAlign.Center)
        }
    }
}

/** The same bronze coins the toss screen throws, resting at slightly different angles. */
@Composable
private fun CoinTrio(size: Dp, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        AncientCoin(face = null, tossKey = 0, size = size, modifier = Modifier.rotate(-8f))
        AncientCoin(face = null, tossKey = 0, size = size, modifier = Modifier.offset(y = -(size / 7)).rotate(14f))
        AncientCoin(face = null, tossKey = 0, size = size, modifier = Modifier.rotate(-14f))
    }
}

/** The Siam Si screen's cylinder drawn small; on home it only starts the draw, so its own hold-to-shake is off. */
@Composable
private fun SmallCylinder(height: Dp, modifier: Modifier = Modifier) {
    // The cylinder lays itself out at its full size; it is scaled down to [height] here.
    val full = 370.dp
    val scale = height / full
    Box(modifier.size(width = height * 0.62f, height = height), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .requiredSize(width = 240.dp, height = full)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clearAndSetSemantics { },
        ) {
            SiamSiCylinder(shaking = false, onHoldChange = {})
        }
    }
}

@Composable
private fun SetButton(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .border(1.dp, ZenColors.current.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("ชุด: $label", style = MaterialTheme.typography.labelMedium, color = ZenColors.InkBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = ZenColors.InkSoft, modifier = Modifier.size(18.dp))
    }
}

/** The question box: one line at rest, a few lines while typing. */
@Composable
private fun AskField(
    question: String,
    onQuestionChange: (String) -> Unit,
    placeholder: String,
    onFocusChange: (Boolean) -> Unit,
    expanded: Boolean,
) {
    val shape = RoundedCornerShape(if (expanded) 20.dp else 26.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ZenColors.Card)
            .border(if (expanded) 1.5.dp else 1.dp, if (expanded) ZenColors.InkBlack else ZenColors.current.outlineVariant, shape),
    ) {
        TextField(
            value = question,
            onValueChange = { if (it.length <= 300) onQuestionChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (expanded) 88.dp else 52.dp)
                .onFocusChanged { onFocusChange(it.isFocused) },
            placeholder = {
                Text(placeholder, color = ZenColors.InkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            maxLines = if (expanded) 4 else 1,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = quietFieldColors(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
        )
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
    DrawButton.PAYWALL -> "สมัคร Premium เสี่ยงได้ ๓ ครั้ง/วัน ไม่มีโฆษณา"
}

/** The Start button waits, with a word, while the status is checked before casting. */
fun startButtonText(deciding: Boolean): String = if (deciding) "กำลังตั้งจิต..." else "เริ่มเสี่ยงทาย"

/** What the Start button and the coins say and whether they can be pressed. */
/** [artHint] is set when tapping the art must not start: the line shown instead, naming the button to press. */
private data class HomeAction(val label: String, val enabled: Boolean, val artHint: String? = null)

private fun ichingAction(deciding: Boolean) = HomeAction(startButtonText(deciding), enabled = !deciding)

private fun siamsiAction(button: DrawButton) =
    HomeAction(siamsiButtonText(button), enabled = button != DrawButton.DISABLED, artHint = siamsiArtHint(button))

/**
 * What a tap on the cylinder says instead of starting, or null when it may start the draw. A rewarded ad must be
 * chosen on purpose and home never upsells, so only the labelled button leads to either.
 */
fun siamsiArtHint(button: DrawButton): String? = when (button) {
    DrawButton.WATCH_AD -> "กดปุ่มด้านล่างเพื่อดูโฆษณาแล้วเขย่า"
    DrawButton.PAYWALL -> "วันนี้เสี่ยงครบแล้ว กลับมาใหม่พรุ่งนี้"
    DrawButton.SHAKE, DrawButton.DISABLED -> null
}

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

/**
 * What guidance is left, shown before Start and never pressable: the words on the left, the count large on the
 * right in bamboo green, or cinnabar at zero. [compact] drops the note while the keyboard is up.
 */
@Composable
internal fun GuidanceQuotaBar(quota: GuidanceQuota, compact: Boolean = false) {
    // The AI-paused label carries its note after the dash; it reads better as heading and note.
    val (label, note) = quota.label.split(" — ", limit = 2).let { parts ->
        if (parts.size == 2) parts[0] to parts[1] else quota.label to quota.note
    }
    QuotaPanel(
        label = label,
        note = note.takeUnless { compact },
        count = quota.count,
        alert = quota.count == 0,
        compact = compact,
        spoken = quotaLine(quota),
    )
}

/** The Siam Si draw line in the same panel; no count, since the gate words already say it. */
@Composable
private fun SiamSiQuotaBar(gate: DrawGateUi) {
    val alert = gate.button == DrawButton.PAYWALL || gate.button == DrawButton.DISABLED
    QuotaPanel(label = gate.line, note = null, count = null, alert = alert, compact = false, spoken = gate.line)
}

@Composable
private fun QuotaPanel(label: String, note: String?, count: Int?, alert: Boolean, compact: Boolean, spoken: String) {
    val shape = RoundedCornerShape(14.dp)
    val edge = if (alert) ZenColors.CinnabarRed.copy(alpha = 0.35f) else ZenColors.current.outlineVariant
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ZenColors.PaperMid)
            .border(1.dp, edge, shape)
            .padding(horizontal = 14.dp, vertical = if (compact) 6.dp else 10.dp)
            .clearAndSetSemantics { contentDescription = listOfNotNull(spoken, note).joinToString(" ") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (alert) ZenColors.CinnabarRed else ZenColors.InkBlack,
            )
            note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkSoft)
            }
        }
        count?.let {
            Spacer(Modifier.width(ZenSpacing.md))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    it.toThaiNumerals(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 22.sp else 32.sp,
                        lineHeight = if (compact) 26.sp else 36.sp,
                    ),
                    color = if (it == 0) ZenColors.CinnabarRed else ZenColors.BambooDeep,
                )
                Spacer(Modifier.width(4.dp))
                Text("ครั้ง", style = MaterialTheme.typography.bodySmall, color = ZenColors.InkSoft, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}
