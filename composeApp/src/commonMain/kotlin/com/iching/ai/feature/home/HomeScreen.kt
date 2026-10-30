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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.theme.ZenColors

private val SUGGESTIONS = listOf(
    "การงาน" to "การงานของข้าพเจ้าช่วงนี้ควรดำเนินไปอย่างไร",
    "ความรัก" to "ความสัมพันธ์ของข้าพเจ้าจะเป็นไปในทิศทางใด",
    "การเงิน" to "ข้าพเจ้าควรจัดการเรื่องเงินทองอย่างไรในยามนี้",
    "การตัดสินใจ" to "ข้าพเจ้าควรตัดสินใจเรื่องที่ค้างคาใจอย่างไร",
    "จิตใจ" to "ข้าพเจ้าจะหาความสงบในใจได้อย่างไร",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCast: (String) -> Unit,
    onOpenHexagram: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val daily by viewModel.dailyHexagram.collectAsStateWithLifecycle()
    val hasKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    var question by rememberSaveable { mutableStateOf("") }

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
                SUGGESTIONS.forEach { (label, text) ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = ZenColors.BambooDeep,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, ZenColors.BambooGreen.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .clickable { question = text }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            ZenPrimaryButton(
                text = "เริ่มเสี่ยงทาย",
                onClick = { onCast(question.trim()) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (!hasKey) {
                Text(
                    "ยังไม่ได้ตั้งค่ากุญแจ Gemini — จะได้รับตัวบทจากคัมภีร์แทนคำชี้แนะของนักพรต",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenColors.CinnabarRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clickable(onClick = onOpenSettings),
                )
            }

            Spacer(Modifier.height(36.dp))
            daily?.let { DailyHexagramCard(it, onClick = { onOpenHexagram(it.id) }) }
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun BreathingTitle() {
    val transition = rememberInfiniteTransition(label = "breath")
    val alpha by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse),
        label = "alpha",
    )
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("問道", style = MaterialTheme.typography.displayMedium, color = ZenColors.InkBlack.copy(alpha = 0.12f))
        Text(
            "ถามทางแห่งเต๋า",
            style = MaterialTheme.typography.displaySmall,
            color = ZenColors.InkBlack,
            modifier = Modifier.alpha(alpha),
        )
    }
}

@Composable
private fun DailyHexagramCard(hexagram: Hexagram, onClick: () -> Unit) {
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
            hexagram.image,
            style = MaterialTheme.typography.bodyMedium,
            color = ZenColors.InkSoft,
            maxLines = 4,
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("อ่านต่อในคัมภีร์ →", style = MaterialTheme.typography.labelMedium, color = ZenColors.CinnabarRed)
        }
    }
    Spacer(Modifier.size(0.dp))
}
