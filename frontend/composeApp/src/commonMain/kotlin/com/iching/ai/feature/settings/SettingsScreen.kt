package com.iching.ai.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.data.BuildConfig
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.SignInProvider
import com.iching.ai.domain.model.currentOn
import com.iching.ai.domain.oracle.bangkokEpochDay
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.formatThaiDate
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.paletteFor
import com.iching.ai.ui.toThaiNumerals
import kotlinx.coroutines.delay
import kotlin.time.Clock

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onOpenPaywall: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    var confirmDelete by remember { mutableStateOf(false) }
    val notificationsGranted by viewModel.notificationsGranted.collectAsStateWithLifecycle()
    val pushBlocked by viewModel.pushBlocked.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshNotificationPermission()
        onPauseOrDispose { }
    }
    val current = settings ?: return

    if (pushBlocked) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPushBlocked,
            title = { Text("ยังไม่ได้รับอนุญาตให้แจ้งเตือน") },
            text = { Text("โปรดเปิดการแจ้งเตือนของแอปนี้ในการตั้งค่าของเครื่อง") },
            confirmButton = { TextButton(onClick = viewModel::openNotificationSettings) { Text("เปิดการตั้งค่า") } },
            dismissButton = { TextButton(onClick = viewModel::dismissPushBlocked) { Text("ไว้ก่อน") } },
        )
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(4000)
            viewModel.messageShown()
        }
    }

    PaperBackground(showMountains = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "กลับ", tint = ZenColors.InkBlack)
                }
                Text("ตั้งค่า", style = MaterialTheme.typography.headlineSmall)
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                message?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZenColors.PaperCream,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ZenColors.BambooDeep).padding(12.dp),
                    )
                }

                SectionLabel("สมาชิก")
                Card {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (status.premium) "Premium" else "ผู้ใช้ทั่วไป", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (status.premium) {
                                    "วันนี้เหลือ ${status.includedLeft} คำทำนาย" + (status.premiumUntilMillis?.let { " · ถึง ${formatThaiDate(it)}" } ?: "")
                                } else {
                                    "วันนี้เหลือ ${status.readingsLeft} คำทำนาย · เหรียญ ${status.coins}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = ZenColors.InkFaint,
                            )
                        }
                        TextButton(onClick = onOpenPaywall) {
                            Text(if (status.premium) "รายละเอียด" else "อัปเกรด", color = ZenColors.CinnabarRed)
                        }
                    }
                    Divider()
                    LinkRow("กู้คืนการซื้อ", enabled = !busy) { viewModel.restore() }
                    Divider()
                    LinkRow("จัดการการสมัครสมาชิก") { runCatching { uri.openUri(viewModel.manageSubscriptionsUrl) } }
                }

                SectionLabel("บัญชี")
                Card {
                    val provider = account?.provider ?: SignInProvider.ANONYMOUS
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            when (provider) {
                                SignInProvider.APPLE -> "เชื่อมกับ Apple แล้ว"
                                SignInProvider.GOOGLE -> "เชื่อมกับ Google แล้ว"
                                SignInProvider.ANONYMOUS -> "บัญชีบนเครื่องนี้ (ยังไม่เชื่อม)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            account?.email ?: "รหัสบัญชี ${account?.uid?.take(8) ?: "—"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZenColors.InkFaint,
                        )
                        if (provider == SignInProvider.ANONYMOUS) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "เชื่อมบัญชีเพื่อเก็บการซื้อและบันทึกชะตาไว้ เมื่อเปลี่ยนเครื่องหรือติดตั้งแอปใหม่",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZenColors.InkSoft,
                            )
                        }
                    }
                    if (provider == SignInProvider.ANONYMOUS) {
                        if (viewModel.supportsApple) {
                            Divider()
                            LinkRow("ลงชื่อเข้าใช้ด้วย Apple", enabled = !busy) { viewModel.link(SignInProvider.APPLE) }
                        }
                        if (viewModel.supportsGoogle) {
                            Divider()
                            LinkRow("ลงชื่อเข้าใช้ด้วย Google", enabled = !busy) { viewModel.link(SignInProvider.GOOGLE) }
                        }
                    }
                    if (status.premium) {
                        Divider()
                        LinkRow("ดึงบันทึกชะตาจากระบบ", enabled = !busy) { viewModel.syncJournal() }
                    }
                    Divider()
                    LinkRow("ลบบัญชีและข้อมูลทั้งหมด", color = ZenColors.CinnabarRed, enabled = !busy) { confirmDelete = true }
                }

                SectionLabel("บรรยากาศ")
                Card {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("เสียงเหรียญและกระดิ่งลม", style = MaterialTheme.typography.bodyMedium)
                            Text("ช่วยให้จิตสงบก่อนการเสี่ยงทาย", style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint)
                        }
                        Switch(
                            checked = current.soundEnabled,
                            onCheckedChange = viewModel::setSound,
                            colors = SwitchDefaults.colors(checkedTrackColor = ZenColors.BambooGreen),
                        )
                    }
                }

                if (viewModel.dailyPushSupported) {
                    Card {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("แจ้งเตือนกวาประจำวัน", style = MaterialTheme.typography.bodyMedium)
                                Text("ทุกเช้าเวลา ๗ นาฬิกา", style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint)
                            }
                            Switch(
                                checked = current.dailyPushEnabled && notificationsGranted,
                                onCheckedChange = viewModel::setDailyPush,
                                colors = SwitchDefaults.colors(checkedTrackColor = ZenColors.BambooGreen),
                            )
                        }
                    }
                }

                SectionLabel("ธีม")
                Card {
                    Text(
                        "เปิดดูกวาประจำวันต่อเนื่อง ${current.streak.currentOn(bangkokEpochDay(Clock.System.now().toEpochMilliseconds())).toThaiNumerals()} วัน · ดีที่สุด ${current.streak.best.toThaiNumerals()} วัน",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZenColors.InkSoft,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    AppTheme.entries.forEach { theme ->
                        Divider()
                        ThemeRow(
                            theme = theme,
                            selected = theme == current.selectedTheme,
                            unlocked = theme in current.streak.unlocked,
                            onSelect = { viewModel.selectTheme(theme) },
                        )
                    }
                }

                SectionLabel("ว่าด้วยแอปนี้")
                Text(
                    "อี้จิง AI ใช้การโยนเหรียญสามเหรียญหกครั้งตามตำราดั้งเดิม แล้วอ้างอิงตัวบทคัมภีร์อี้จิง ๖๔ กวา ๓๘๔ เส้น " +
                        "เป็นแก่นของคำชี้แนะทุกครั้ง คำชี้แนะสร้างโดย AI (Google Gemini) มีไว้เพื่อความบันเทิงและการสะท้อนคิด " +
                        "มิใช่คำแนะนำทางการแพทย์ กฎหมาย หรือการเงิน",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenColors.InkSoft,
                )
                Card {
                    LinkRow("นโยบายความเป็นส่วนตัว") { runCatching { uri.openUri("${BuildConfig.WEB_BASE_URL}/privacy") } }
                    Divider()
                    LinkRow("ข้อกำหนดการใช้งาน") { runCatching { uri.openUri("${BuildConfig.WEB_BASE_URL}/terms") } }
                }
                Text("เวอร์ชัน ${viewModel.appVersion}", style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint)
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = ZenColors.PaperCream,
            title = { Text("ลบบัญชีและข้อมูลทั้งหมด?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "บันทึกชะตา บทสนทนา และเหรียญที่เหลือจะถูกลบถาวรทั้งบนเครื่องนี้และบนระบบ ไม่สามารถกู้คืนได้ " +
                        "การลบบัญชีไม่ยกเลิกการสมัครสมาชิก โปรดยกเลิกในการตั้งค่าของ App Store / Google Play ด้วย",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteAccount(onDone = onBack)
                }) { Text("ลบถาวร", color = ZenColors.CinnabarRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("ยกเลิก", color = ZenColors.InkSoft) } },
        )
    }
}

