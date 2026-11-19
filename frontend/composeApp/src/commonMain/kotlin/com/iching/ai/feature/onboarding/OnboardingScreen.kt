package com.iching.ai.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.iching.ai.data.BuildConfig
import com.iching.ai.domain.model.CoinFace
import com.iching.ai.domain.model.CoinToss
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.ui.components.AncientCoin
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.ZenCard
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.toThaiNumerals
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val POINTS = listOf(
    "เพื่อความบันเทิงและการสะท้อนคิด" to
        "คำทำนายช่วยให้ท่านใคร่ครวญสถานการณ์ มิใช่คำแนะนำทางการแพทย์ กฎหมาย การเงิน หรือจิตวิทยา โปรดใช้วิจารณญาณก่อนตัดสินใจเรื่องสำคัญ",
    "เนื้อหาสร้างโดย AI" to
        "นักพรตในแอปนี้คือผู้ช่วย AI (Google Gemini) ที่สวมบทบาทและอ้างอิงตัวบทคัมภีร์อี้จิงจริง คำตอบอาจคลาดเคลื่อนได้",
    "ความเป็นส่วนตัว" to
        "คำถามของท่านจะถูกส่งไปประมวลผลที่ Google Gemini โปรดอย่าพิมพ์ข้อมูลที่ระบุตัวตน เช่น เลขบัตร ที่อยู่ หรือข้อมูลสุขภาพ",
    "ยามทุกข์ใจ" to
        "หากท่านกำลังรู้สึกไม่ปลอดภัยหรือคิดทำร้ายตนเอง โปรดโทรสายด่วนสุขภาพจิต 1323 หรือเหตุฉุกเฉิน 1669",
)

/** The three things a newcomer needs before the first cast: what to ask, how the coins work, what comes back. */
private enum class IntroPage { ASK, CAST, READ }

/**
 * First launch: three short pages on how a reading works, then the disclaimer, which must be accepted
 * before the first reading. Skip goes straight to the disclaimer; it never skips the disclaimer itself.
 */
@Composable
fun OnboardingScreen(onAccept: () -> Unit) {
    IntroFlow(withDisclaimer = true, onFinish = onAccept, onClose = null)
}

/** The same three pages opened again from settings; no disclaimer, and closing returns to settings. */
@Composable
fun GuideScreen(onClose: () -> Unit) {
    IntroFlow(withDisclaimer = false, onFinish = onClose, onClose = onClose)
}

@Composable
private fun IntroFlow(withDisclaimer: Boolean, onFinish: () -> Unit, onClose: (() -> Unit)?) {
    val intro = IntroPage.entries
    val pageCount = intro.size + if (withDisclaimer) 1 else 0
    val pager = rememberPagerState { pageCount }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage
    val last = page == pageCount - 1
    val goTo = { target: Int -> scope.launch { pager.animateScrollToPage(target) } }

    // System Back walks back through the pages; on the first page it leaves (settings) or does nothing special.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = page > 0,
        onBackCompleted = { goTo(page - 1) },
    )

    PaperBackground(showMountains = true) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "กลับ", tint = ZenColors.InkBlack)
                    }
                } else {
                    Spacer(Modifier.width(ZenSpacing.lg))
                    CinnabarSeal(text = "易", size = 24.dp, tilt = 0f)
                    Spacer(Modifier.width(10.dp))
                    Text("อี้จิง", style = MaterialTheme.typography.titleMedium, color = ZenColors.InkBlack)
                }
                Spacer(Modifier.weight(1f))
                // Skip lands on the disclaimer when there is one, else it closes; hidden where it would do nothing.
                val skipTarget = if (withDisclaimer) pageCount - 1 else null
                if (!last && (skipTarget != null || onClose != null)) {
                    TextButton(
                        onClick = { if (skipTarget != null) goTo(skipTarget) else onClose?.invoke() },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("ข้าม", style = MaterialTheme.typography.labelLarge, color = ZenColors.InkSoft)
                    }
                }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                beyondViewportPageCount = 1,
            ) { index ->
                val introPage = intro.getOrNull(index)
                if (introPage == null) {
                    DisclaimerPage()
                } else {
                    IntroPageContent(introPage, active = pager.currentPage == index)
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = ZenSpacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StepMarks(count = pageCount, current = page)
                Spacer(Modifier.height(ZenSpacing.lg))
                ZenPrimaryButton(
                    text = when {
                        !last -> "ถัดไป"
                        withDisclaimer -> "รับทราบและเริ่มต้น"
                        else -> "เข้าใจแล้ว"
                    },
                    onClick = { if (last) onFinish() else goTo(page + 1) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(ZenSpacing.md))
            }
        }
    }
}

