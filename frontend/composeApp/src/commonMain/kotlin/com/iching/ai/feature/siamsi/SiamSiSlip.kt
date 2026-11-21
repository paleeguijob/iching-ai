package com.iching.ai.feature.siamsi

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.siamsi.SiamSiLevel
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiStick
import com.iching.ai.domain.siamsi.SiamSiTopic
import com.iching.ai.domain.siamsi.topicOrder
import com.iching.ai.feature.divination.DivinationUiState
import com.iching.ai.feature.divination.FollowUpSection
import com.iching.ai.feature.divination.ReportDialog
import com.iching.ai.feature.reading.AiContentLabel
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.components.TypewriterText
import com.iching.ai.ui.components.ZenCard
import com.iching.ai.ui.components.ZenCardDivider
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenSecondaryButton
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.strings.LocalStrings
import com.iching.ai.ui.strings.SiamSiStrings

/** The slip header's short form: the set's name without its prefix, then the slip's number. */
private fun slipHeader(set: SiamSiSet, number: Int, strings: SiamSiStrings): String =
    strings.slipHeader(set.nameWithoutPrefix().trim(), number)

/**
 * The slip and what follows it. Before an interpretation: the full slip, the card that asks the hermit, the
 * seven matters. Once the hermit speaks, the slip folds to its first lines above the interpretation (tap to
 * open it), with "ถามต่อ" and "แชร์ใบ" under it.
 */
@Composable
fun SiamSiSlipContent(
    set: SiamSiSet,
    stick: SiamSiStick,
    question: String,
    category: QuestionCategory?,
    interpret: InterpretUi,
    status: AccountStatus,
    onInterpret: (() -> Unit)?,
    onSendFollowUp: ((String) -> Unit)?,
    onOpenPaywall: () -> Unit,
    animateInterpretation: Boolean = true,
    onReport: ((String) -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    showQuestion: Boolean = false,
) {
    val strings = LocalStrings.current.siamsi
    val interpreting = interpret.text.isNotEmpty() || interpret.streaming || interpret.safety != null
    var slipOpen by rememberSaveable { mutableStateOf(false) }
    var asking by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(ZenSpacing.md)) {
        if (showQuestion && question.isNotBlank()) {
            Text(
                "“$question”",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = ZenColors.InkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (interpreting && !slipOpen) {
            FoldedSlip(set, stick, onOpen = { slipOpen = true })
        } else {
            SlipCard(set, stick)
        }
        if (interpreting) {
            InterpretationCard(interpret, animateInterpretation, onReport)
            if (interpret.done && (onSendFollowUp != null || onShare != null)) {
                Row(horizontalArrangement = Arrangement.spacedBy(ZenSpacing.sm)) {
                    if (onSendFollowUp != null) ZenSecondaryButton(strings.askMore, onClick = { asking = true }, modifier = Modifier.weight(1f))
                    if (onShare != null) ZenSecondaryButton(strings.shareSlip, onClick = onShare, modifier = Modifier.weight(1f))
                }
            }
            if (interpret.done && onSendFollowUp != null && (asking || interpret.followUps.isNotEmpty())) {
                FollowUpSection(
                    state = DivinationUiState(question = question, status = status, followUps = interpret.followUps, followUpBusy = interpret.followUpBusy),
                    onSend = onSendFollowUp,
                    onOpenPaywall = onOpenPaywall,
                )
            }
        } else if (onInterpret != null) {
            AskHermitRow(status, onInterpret)
        }
        interpret.message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = ZenColors.CinnabarRed, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        TopicsCard(stick, category)
        Text(
            "${strings.sourceLine(set.sourceTitle)}\n${strings.reflection}",
            style = MaterialTheme.typography.labelSmall,
            color = ZenColors.InkFaint,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = ZenSpacing.xs),
        )
    }
}

@Composable
private fun SlipHeader(set: SiamSiSet, stick: SiamSiStick, grade: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(slipHeader(set, stick.number, LocalStrings.current.siamsi), modifier = Modifier.weight(1f))
        LevelPill(stick.level)
        if (grade) {
            stick.gradeOrig?.let {
                Spacer(Modifier.width(6.dp))
                Text(it, fontFamily = FontFamily.Serif, fontSize = 13.sp, color = ZenColors.InkSoft)
            }
        }
    }
}

