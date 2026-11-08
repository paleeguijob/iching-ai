package com.iching.ai.feature.siamsi

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiStick
import com.iching.ai.domain.siamsi.SiamSiTopic
import com.iching.ai.domain.siamsi.topicOrder
import com.iching.ai.feature.divination.DivinationUiState
import com.iching.ai.feature.divination.FollowUpSection
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.components.TypewriterText
import com.iching.ai.ui.components.ZenCard
import com.iching.ai.ui.components.ZenCardDivider
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenSecondaryButton
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.toThaiNumerals

const val SIAMSI_REFLECTION = "เซียมซีเพื่อการไตร่ตรองและความบันเทิง โปรดใช้วิจารณญาณ"

/** "เซียมซีเจ้าแม่กวนอิม · ใบที่ ๑๗" */
fun slipTitle(set: SiamSiSet, number: Int): String = "${set.name} · ใบที่ ${number.toThaiNumerals()}"

/** The slip, the card that asks the hermit for an interpretation, the seven matters, and the source line. */
@Composable
fun SiamSiSlipContent(
    set: SiamSiSet,
    stick: SiamSiStick,
    autoConfirmed: Boolean,
    question: String,
    category: QuestionCategory?,
    interpret: InterpretUi,
    status: AccountStatus,
    onInterpret: (() -> Unit)?,
    onSendFollowUp: ((String) -> Unit)?,
    onOpenPaywall: () -> Unit,
    animateInterpretation: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ZenSpacing.md)) {
        if (question.isNotBlank()) {
            Text(
                "“$question”",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = ZenColors.InkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (autoConfirmed) {
            Text(
                "ไม้ปัวไม่ได้ศักดิ์ครบสามรอบ ฟ้ายอมให้ใบนี้",
                style = MaterialTheme.typography.bodySmall,
                color = ZenColors.InkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SlipCard(set, stick)
        InterpretCard(interpret, onInterpret, animateInterpretation)
        if (interpret.done && onSendFollowUp != null) {
            FollowUpSection(
                state = DivinationUiState(question = question, status = status, followUps = interpret.followUps, followUpBusy = interpret.followUpBusy),
                onSend = onSendFollowUp,
                onOpenPaywall = onOpenPaywall,
            )
        }
        TopicsCard(stick, category)
        Text(
            "ตำรา ${set.sourceTitle} · คำแปลโดยทีมอี้จิง AI · $NOT_ENDORSED",
            style = MaterialTheme.typography.labelSmall,
            color = ZenColors.InkFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            SIAMSI_REFLECTION,
            style = MaterialTheme.typography.labelSmall,
            color = ZenColors.InkFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SlipCard(set: SiamSiSet, stick: SiamSiStick) {
    var storyOpen by rememberSaveable { mutableStateOf(false) }
    ZenCard(accentTop = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(slipTitle(set, stick.number), modifier = Modifier.weight(1f))
            LevelBadge(stick)
        }
        Spacer(Modifier.height(ZenSpacing.sm))
        Text(
            stick.number.toThaiNumerals(),
            fontSize = 44.sp,
            color = ZenColors.CinnabarRed,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        stick.labelOrig?.let {
            Text(it, fontFamily = FontFamily.Serif, color = ZenColors.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(ZenSpacing.md))
        val thaiOnly = stick.poemOrig == stick.poemTh
        if (!thaiOnly) {
            stick.poemOrig.forEach {
                Text(
                    it,
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    color = ZenColors.InkBlack,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(ZenSpacing.sm))
        }
        stick.poemTh.forEach {
            Text(
                it,
                style = if (thaiOnly) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                color = if (thaiOnly) ZenColors.InkBlack else ZenColors.InkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(ZenSpacing.md))
        ZenCardDivider()
        Spacer(Modifier.height(ZenSpacing.md))
        Text(stick.summary, style = MaterialTheme.typography.bodyLarge, color = ZenColors.InkBlack)
        if (stick.story.isNotBlank()) {
            Spacer(Modifier.height(ZenSpacing.sm))
            Column(Modifier.animateContentSize()) {
                Text(
                    (if (storyOpen) "▾ " else "▸ ") + "เรื่องในตำนานของใบนี้",
                    style = MaterialTheme.typography.labelLarge,
                    color = ZenColors.BambooDeep,
                    modifier = Modifier.clickable { storyOpen = !storyOpen }.padding(vertical = 6.dp),
                )
                if (storyOpen) Text(stick.story, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
            }
        }
    }
}

@Composable
private fun LevelBadge(stick: SiamSiStick) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(ZenColors.current.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stick.level.labelTh, style = MaterialTheme.typography.labelMedium, color = ZenColors.InkBlack)
        stick.gradeOrig?.let {
            Spacer(Modifier.width(6.dp))
            Text(it, fontFamily = FontFamily.Serif, fontSize = 11.sp, color = ZenColors.InkSoft)
        }
    }
}

@Composable
private fun InterpretCard(ui: InterpretUi, onInterpret: (() -> Unit)?, animate: Boolean) {
    val uri = LocalUriHandler.current
    when {
        ui.safety != null -> ZenCard {
            Text(ui.safety, style = MaterialTheme.typography.bodyLarge, color = ZenColors.InkBlack)
            Spacer(Modifier.height(ZenSpacing.md))
            ZenPrimaryButton("โทรสายด่วนสุขภาพจิต 1323", onClick = { runCatching { uri.openUri("tel:1323") } }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(ZenSpacing.sm))
            ZenSecondaryButton("เหตุฉุกเฉินทางการแพทย์ 1669", onClick = { runCatching { uri.openUri("tel:1669") } }, modifier = Modifier.fillMaxWidth())
        }
        ui.text.isNotEmpty() || ui.streaming -> ZenCard(accentTop = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("คำแปลจากนักพรต", style = MaterialTheme.typography.titleMedium, color = ZenColors.InkBlack, modifier = Modifier.weight(1f))
                Text(
                    "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZenColors.InkSoft,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ZenColors.current.outlineVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(ZenSpacing.sm))
            if (ui.text.isEmpty()) {
                Text("นักพรตกำลังพิจารณาใบของท่าน...", style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkFaint)
            } else {
                TypewriterText(text = ui.text, animate = animate, style = MaterialTheme.typography.bodyLarge)
            }
        }
        onInterpret != null -> ZenCard {
            Text("ให้นักพรตแปลความหมายตามคำถามของคุณ · ใช้ ๑ สิทธิ์", style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
            Spacer(Modifier.height(ZenSpacing.sm))
            ZenPrimaryButton("แปลความหมาย", onClick = onInterpret, modifier = Modifier.fillMaxWidth())
        }
    }
    ui.message?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = ZenColors.CinnabarRed, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TopicsCard(stick: SiamSiStick, category: QuestionCategory?) {
    val order = topicOrder(category)
    // The dot marks the matter the seeker's category points to; a category with no matching topic marks nothing.
    val marked = order != SiamSiTopic.entries || category == QuestionCategory.WORK
    ZenCard {
        SectionLabel("คำทำนายรายเรื่อง")
        order.forEachIndexed { i, topic ->
            val text = stick.topics[topic] ?: return@forEachIndexed
            if (i > 0) ZenCardDivider()
            Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.width(14.dp).padding(top = 7.dp)) {
                    if (i == 0 && marked) Box(Modifier.size(5.dp).clip(CircleShape).background(ZenColors.CinnabarRed))
                }
                Column {
                    Text(topic.labelTh, style = MaterialTheme.typography.labelLarge, color = ZenColors.InkBlack)
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
                }
            }
        }
    }
}