@Composable
private fun ThemeRow(theme: AppTheme, selected: Boolean, unlocked: Boolean, onSelect: () -> Unit) {
    val swatch = paletteFor(theme)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = unlocked, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(swatch.paperCream)
                .border(5.dp, swatch.inkBlack, CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                theme.labelTh,
                style = MaterialTheme.typography.bodyMedium,
                color = if (unlocked) ZenColors.InkBlack else ZenColors.InkFaint,
            )
            if (!unlocked) {
                Text(
                    "ปลดล็อกเมื่อเปิดดูกวาครบ ${theme.unlockDays.toThaiNumerals()} วันต่อเนื่อง",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZenColors.InkFaint,
                )
            }
        }
        when {
            selected -> Icon(Icons.Outlined.Check, contentDescription = "ใช้อยู่", tint = ZenColors.BambooDeep)
            !unlocked -> Icon(Icons.Outlined.Lock, contentDescription = "ยังล็อกอยู่", tint = ZenColors.InkFaint)
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ZenColors.PaperCream)
            .border(1.dp, ZenColors.InkBlack.copy(alpha = 0.07f), RoundedCornerShape(18.dp)),
        content = content,
    )
}

@Composable
private fun Divider() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(ZenColors.InkBlack.copy(alpha = 0.06f)))
}

@Composable
private fun LinkRow(text: String, color: Color = ZenColors.InkBlack, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (enabled) color else color.copy(alpha = 0.4f), modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = ZenColors.InkFaint)
    }
}
