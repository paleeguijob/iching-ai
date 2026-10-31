package com.iching.ai.feature.divination

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.LimitReason
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenSecondaryButton
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.toThaiNumerals

/** Explains why the scroll shows raw scripture, with the ways to get the hermit's words. */
@Composable
fun ReadingNoticeCard(
    notice: ReadingNotice,
    status: AccountStatus,
    canRetry: Boolean,
    isWatchingAd: Boolean,
    onRetry: () -> Unit,
    onWatchAd: () -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val quota = notice is ReadingNotice.Limited && notice.reason == LimitReason.QUOTA
    val message = when (notice) {
        is ReadingNotice.Limited -> when (notice.reason) {
            LimitReason.QUOTA -> if (status.readingsLeft > 0) {
                "ท่านได้รับสิทธิ์เพิ่มแล้ว แตะด้านล่างเพื่อรับคำชี้แนะจากนักพรตสำหรับกวานี้"
            } else {
                "คำชี้แนะจากนักพรตสำหรับวันนี้ครบแล้ว ท่านยังอ่านตัวบทคัมภีร์ด้านล่างได้เสมอ"
            }
            LimitReason.RATE_LIMITED -> "โปรดเว้นจังหวะหายใจสักครู่${notice.retryAfterSeconds?.let { " (${it.toThaiNumerals()} วินาที)" } ?: ""} แล้วจึงถามนักพรตอีกครั้ง"
            LimitReason.AI_DISABLED -> "นักพรตปลีกวิเวกชั่วคราว จึงแสดงตัวบทจากคัมภีร์แทน โปรดกลับมาใหม่ภายหลัง"
            LimitReason.FOLLOW_UP_LIMIT -> "ถามต่อครบจำนวนครั้งแล้ว"
        }
        is ReadingNotice.Failed -> when (notice.reason) {
            FailureReason.NETWORK -> "การเชื่อมต่อขาดหาย จึงแสดงตัวบทจากคัมภีร์แทน แตะถามนักพรตอีกครั้งเพื่อรับคำทำนายฉบับเต็ม โดยไม่เสียสิทธิ์ซ้ำ"
            FailureReason.BLOCKED -> "คำถามนี้ไม่อาจตีความได้ จึงแสดงตัวบทจากคัมภีร์แทน สิทธิ์ของท่านยังไม่ถูกใช้"
            FailureReason.UNAUTHENTICATED -> "ยืนยันตัวตนกับระบบไม่สำเร็จ จึงแสดงตัวบทจากคัมภีร์แทน"
            else -> "การติดต่อนักพรตขัดข้องชั่วคราว จึงแสดงตัวบทจากคัมภีร์แทน สิทธิ์ของท่านยังไม่ถูกใช้"
        }
        ReadingNotice.Offline -> "ไม่สามารถเชื่อมต่ออินเทอร์เน็ตได้ จึงเสี่ยงทายในเครื่องและแสดงตัวบทจากคัมภีร์"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZenColors.CinnabarRed.copy(alpha = 0.07f))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Info, null, tint = ZenColors.CinnabarRed, modifier = Modifier.size(20.dp).padding(top = 2.dp))
            Spacer(Modifier.width(10.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkBlack)
        }
        val retryable = notice !is ReadingNotice.Offline && !(notice is ReadingNotice.Limited && notice.reason == LimitReason.AI_DISABLED)
        when {
            quota && status.readingsLeft > 0 && canRetry -> {
                Spacer(Modifier.height(12.dp))
                ZenPrimaryButton("รับคำชี้แนะจากนักพรต", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            }
            quota -> {
                Spacer(Modifier.height(12.dp))
                if (status.adsWatchableLeft > 0 && canRetry) {
                    ZenPrimaryButton(
                        text = if (isWatchingAd) "กำลังเปิดโฆษณา..." else "ชมโฆษณาสั้น รับคำชี้แนะ (เหลือ ${status.adsWatchableLeft.toThaiNumerals()})",
                        onClick = onWatchAd,
                        enabled = !isWatchingAd,
                        color = ZenColors.BambooDeep,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                ZenSecondaryButton("Premium · ไม่มีโฆษณา · วันละ ๑๐ คำทำนาย", onClick = onOpenPaywall, modifier = Modifier.fillMaxWidth())
            }
            canRetry && retryable -> TextButton(onClick = onRetry) { Text("ถามนักพรตอีกครั้ง", color = ZenColors.CinnabarRed) }
        }
    }
}

/** Premium chat with the hermit about the same cast (up to N turns), or a teaser for free users. */
@Composable
fun FollowUpSection(
    state: DivinationUiState,
    onSend: (String) -> Unit,
    onOpenPaywall: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionLabel("ถามต่อกับนักพรต", color = ZenColors.CinnabarRed)
        Spacer(Modifier.height(12.dp))
        if (!state.status.premium) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, ZenColors.InkBlack.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenPaywall)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Lock, null, tint = ZenColors.BambooDeep)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("สนทนาต่อในกวาเดิมได้ ๓ คำถาม", style = MaterialTheme.typography.bodyMedium)
                    Text("สิทธิ์ของสมาชิก Premium", style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint)
                }
                Text("ดูแพ็กเกจ", style = MaterialTheme.typography.labelMedium, color = ZenColors.CinnabarRed)
            }
        } else {
            FollowUpThread(state, onSend)
        }
    }
}

