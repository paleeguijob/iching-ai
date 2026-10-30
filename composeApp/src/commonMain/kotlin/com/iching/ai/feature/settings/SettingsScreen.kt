package com.iching.ai.feature.settings

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.theme.ZenColors
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val current = settings ?: return
    var keyDraft by rememberSaveable(current.apiKey) { mutableStateOf(current.apiKey) }
    var showKey by remember { mutableStateOf(false) }
    var savedTick by remember { mutableStateOf(0) }
    var showSaved by remember { mutableStateOf(false) }
    LaunchedEffect(savedTick) {
        if (savedTick > 0) {
            showSaved = true
            delay(2000)
            showSaved = false
        }
    }

    PaperBackground(showMountains = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
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
                SectionLabel("กุญแจสู่นักพรต (Gemini API Key)")
                Text(
                    "แอปเรียกใช้ Google Gemini โดยตรงจากเครื่องของท่าน กุญแจถูกเก็บไว้ในเครื่องนี้เท่านั้น " +
                        "ขอรับกุญแจได้ฟรีที่ aistudio.google.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenColors.InkSoft,
                )
                OutlinedTextField(
                    value = keyDraft,
                    onValueChange = { keyDraft = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("วางกุญแจ API ที่นี่", color = ZenColors.InkFaint) },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showKey) "ซ่อนกุญแจ" else "แสดงกุญแจ",
                                tint = ZenColors.InkFaint,
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZenColors.BambooGreen,
                        unfocusedBorderColor = ZenColors.InkBlack.copy(alpha = 0.15f),
                        focusedContainerColor = ZenColors.PaperCream,
                        unfocusedContainerColor = ZenColors.PaperCream,
                    ),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ZenPrimaryButton(
                        text = "บันทึกกุญแจ",
                        onClick = {
                            viewModel.saveApiKey(keyDraft)
                            savedTick++
                        },
                        enabled = keyDraft != current.apiKey,
                        color = ZenColors.BambooDeep,
                    )
                    Spacer(Modifier.width(12.dp))
                    if (showSaved) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = ZenColors.BambooDeep)
                        Spacer(Modifier.width(4.dp))
                        Text("บันทึกแล้ว", style = MaterialTheme.typography.labelMedium, color = ZenColors.BambooDeep)
                    } else if (current.hasApiKey) {
                        Text("เชื่อมต่อแล้ว", style = MaterialTheme.typography.labelMedium, color = ZenColors.BambooDeep)
                    }
                }

                Spacer(Modifier.height(8.dp))
                SectionLabel("ปัญญาของนักพรต (โมเดล)")
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ZenColors.PaperCream)
                        .border(1.dp, ZenColors.InkBlack.copy(alpha = 0.07f), RoundedCornerShape(18.dp)),
                ) {
                    SettingsViewModel.MODELS.forEach { (label, id) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.setModel(id) }.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = current.model == id,
                                onClick = { viewModel.setModel(id) },
                                colors = RadioButtonDefaults.colors(selectedColor = ZenColors.CinnabarRed),
                            )
                            Column {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                Text(id, style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                SectionLabel("บรรยากาศ")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ZenColors.PaperCream)
                        .border(1.dp, ZenColors.InkBlack.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
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

                Spacer(Modifier.height(8.dp))
                SectionLabel("ว่าด้วยแอปนี้")
                Text(
                    "อี้จิง AI ใช้การโยนเหรียญสามเหรียญหกครั้งตามตำราดั้งเดิม แล้วอ้างอิงตัวบทคัมภีร์อี้จิง ๖๔ กวา ๓๘๔ เส้น " +
                        "เป็นแก่นของคำชี้แนะทุกครั้ง คำทำนายมีไว้เพื่อการใคร่ครวญและเป็นแรงบันดาลใจ " +
                        "มิใช่คำแนะนำทางการแพทย์ กฎหมาย หรือการเงิน",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenColors.InkSoft,
                )
                Text("เวอร์ชัน 1.0.0", style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