/** One short ink stroke per page; the current one is longer and inked, the rest faint. */
@Composable
private fun StepMarks(count: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = "หน้า ${(current + 1).toThaiNumerals()} จาก ${count.toThaiNumerals()}" },
    ) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 22.dp else 8.dp, tween(260), label = "mark")
            val color by animateColorAsState(
                if (i <= current) ZenColors.InkBlack else ZenColors.current.outline,
                tween(260),
                label = "markColor",
            )
            Box(Modifier.size(width = width, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

@Composable
private fun IntroPageContent(page: IntroPage, active: Boolean) {
    val (title, body) = when (page) {
        IntroPage.ASK -> "ถามเรื่องที่ค้างใจ" to
            "อี้จิงคือคัมภีร์โบราณของจีน ที่ผู้คนใช้ถามทางในยามลังเล\nพิมพ์คำถามของท่าน\nหรือเว้นว่างไว้แล้วอธิษฐานในใจก็ได้"
        IntroPage.CAST -> "โยนเหรียญหกครั้ง" to
            "เหรียญสามเหรียญ โยนหนึ่งครั้งได้หนึ่งเส้น\nต่อขึ้นจากล่างทีละเส้น\nครบหกเส้นเรียกว่า “กวา” หนึ่งใน ๖๔ กวาของคัมภีร์"
        IntroPage.READ -> "อ่านคำตอบสองชั้น" to
            "ชั้นแรก คือตัวบทคัมภีร์ของกวาที่ท่านได้\nชั้นที่สอง คือคำชี้แนะจากนักพรต\nซึ่ง AI เรียบเรียงจากตัวบท\nให้ตรงกับคำถามของท่าน"
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 520.dp
        // Centred in the space between the top bar and the button; scrolls instead when the font is large.
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = ZenSpacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = if (compact) 220.dp else 300.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (page) {
                    IntroPage.ASK -> AskVisual(active)
                    IntroPage.CAST -> CastVisual(compact)
                    IntroPage.READ -> ReadVisual()
                }
            }
            Spacer(Modifier.height(ZenSpacing.lg))
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = ZenColors.InkBlack,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(ZenSpacing.md))
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = ZenColors.InkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 420.dp),
            )
            if (page == IntroPage.READ) {
                Spacer(Modifier.height(ZenSpacing.md))
                Text(
                    "เสี่ยงทายและอ่านตัวบทได้เสมอ\nส่วนคำชี้แนะจากนักพรตมีจำนวนต่อวัน\nหน้าแรกบอกที่เหลือไว้ก่อนท่านเริ่มทุกครั้ง",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenColors.InkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 420.dp),
                )
            }
            Spacer(Modifier.height(ZenSpacing.lg))
        }
    }
}