@Composable
private fun SlipCard(set: SiamSiSet, stick: SiamSiStick) {
    val strings = LocalStrings.current.siamsi
    val common = LocalStrings.current.common
    var storyOpen by rememberSaveable { mutableStateOf(false) }
    val thaiOnly = stick.poemOrig == stick.poemTh
    ZenCard(accentTop = true) {
        SlipHeader(set, stick, grade = true)
        Spacer(Modifier.height(ZenSpacing.sm))
        Text(
            buildAnnotatedString {
                append(common.number(stick.number))
                withStyle(SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, color = ZenColors.InkSoft)) {
                    append("  / ${common.number(set.count)}")
                }
            },
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
            color = ZenColors.InkBlack,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        stick.labelOrig?.let {
            Text(it, fontFamily = FontFamily.Serif, fontSize = 15.sp, color = ZenColors.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(ZenSpacing.sm))
        if (!thaiOnly) {
            stick.poemOrig.forEach { OriginalLine(it, fontSize = 20.sp) }
            Spacer(Modifier.height(ZenSpacing.sm))
        }
        // Centred on the same axis as the original lines above them.
        stick.poemTh.forEach { line ->
            Text(
                line,
                style = if (thaiOnly) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                color = if (thaiOnly) ZenColors.InkBlack else ZenColors.InkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(ZenSpacing.md))
        DashedRule()
        if (stick.story.isNotBlank()) {
            Column(Modifier.animateContentSize()) {
                Row(
                    Modifier.fillMaxWidth().clickable { storyOpen = !storyOpen }.padding(vertical = ZenSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(strings.storyTitle, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkBlack, modifier = Modifier.weight(1f))
                    Icon(
                        if (storyOpen) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (storyOpen) strings.storyHide else strings.storyShow,
                        tint = ZenColors.InkFaint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (storyOpen) {
                    Text(stick.story, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkSoft)
                    Spacer(Modifier.height(ZenSpacing.sm))
                }
            }
        } else {
            Spacer(Modifier.height(ZenSpacing.sm))
        }
        Text(stick.summary, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
    }
}

/** The slip folded above an interpretation: its header and first two lines; a tap opens it in full. */
@Composable
private fun FoldedSlip(set: SiamSiSet, stick: SiamSiStick, onOpen: () -> Unit) {
    val thaiOnly = stick.poemOrig == stick.poemTh
    ZenCard(onClick = onOpen, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        SlipHeader(set, stick, grade = false)
        Spacer(Modifier.height(ZenSpacing.xs))
        if (thaiOnly) {
            stick.poemTh.take(2).forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkBlack, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        } else {
            stick.poemOrig.take(2).forEach { OriginalLine(it, fontSize = 16.sp) }
        }
    }
}

@Composable
private fun OriginalLine(text: String, fontSize: androidx.compose.ui.unit.TextUnit) {
    Text(
        text,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize,
        lineHeight = fontSize * 1.75f,
        letterSpacing = fontSize * 0.12f,
        color = ZenColors.InkBlack,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DashedRule() {
    val line = ZenColors.current.outline
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(line, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
    }
}

@Composable
private fun LevelPill(level: SiamSiLevel) {
    val tone = if (level == SiamSiLevel.CAUTION) ZenColors.CinnabarRed else ZenColors.BambooDeep
    Text(
        LocalStrings.current.common.level(level),
        style = MaterialTheme.typography.labelMedium,
        color = tone,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tone.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}

@Composable
private fun AiTag() {
    Text(
        "AI",
        style = MaterialTheme.typography.labelSmall,
        color = ZenColors.InkFaint,
        modifier = Modifier
            .border(1.dp, ZenColors.current.outline, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp),
    )
}

/** "ให้นักพรตแปลความหมาย…" with the credit it costs, and the button. */
@Composable
private fun AskHermitRow(status: AccountStatus, onInterpret: () -> Unit) {
    val strings = LocalStrings.current.siamsi
    ZenCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.askHermit, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkBlack, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(6.dp))
                    AiTag()
                }
                Text(
                    if (status.readingsLeft > 0) strings.costLeft(status.readingsLeft) else strings.costOne,
                    style = MaterialTheme.typography.labelSmall,
                    color = ZenColors.InkFaint,
                )
            }
            Spacer(Modifier.width(ZenSpacing.sm))
            ZenPrimaryButton(strings.interpret, onClick = onInterpret, compact = true, showSeal = false, modifier = Modifier.widthIn(min = 112.dp))
        }
    }
}

@Composable
private fun InterpretationCard(ui: InterpretUi, animate: Boolean, onReport: ((String) -> Unit)?) {
    val strings = LocalStrings.current.siamsi
    val crisis = LocalStrings.current.divination
    val uri = LocalUriHandler.current
    var reporting by rememberSaveable { mutableStateOf(false) }
    if (reporting && onReport != null) {
        ReportDialog(onDismiss = { reporting = false }, onSubmit = {
            reporting = false
            onReport(it)
        })
    }
    if (ui.safety != null) {
        ZenCard {
            Text(ui.safety, style = MaterialTheme.typography.bodyLarge, color = ZenColors.InkBlack)
            Spacer(Modifier.height(ZenSpacing.md))
            ZenPrimaryButton(crisis.helplineLabel, onClick = { runCatching { uri.openUri(crisis.helplineUri) } }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(ZenSpacing.sm))
            ZenSecondaryButton(crisis.emergencyLabel, onClick = { runCatching { uri.openUri(crisis.emergencyUri) } }, modifier = Modifier.fillMaxWidth())
        }
        return
    }
    ZenCard(accentTop = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(strings.hermitInterpretation, color = ZenColors.CinnabarRed, modifier = Modifier.weight(1f))
            AiTag()
        }
        Spacer(Modifier.height(ZenSpacing.sm))
        if (ui.text.isEmpty()) {
            Text(strings.hermitConsidering, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkFaint)
        } else {
            TypewriterText(text = ui.text, animate = animate, style = MaterialTheme.typography.bodyLarge)
        }
        if (ui.done) {
            AiContentLabel(
                source = InterpretationSource.AI,
                onReport = onReport?.let { { reporting = true } },
                reported = ui.reported,
                label = strings.aiLabel,
            )
        }
    }
}

/** The seven matters, the seeker's own first with a cinnabar dot. */
@Composable
private fun TopicsCard(stick: SiamSiStick, category: QuestionCategory?) {
    val order = topicOrder(category)
    // The dot marks the matter the seeker's category points to; a category with no matching topic marks nothing.
    val marked = order != SiamSiTopic.entries || category == QuestionCategory.WORK
    ZenCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        order.forEachIndexed { i, topic ->
            val text = stick.topics[topic] ?: return@forEachIndexed
            if (i > 0) ZenCardDivider()
            Row(Modifier.padding(vertical = 11.dp), verticalAlignment = Alignment.Top) {
                Row(Modifier.width(92.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (i == 0 && marked) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(ZenColors.CinnabarRed))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(LocalStrings.current.common.topic(topic), style = MaterialTheme.typography.labelMedium, color = ZenColors.InkBlack)
                }
                Text(text, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkSoft, modifier = Modifier.weight(1f))
            }
        }
    }
}
