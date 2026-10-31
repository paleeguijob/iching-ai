package com.iching.ai.feature.paywall

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.data.BuildConfig
import com.iching.ai.domain.model.ProductKind
import com.iching.ai.domain.model.StoreProduct
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenSecondaryButton
import com.iching.ai.ui.formatThaiDate
import com.iching.ai.ui.theme.ZenColors
import kotlinx.coroutines.delay

private val BENEFITS = listOf(
    "คำทำนายจากนักพรต วันละ ๑๐ ครั้ง ด้วยปัญญาที่ลึกกว่า",
    "ถามต่อกับนักพรตได้ ๓ คำถามในแต่ละคำทำนาย",
    "บันทึกชะตาไม่จำกัด และกู้คืนข้ามเครื่องได้",
    "ไม่มีโฆษณา",
)

@Composable
fun PaywallScreen(viewModel: PaywallViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    LaunchedEffect(state.purchased) {
        if (state.purchased) {
            delay(1600)
            onClose()
        }
    }

    PaperBackground(showMountains = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "ปิด", tint = ZenColors.InkBlack) }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CinnabarSeal(text = "道", size = 56.dp)
                Spacer(Modifier.height(14.dp))
                Text("อี้จิง Premium", style = MaterialTheme.typography.headlineMedium, color = ZenColors.InkBlack)
                Text(
                    "ให้นักพรตร่วมพินิจทุกการตัดสินใจของท่าน",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZenColors.InkSoft,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BENEFITS.forEach { benefit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = ZenColors.BambooDeep, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(benefit, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))

                if (state.status.premium) {
                    Text(
                        "ท่านเป็นสมาชิก Premium" + (state.status.premiumUntilMillis?.let { " ถึง ${formatThaiDate(it)}" } ?: ""),
                        style = MaterialTheme.typography.titleSmall,
                        color = ZenColors.BambooDeep,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                when {
                    state.loading -> CircularProgressIndicator(color = ZenColors.BambooDeep)
                    state.products.isEmpty() -> ZenSecondaryButton("ลองโหลดใหม่", onClick = viewModel::load)
                    else -> {
                        if (!state.status.premium) {
                            state.subscriptions.forEach { product ->
                                PlanCard(product, selected = product == state.selected, onClick = { viewModel.select(product) })
                                Spacer(Modifier.height(10.dp))
                            }
                            val selected = state.selected
                            if (selected != null && selected.kind != ProductKind.COINS) {
                                Spacer(Modifier.height(6.dp))
                                ZenPrimaryButton(
                                    text = if (state.busy) "กำลังดำเนินการ..." else selected.trialTh?.let { "เริ่ม$it" } ?: "สมัคร ${selected.price}/${selected.periodTh ?: ""}",
                                    onClick = { viewModel.buy() },
                                    enabled = !state.busy,
                                    color = ZenColors.CinnabarRed,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    renewalTerms(selected),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZenColors.InkFaint,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        state.coins?.let { coins ->
                            Spacer(Modifier.height(24.dp))
                            SectionLabel("ไม่ชอบสมัครสมาชิก?")
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, ZenColors.InkBlack.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                                    .clickable(enabled = !state.busy) { viewModel.buy(coins) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("เหรียญอี้จิง ๑๐ เหรียญ", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "๑ เหรียญ = ๑ คำทำนายจากนักพรต ไม่มีวันหมดอายุ · มีอยู่ ${state.status.coins} เหรียญ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ZenColors.InkFaint,
                                    )
                                }
                                Text(coins.price, style = MaterialTheme.typography.titleMedium, color = ZenColors.CinnabarRed)
                            }
                        }
                    }
                }

                state.message?.let {
                    Spacer(Modifier.height(14.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = ZenColors.CinnabarRed, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = viewModel::restore, enabled = !state.busy) {
                        Text("กู้คืนการซื้อ", color = ZenColors.InkSoft, style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = { runCatching { uri.openUri(viewModel.manageSubscriptionsUrl) } }) {
                        Text("จัดการการสมัคร", color = ZenColors.InkSoft, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { runCatching { uri.openUri("${BuildConfig.WEB_BASE_URL}/terms") } }) {
                        Text("ข้อกำหนดการใช้งาน", color = ZenColors.InkFaint, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { runCatching { uri.openUri("${BuildConfig.WEB_BASE_URL}/privacy") } }) {
                        Text("นโยบายความเป็นส่วนตัว", color = ZenColors.InkFaint, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Auto-renewal disclosure required by both stores, next to the purchase button. */
private fun renewalTerms(product: StoreProduct): String {
    val period = product.periodTh ?: "รอบ"
    val trial = product.trialTh?.let { "$it จากนั้น " } ?: ""
    return "${trial}เรียกเก็บ ${product.price} ทุก$period ต่ออายุอัตโนมัติจนกว่าจะยกเลิก " +
        "ยกเลิกได้ทุกเมื่อในการตั้งค่าบัญชีของร้านค้า อย่างน้อย ๒๔ ชั่วโมงก่อนรอบบิลถัดไป"
}

@Composable
private fun PlanCard(product: StoreProduct, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) ZenColors.CinnabarRed else ZenColors.InkBlack.copy(alpha = 0.12f)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) ZenColors.CinnabarRed.copy(alpha = 0.05f) else ZenColors.PaperCream)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (product.kind == ProductKind.YEARLY) "รายปี" else "รายเดือน", style = MaterialTheme.typography.titleSmall)
                if (product.kind == ProductKind.YEARLY) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "คุ้มที่สุด",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZenColors.PaperCream,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ZenColors.CinnabarRed).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            product.trialTh?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = ZenColors.BambooDeep) }
        }
        Text("${product.price}/${product.periodTh ?: ""}", style = MaterialTheme.typography.titleMedium, color = ZenColors.InkBlack)
    }
}
