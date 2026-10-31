package com.iching.ai.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iching.ai.data.BuildConfig
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.theme.ZenColors

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

/** First-launch disclaimer, required before the first reading. */
@Composable
fun OnboardingScreen(onAccept: () -> Unit) {
    val uri = LocalUriHandler.current
    PaperBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            CinnabarSeal(text = "易經", size = 64.dp)
            Spacer(Modifier.height(18.dp))
            Text("ก่อนเริ่มถามทาง", style = MaterialTheme.typography.headlineMedium, color = ZenColors.InkBlack)
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                POINTS.forEachIndexed { i, (title, body) ->
                    Row {
                        Text("${"๑๒๓๔"[i]}", style = MaterialTheme.typography.titleMedium, color = ZenColors.CinnabarRed)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleSmall, color = ZenColors.InkBlack)
                            Text(body, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkSoft)
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            ZenPrimaryButton("รับทราบและเริ่มต้น", onClick = onAccept, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = { runCatching { uri.openUri("${BuildConfig.WEB_BASE_URL}/terms") } }) {
                    Text("ข้อกำหนดการใช้งาน", style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint)
                }
                TextButton(onClick = { runCatching { uri.openUri("${BuildConfig.WEB_BASE_URL}/privacy") } }) {
                    Text("นโยบายความเป็นส่วนตัว", style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
