package com.iching.ai.feature.daily

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.feature.share.ShareCardSheet
import com.iching.ai.feature.share.dailyShareContent
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.InkDivider
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenSecondaryButton
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.toThaiNumerals

@Composable
fun DailyScreen(viewModel: DailyViewModel, onBack: () -> Unit, onOpenHexagram: (Int) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sharing by remember { mutableStateOf(false) }

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    PaperBackground(showMountains = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "กลับ", tint = ZenColors.InkBlack)
                }
                Text("กวาประจำวัน", style = MaterialTheme.typography.titleLarge, color = ZenColors.InkBlack)
            }
            val hexagram = state.hexagram ?: return@Column
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HexagramFigure(hexagram.pattern, lineWidth = 96.dp, lineHeight = 11.dp, gap = 8.dp)
                Spacer(Modifier.height(18.dp))
                Text(hexagram.nameCn, style = MaterialTheme.typography.displayMedium, color = ZenColors.InkBlack)
                Text(
                    "${hexagram.id.toThaiNumerals()} · ${hexagram.shortNameTh}",
                    style = MaterialTheme.typography.titleSmall,
                    color = ZenColors.InkSoft,
                )
                Text(hexagram.meaningTh, style = MaterialTheme.typography.bodyMedium, color = ZenColors.BambooDeep, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                Text(
                    hexagram.dailyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ZenColors.InkBlack,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    "เส้นทางเต๋า · เปิดดูต่อเนื่อง ${state.streak.current.toThaiNumerals()} วัน",
                    style = MaterialTheme.typography.labelMedium,
                    color = ZenColors.CinnabarRed,
                )
                Spacer(Modifier.height(22.dp))
                InkDivider()
                Spacer(Modifier.height(22.dp))
                SectionLabel("คำตัดสิน", Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(hexagram.judgment, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
                ZenPrimaryButton("แชร์การ์ดวันนี้", onClick = { sharing = true }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                ZenSecondaryButton("อ่านต่อในคัมภีร์", onClick = { onOpenHexagram(hexagram.id) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
            }
            if (sharing) {
                ShareCardSheet(dailyShareContent(hexagram), viewModel::logEvent, onDismiss = { sharing = false })
            }
        }
    }

    val unlocked = state.newlyUnlocked.firstOrNull()
    when {
        unlocked != null -> AlertDialog(
            onDismissRequest = viewModel::unlockNoticeShown,
            title = { Text("ปลดล็อกธีม${unlocked.labelTh}") },
            text = { Text("ท่านเปิดดูกวาประจำวันครบ ${unlocked.unlockDays.toThaiNumerals()} วันต่อเนื่องแล้ว เลือกใช้ธีมนี้ได้ในหน้าตั้งค่า") },
            confirmButton = { TextButton(onClick = viewModel::unlockNoticeShown) { Text("รับทราบ") } },
        )
        state.showPushPrompt -> AlertDialog(
            onDismissRequest = viewModel::dismissPushPrompt,
            title = { Text("รับกวาประจำวันทุกเช้าไหม") },
            text = { Text("นักพรตจะส่งข้อความประจำวันให้ท่านเวลา ๗ โมงเช้า ปิดได้ทุกเมื่อในหน้าตั้งค่า") },
            confirmButton = { TextButton(onClick = viewModel::acceptPushPrompt) { Text("รับแจ้งเตือน") } },
            dismissButton = { TextButton(onClick = viewModel::dismissPushPrompt) { Text("ไว้ก่อน") } },
        )
    }
}