/** The question box as home shows it, cycling through the sample questions of each category. */
@Composable
private fun AskVisual(active: Boolean) {
    val samples = remember { QuestionCategory.entries.filter { it.pickable } }
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        while (active) {
            delay(2600)
            index = (index + 1) % (samples.size + 1)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CinnabarSeal(text = "易經", size = 72.dp)
        Spacer(Modifier.height(ZenSpacing.xl))
        val shape = RoundedCornerShape(26.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .heightIn(min = 52.dp)
                .clip(shape)
                .background(ZenColors.Card)
                .border(1.dp, ZenColors.current.outlineVariant, shape)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            // The last step of the cycle is the empty box: asking without words is allowed.
            AnimatedContent(
                targetState = index,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
                label = "sample",
            ) { i ->
                val sample = samples.getOrNull(i)
                Text(
                    sample?.sampleTh ?: "เว้นว่างไว้ อธิษฐานในใจ…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (sample != null) ZenColors.InkBlack else ZenColors.InkFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(ZenSpacing.sm))
        AnimatedContent(
            targetState = samples.getOrNull(index)?.labelTh,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
            label = "category",
        ) { label ->
            Text(
                label?.let { "เรื่อง$it" } ?: "ไม่ต้องเอ่ยคำถามก็ได้",
                style = MaterialTheme.typography.labelMedium,
                color = ZenColors.BambooDeep,
            )
        }
    }
}

/**
 * A practice cast: each tap throws the three coins and inks one line, bottom up, with what that line is called.
 * Nothing is saved or charged; six lines make a hexagram, and a further tap starts over.
 */
@Composable
private fun CastVisual(compact: Boolean) {
    var tosses by remember { mutableStateOf(emptyList<CoinToss>()) }
    var tossKey by remember { mutableIntStateOf(0) }
    var settling by remember { mutableStateOf(false) }
    LaunchedEffect(tossKey) {
        if (tossKey > 0) {
            settling = true
            delay(700)
            settling = false
        }
    }
    val done = tosses.size == 6
    val toss = {
        if (!settling) {
            tosses = if (done) emptyList() else tosses + CoinToss(List(3) { if (Random.nextBoolean()) CoinFace.HEADS else CoinFace.TAILS })
            tossKey++
        }
    }
    val coinSize: Dp = if (compact) 52.dp else 64.dp
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = "โยนเหรียญ", onClick = toss)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HexagramFigure(
            pattern = null,
            lines = tosses.map { it.line } + List(6 - tosses.size) { LineType.YOUNG_YANG },
            revealed = tosses.size,
            animateReveal = true,
            lineWidth = if (compact) 96.dp else 120.dp,
            lineHeight = if (compact) 9.dp else 11.dp,
            gap = if (compact) 6.dp else 8.dp,
        )
        Spacer(Modifier.height(ZenSpacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val faces = tosses.lastOrNull()?.faces
            repeat(3) { i ->
                AncientCoin(
                    face = faces?.get(i),
                    tossKey = tossKey,
                    delayMillis = i * 90 + if (i == 1) 80 else 0,
                    size = coinSize,
                    modifier = if (tosses.isEmpty()) Modifier.rotate(listOf(-8f, 14f, -14f)[i]) else Modifier,
                )
            }
        }
        Spacer(Modifier.height(ZenSpacing.md))
        Box(Modifier.heightIn(min = 22.dp), contentAlignment = Alignment.Center) {
            val line = tosses.lastOrNull()?.line
            Text(
                when {
                    tosses.isEmpty() -> "ลองแตะเหรียญดู"
                    settling -> "…"
                    done -> "ครบหกเส้น ได้หนึ่งกวา · แตะเพื่อลองใหม่"
                    line != null -> "${tosses.size.toThaiNumerals()} / ๖ · ${practiceLineName(line)}"
                    else -> ""
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (tosses.isEmpty() || done) ZenColors.CinnabarRed else ZenColors.InkSoft,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** What a practice line is called in plain words. */
private fun practiceLineName(line: LineType): String = when (line) {
    LineType.YOUNG_YANG -> "เส้นเต็ม (หยาง)"
    LineType.YOUNG_YIN -> "เส้นขาด (หยิน)"
    LineType.OLD_YANG -> "เส้นเต็มที่กำลังแปร"
    LineType.OLD_YIN -> "เส้นขาดที่กำลังแปร"
}

/** A hexagram as a reading opens it: the figure, its name and its judgment straight from the scripture. */
@Composable
private fun ReadVisual() {
    ZenCard(
        modifier = Modifier.widthIn(max = 340.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("กวาหลัก", style = MaterialTheme.typography.labelSmall, color = ZenColors.BambooDeep)
            Spacer(Modifier.height(ZenSpacing.md))
            HexagramFigure(pattern = "111000", lineWidth = 74.dp, lineHeight = 9.dp, gap = 6.dp)
            Spacer(Modifier.height(ZenSpacing.md))
            Text("泰", style = MaterialTheme.typography.headlineMedium, color = ZenColors.InkBlack)
            Text("๑๑ · ไท่ สันติ, ความรุ่งเรือง", style = MaterialTheme.typography.titleSmall, color = ZenColors.InkBlack)
            Spacer(Modifier.height(ZenSpacing.md))
            Text(
                "“สิ่งเล็กจากไป สิ่งใหญ่มาถึง เป็นมงคล แผ่ซ่าน”",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = ZenColors.InkSoft,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The disclaimer, unchanged in substance: it is what the seeker accepts before the first reading. */
@Composable
private fun DisclaimerPage() {
    val uri = LocalUriHandler.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .heightIn(min = maxHeight)
            .padding(horizontal = ZenSpacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(ZenSpacing.md))
        CinnabarSeal(text = "易經", size = 56.dp)
        Spacer(Modifier.height(18.dp))
        Text("ก่อนเริ่มถามทาง", style = MaterialTheme.typography.headlineMedium, color = ZenColors.InkBlack)
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            POINTS.forEachIndexed { i, (title, body) ->
                Row {
                    Text((i + 1).toThaiNumerals(), style = MaterialTheme.typography.titleMedium, color = ZenColors.CinnabarRed)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = ZenColors.InkBlack)
                        Text(body, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkSoft)
                    }
                }
            }
        }
        Spacer(Modifier.height(ZenSpacing.sm))
        Row {
            TextButton(onClick = { runCatching { uri.openUri("${BuildConfig.WEB_BASE_URL}/terms") } }) {
                Text("ข้อกำหนดการใช้งาน", style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint)
            }
            TextButton(onClick = { runCatching { uri.openUri("${BuildConfig.WEB_BASE_URL}/privacy") } }) {
                Text("นโยบายความเป็นส่วนตัว", style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(ZenSpacing.md))
    }
    }
}