@Composable
private fun FollowUpThread(state: DivinationUiState, onSend: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        state.followUps.forEach { turn ->
            Text(
                "“${turn.question}”",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = ZenColors.InkSoft,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                turn.answer.ifEmpty { "นักพรตกำลังพินิจ..." },
                style = MaterialTheme.typography.bodyMedium,
                color = if (turn.answer.isEmpty()) ZenColors.InkFaint else ZenColors.InkBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ZenColors.Scroll)
                    .padding(14.dp),
            )
            Spacer(Modifier.height(14.dp))
        }
        if (state.followUpsLeft > 0) {
            var draft by rememberSaveable { mutableStateOf("") }
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 300) draft = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.followUpBusy,
                placeholder = { Text("ถามต่อ (เหลือ ${state.followUpsLeft.toThaiNumerals()} คำถาม)", color = ZenColors.InkFaint) },
                trailingIcon = {
                    if (state.followUpBusy) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = ZenColors.BambooDeep, strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { onSend(draft); draft = "" }, enabled = draft.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "ส่ง", tint = ZenColors.CinnabarRed)
                        }
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZenColors.BambooGreen,
                    unfocusedBorderColor = ZenColors.InkBlack.copy(alpha = 0.15f),
                ),
            )
        } else {
            Text("ถามต่อครบ ๓ คำถามแล้ว — หากใจยังมีคำถามใหม่ ให้เสี่ยงทายครั้งใหม่", style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint)
        }
    }
}

private val REPORT_REASONS = listOf(
    "harmful" to "เป็นอันตราย หรือชี้นำให้ทำสิ่งเสี่ยง",
    "offensive" to "หยาบคาย ไม่เหมาะสม",
    "wrong" to "ไม่ตรงกับคัมภีร์ หรือข้อมูลผิด",
    "other" to "อื่น ๆ",
)

@Composable
fun ReportDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(REPORT_REASONS.first().first) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZenColors.PaperCream,
        title = { Text("รายงานคำตอบนี้", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                REPORT_REASONS.forEach { (id, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == id, onClick = { selected = id }, colors = RadioButtonDefaults.colors(selectedColor = ZenColors.CinnabarRed))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(selected) }) { Text("ส่งรายงาน", color = ZenColors.CinnabarRed) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก", color = ZenColors.InkSoft) } },
    )
}

/** Shown instead of a reading when the question signals a crisis. */
@Composable
fun SafetyStage(message: String, onDone: () -> Unit) {
    val uri = LocalUriHandler.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Outlined.Favorite, null, tint = ZenColors.CinnabarRed, modifier = Modifier.size(36.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = ZenColors.InkBlack)
        ZenPrimaryButton("โทรสายด่วนสุขภาพจิต 1323", onClick = { runCatching { uri.openUri("tel:1323") } }, modifier = Modifier.fillMaxWidth(), color = ZenColors.CinnabarRed)
        ZenSecondaryButton("เหตุฉุกเฉินทางการแพทย์ 1669", onClick = { runCatching { uri.openUri("tel:1669") } }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = onDone) { Text("กลับสู่หน้าแรก", color = ZenColors.InkSoft) }
        CinnabarSeal(text = "安", size = 44.dp)
    }
}
