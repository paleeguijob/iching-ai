package com.iching.ai.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenSecondaryButton
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.toThaiNumerals

/** The choice before casting when nothing pays for the hermit's words yet (spec 3.1–3.3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreCastSheet(
    ui: PreCastSheetUi,
    onWatchAd: () -> Unit,
    onBuyCoins: () -> Unit,
    onScriptureOnly: () -> Unit,
    onPremium: () -> Unit,
    onCastFromReady: () -> Unit,
    onDismiss: () -> Unit,
) {
    val busy by rememberUpdatedState(ui.busy)
    // An ad or a purchase in progress must not be swiped away under the seeker's thumb.
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        confirmValueChange = { !busy || it != SheetValue.Hidden },
    )
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = ZenColors.PaperCream) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                ui.question.ifBlank { "อธิษฐานในใจ" }.let { if (ui.question.isBlank()) it else "“$it”" },
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = ZenColors.InkSoft,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            when (ui.kind) {
                PreCastSheetKind.AD -> AdChoice(ui, onWatchAd, onScriptureOnly, onPremium)
                PreCastSheetKind.UPSELL -> UpsellChoice(ui, onPremium, onBuyCoins, onScriptureOnly)
                PreCastSheetKind.READY -> ReadyChoice(onCastFromReady)
            }
            ui.message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = ZenColors.CinnabarRed, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Heading(title: String, body: String?) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = ZenColors.InkBlack, textAlign = TextAlign.Center)
    body?.let {
        Spacer(Modifier.height(6.dp))
        Text(it, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun AdChoice(ui: PreCastSheetUi, onWatchAd: () -> Unit, onScriptureOnly: () -> Unit, onPremium: () -> Unit) {
    Heading("คำชี้แนะฟรีของวันนี้ใช้แล้ว", "ชมโฆษณาสั้น ๆ เพื่อรับคำชี้แนะจากนักพรตสำหรับการเสี่ยงทายครั้งนี้")
    ZenPrimaryButton(
        text = if (ui.busy) "กำลังเปิดโฆษณา..." else "ชมโฆษณาสั้น แล้วเริ่มเสี่ยงทาย (เหลือ ${ui.adsLeft.toThaiNumerals()})",
        onClick = onWatchAd,
        enabled = !ui.busy,
        color = ZenColors.BambooDeep,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    ZenSecondaryButton("เสี่ยงทาย อ่านตัวบทคัมภีร์อย่างเดียว", onClick = onScriptureOnly, enabled = !ui.busy, modifier = Modifier.fillMaxWidth())
    TextButton(onClick = onPremium, enabled = !ui.busy) {
        Text("Premium · วันละ ๑๐ คำทำนาย ไม่มีโฆษณา", style = MaterialTheme.typography.labelMedium, color = ZenColors.CinnabarRed)
    }
}

@Composable
private fun UpsellChoice(ui: PreCastSheetUi, onPremium: () -> Unit, onBuyCoins: () -> Unit, onScriptureOnly: () -> Unit) {
    Heading("คำชี้แนะฟรีและจากโฆษณาของวันนี้ครบแล้ว", "ท่านยังรับคำชี้แนะจากนักพรตสำหรับการเสี่ยงทายครั้งนี้ได้")
    ZenPrimaryButton(
        text = "Premium · วันละ ๑๐ คำทำนาย" + (ui.premiumTrial?.let { " · $it" } ?: ""),
        onClick = onPremium,
        enabled = !ui.busy,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    ZenSecondaryButton(
        text = if (ui.busy) "กำลังดำเนินการ..." else "เหรียญอี้จิง ๑๐ เหรียญ" + (ui.coins?.let { " · ${it.price}" } ?: ""),
        onClick = onBuyCoins,
        enabled = !ui.busy,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = onScriptureOnly, enabled = !ui.busy) {
        Text("เสี่ยงทาย อ่านตัวบทคัมภีร์อย่างเดียว", style = MaterialTheme.typography.labelMedium, color = ZenColors.InkSoft)
    }
    Text(
        "หรือกลับมาใหม่พรุ่งนี้ สิทธิ์ฟรีเริ่มนับใหม่ทุกวัน",
        style = MaterialTheme.typography.labelSmall,
        color = ZenColors.InkBlack.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ReadyChoice(onCastFromReady: () -> Unit) {
    Heading("พร้อมรับคำชี้แนะแล้ว", null)
    ZenPrimaryButton("เริ่มเสี่ยงทาย", onClick = onCastFromReady, modifier = Modifier.fillMaxWidth())
}
