package com.iching.ai.feature.reading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.oracle.linePositionTh
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.InkDivider
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.components.TypewriterText
import com.iching.ai.ui.formatThaiDate
import com.iching.ai.ui.formatThaiTime
import com.iching.ai.ui.theme.ZenColors

/** Question header plus the primary → relating hexagram pair. */
@Composable
fun CastSummary(
    question: String,
    lines: List<LineType>,
    primary: Hexagram,
    relating: Hexagram?,
    createdAtMillis: Long?,
    modifier: Modifier = Modifier,
    onHexagramClick: ((Int) -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (createdAtMillis != null && createdAtMillis > 0) {
            Text(
                "${formatThaiDate(createdAtMillis)} · ${formatThaiTime(createdAtMillis)}",
                style = MaterialTheme.typography.labelMedium,
                color = ZenColors.InkFaint,
            )
            Spacer(Modifier.height(10.dp))
        }
        Text(
            if (question.isBlank()) "ตั้งจิตอธิษฐาน มิได้เอ่ยคำถาม" else "“$question”",
            style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
            color = ZenColors.InkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.Center) {
            HexagramCard(primary, lines, label = "กวาหลัก", onClick = onHexagramClick)
            if (relating != null) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = "เปลี่ยนเป็น",
                    tint = ZenColors.CinnabarRed.copy(alpha = 0.7f),
                    // Level with the middle of the hexagram figures.
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 64.dp).size(22.dp),
                )
                HexagramCard(relating, null, label = "กวาแปร", onClick = onHexagramClick)
            }
        }
    }
}

@Composable
private fun HexagramCard(hexagram: Hexagram, lines: List<LineType>?, label: String, onClick: ((Int) -> Unit)?) {
    Column(
        Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable { onClick(hexagram.id) } else Modifier)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ZenColors.BambooDeep)
        Spacer(Modifier.height(10.dp))
        HexagramFigure(
            pattern = hexagram.pattern,
            lines = lines,
            lineWidth = 74.dp,
            lineHeight = 9.dp,
            gap = 6.dp,
        )
        Spacer(Modifier.height(12.dp))
        Text(hexagram.nameCn, style = MaterialTheme.typography.headlineMedium, color = ZenColors.InkBlack)
        Text(
            "${hexagram.id}. ${hexagram.shortNameTh}",
            style = MaterialTheme.typography.titleSmall,
            color = ZenColors.InkBlack,
        )
        Text(
            hexagram.meaningTh,
            style = MaterialTheme.typography.bodySmall,
            color = ZenColors.InkSoft,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/**
 * The hermit's words on a paper scroll with wooden rollers.
 * [animate] types the text out; the seal appears once [isComplete].
 */
@Composable
fun OracleScroll(
    text: String,
    isComplete: Boolean,
    animate: Boolean,
    source: InterpretationSource?,
    modifier: Modifier = Modifier,
) {
    var revealed by rememberSaveable { mutableStateOf(!animate) }
    Column(modifier.fillMaxWidth()) {
        ScrollRoller()
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .shadow(6.dp, RoundedCornerShape(2.dp), ambientColor = ZenColors.ScrollWood, spotColor = ZenColors.ScrollWood)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFF3EBD7), ZenColors.Scroll, Color(0xFFF3EBD7)))
                )
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .animateContentSize(tween(300)),
        ) {
            Column {
                Text(
                    if (source == InterpretationSource.SCRIPTURE) "ตัวบทจากคัมภีร์" else "คำชี้แนะจากนักพรต · สร้างโดย AI",
                    style = MaterialTheme.typography.labelMedium,
                    color = ZenColors.CinnabarRed,
                )
                Spacer(Modifier.height(14.dp))
                if (text.isEmpty()) {
                    Text(
                        "นักพรตกำลังพินิจคัมภีร์...",
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        color = ZenColors.InkFaint,
                    )
                } else {
                    TypewriterText(
                        text = text,
                        animate = animate,
                        style = MaterialTheme.typography.bodyLarge,
                        onRevealed = { if (isComplete) revealed = true },
                    )
                }
                AnimatedVisibility(
                    visible = isComplete && revealed,
                    enter = fadeIn(tween(900)) + scaleIn(tween(500), initialScale = 1.6f),
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        CinnabarSeal(text = "武當", size = 50.dp)
                    }
                }
            }
        }
        ScrollRoller()
    }
}

@Composable
private fun ScrollRoller() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF7A5E48), ZenColors.ScrollWood, Color(0xFF3E2E22)))
            )
    )
}

/** Required by App Store / Google Play rules for generative AI: label the content and offer a report button. */
@Composable
fun AiContentLabel(source: InterpretationSource?, onReport: (() -> Unit)?, reported: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (source == InterpretationSource.AI) {
                "เนื้อหาคำทำนายสร้างโดย AI อ้างอิงคัมภีร์อี้จิง · เพื่อความบันเทิงและการสะท้อนคิด ไม่ใช่คำแนะนำทางการแพทย์ กฎหมาย หรือการเงิน"
            } else {
                "ตัวบทจากคัมภีร์อี้จิง · เพื่อความบันเทิงและการสะท้อนคิด ไม่ใช่คำแนะนำทางการแพทย์ กฎหมาย หรือการเงิน"
            },
            style = MaterialTheme.typography.labelSmall,
            color = ZenColors.InkFaint,
            textAlign = TextAlign.Center,
        )
        if (onReport != null) {
            TextButton(onClick = onReport, enabled = !reported) {
                Icon(Icons.Outlined.Info, null, tint = ZenColors.InkFaint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (reported) "รายงานแล้ว" else "รายงานคำตอบไม่เหมาะสม",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZenColors.InkFaint,
                )
            }
        }
    }
}

/** Collapsible original scripture: judgment, image and the changing lines. */
@Composable
fun ScriptureSection(primary: Hexagram, relating: Hexagram?, changingPositions: List<Int>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        InkDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ตัวบทคัมภีร์ดั้งเดิม", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = ZenColors.InkSoft)
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                ScriptureBlock("คำตัดสิน", primary.judgment)
                ScriptureBlock("ภาพลักษณ์", primary.image)
                changingPositions.forEach { position ->
                    ScriptureBlock(linePositionTh(position), primary.lines[position - 1], accent = ZenColors.CinnabarRed)
                }
                if (changingPositions.size == 6) primary.allLines?.let { ScriptureBlock("เส้นทั้งหกแปร", it, ZenColors.CinnabarRed) }
                relating?.let { ScriptureBlock("กวาแปร · ${it.shortNameTh}", it.judgment, ZenColors.BambooDeep) }
            }
        }
        InkDivider()
    }
}

@Composable
fun ScriptureBlock(title: String, body: String, accent: Color = ZenColors.BambooDeep) {
    Column {
        SectionLabel(title, color = accent)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkBlack)
    }
}
