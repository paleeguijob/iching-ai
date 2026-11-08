package com.iching.ai.feature.journal

import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.formatThaiShortDate
import com.iching.ai.ui.components.ZenCardDivider
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import com.iching.ai.domain.model.Outcome
import com.iching.ai.feature.reading.AiContentLabel
import com.iching.ai.ui.components.SectionLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.Reading
import com.iching.ai.feature.reading.CastSummary
import com.iching.ai.feature.reading.OracleSheet
import com.iching.ai.feature.reading.ScriptureSection
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.formatThaiDate
import com.iching.ai.ui.formatThaiTime
import com.iching.ai.ui.theme.ZenColors

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onOpenEntry: (Long) -> Unit,
    onStartCasting: () -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PaperBackground(showMountains = false) {
        when (val s = state) {
            JournalUiState.Loading -> Unit
            is JournalUiState.Loaded -> if (s.isEmpty) {
                EmptyJournal(onStartCasting)
            } else {
                Timeline(s.months, s.entryCount, s.hiddenCount, s.journalDays, onOpenEntry, onOpenPaywall)
            }
        }
    }
}

@Composable
private fun Timeline(
    months: List<JournalMonth>,
    entryCount: Int,
    hiddenCount: Int,
    journalDays: Int?,
    onOpenEntry: (Long) -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val bottom = ZenSpacing.tabBar + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + ZenSpacing.lg
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = ZenSpacing.screen, end = ZenSpacing.screen, top = ZenSpacing.md, bottom = bottom),
    ) {
        item {
            Text("บันทึกชะตา", style = MaterialTheme.typography.displaySmall, color = ZenColors.InkBlack)
            Text(
                if (journalDays == null) "ย้อนมองเส้นทางที่ผ่านมา $entryCount ครั้งแห่งการถามไถ่"
                else "${journalDays} วันล่าสุด · $entryCount ครั้งแห่งการถามไถ่",
                style = MaterialTheme.typography.bodyMedium,
                color = ZenColors.InkSoft,
            )
            Spacer(Modifier.height(16.dp))
        }
        if (hiddenCount > 0) {
            item(key = "locked") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ZenColors.Card)
                        .clickable(onClick = onOpenPaywall)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("บันทึกเก่าอีก $hiddenCount รายการ", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "ยังเก็บไว้ในเครื่องของท่าน · เปิดดูทั้งหมดและกู้คืนข้ามเครื่องได้ด้วย Premium",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZenColors.InkSoft,
                        )
                    }
                    Text("ดูแพ็กเกจ", style = MaterialTheme.typography.labelMedium, color = ZenColors.CinnabarRed)
                }
            }
        }
        months.forEach { month ->
            item(key = "month-${month.label}", contentType = "month") {
                SectionLabel(month.label, modifier = Modifier.padding(top = ZenSpacing.md, bottom = ZenSpacing.sm, start = 2.dp))
            }
            itemsIndexed(month.entries, key = { _, e -> e.reading.id }, contentType = { _, _ -> "reading" }) { index, entry ->
                TimelineItem(
                    entry,
                    first = index == 0,
                    last = index == month.entries.lastIndex,
                    onClick = { onOpenEntry(entry.reading.id) },
                )
            }
        }
    }
}

/** Marks a reading of the year hexagram in the journal. */
internal const val YEAR_READING_LABEL = "กวาประจำปี"

/** A reading row; the rows of one month read as a single card, rounded only at its ends. */
@Composable
private fun TimelineItem(entry: JournalEntry, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val reading = entry.reading
    val corner = 16.dp
    val shape = RoundedCornerShape(
        topStart = if (first) corner else 0.dp,
        topEnd = if (first) corner else 0.dp,
        bottomStart = if (last) corner else 0.dp,
        bottomEnd = if (last) corner else 0.dp,
    )
    Column(Modifier.fillMaxWidth().clip(shape).background(ZenColors.Card)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        append("${formatThaiShortDate(reading.createdAtMillis)} · ${formatThaiTime(reading.createdAtMillis).removeSuffix(" น.")}")
                        if (reading.source == InterpretationSource.SCRIPTURE) append(" · ตัวบทคัมภีร์")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenColors.InkFaint,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    reading.question.ifBlank { "อธิษฐานในใจ" },
                    style = MaterialTheme.typography.titleSmall,
                    color = ZenColors.InkBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append("${entry.primary.nameCn} ${entry.primary.shortNameTh}")
                        entry.relating?.let { append(" → ${it.nameCn} ${it.shortNameTh}") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenColors.BambooDeep,
                )
                if (reading.yearOf != null) {
                    Text(YEAR_READING_LABEL, style = MaterialTheme.typography.labelSmall, color = ZenColors.CinnabarRed)
                }
                reading.outcome?.let {
                    Text("✓ ${outcomeLabel(it)}", style = MaterialTheme.typography.labelSmall, color = ZenColors.CinnabarRed)
                }
            }
            Spacer(Modifier.width(12.dp))
            HexagramFigure(
                pattern = entry.primary.pattern,
                lines = reading.lines,
                lineWidth = 34.dp,
                lineHeight = 5.dp,
                gap = 3.dp,
            )
        }
        if (!last) ZenCardDivider()
    }
}

@Composable
private fun EmptyJournal(onStartCasting: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CinnabarSeal(text = "命錄", size = 64.dp)
        Spacer(Modifier.height(24.dp))
        Text("บันทึกชะตายังว่างเปล่า", style = MaterialTheme.typography.headlineSmall, color = ZenColors.InkBlack)
        Spacer(Modifier.height(8.dp))
        Text(
            "ทุกครั้งที่ท่านเสี่ยงทาย คำถามและคำชี้แนะจะถูกจารึกไว้ที่นี่\nให้ย้อนกลับมาทบทวนตามกาลเวลา",
            style = MaterialTheme.typography.bodyMedium,
            color = ZenColors.InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        ZenPrimaryButton("เสี่ยงทายครั้งแรก", onClick = onStartCasting)
    }
}

@Composable
fun JournalEntryScreen(
    viewModel: JournalEntryViewModel,
    onBack: () -> Unit,
    onOpenHexagram: (Int) -> Unit,
    onAskHermit: (Reading) -> Unit,
) {
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    PaperBackground(showMountains = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "กลับ", tint = ZenColors.InkBlack)
                }
                Text("บันทึกชะตา", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "ลบบันทึก", tint = ZenColors.InkSoft)
                }
            }
            val e = entry ?: return@Column
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                CastSummary(
                    question = e.reading.question,
                    lines = e.reading.lines,
                    primary = e.primary,
                    relating = e.relating,
                    createdAtMillis = e.reading.createdAtMillis,
                    onHexagramClick = onOpenHexagram,
                )
                Spacer(Modifier.height(24.dp))
                OracleSheet(
                    text = e.reading.interpretation,
                    isComplete = true,
                    animate = false,
                    source = e.reading.source,
                )
                AiContentLabel(source = e.reading.source, onReport = null)
                if (e.reading.source == InterpretationSource.SCRIPTURE && e.reading.serverId != null) {
                    Spacer(Modifier.height(16.dp))
                    AskHermitCard(onClick = { onAskHermit(e.reading) })
                }
                if (e.reading.followUps.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    SectionLabel("ถามต่อกับนักพรต", color = ZenColors.CinnabarRed)
                    Spacer(Modifier.height(10.dp))
                    e.reading.followUps.forEach { turn ->
                        Text("“${turn.question}”", style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
                        Spacer(Modifier.height(6.dp))
                        Text(turn.answer, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkBlack)
                        Spacer(Modifier.height(14.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
                OutcomeSection(e.reading.outcome, e.reading.outcomeNote, viewModel::setOutcome)
                Spacer(Modifier.height(24.dp))
                ScriptureSection(e.primary, e.relating, e.reading.changingPositions)
                Spacer(Modifier.height(40.dp))
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("ลบบันทึกนี้?") },
            text = { Text("บันทึกที่ลบแล้วจะไม่สามารถนำกลับคืนมาได้") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("ลบ", color = ZenColors.CinnabarRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("ยกเลิก") } },
            containerColor = ZenColors.PaperCream,
        )
    }
}

private fun outcomeLabel(outcome: Outcome) = when (outcome) {
    Outcome.YES -> "เกิดขึ้นจริง"
    Outcome.PARTLY -> "เกิดขึ้นบางส่วน"
    Outcome.NO -> "ไม่เกิดขึ้น"
}

/** "Did it come true?" with a private note — the heart of the Destiny Journal. */
@Composable
private fun OutcomeSection(outcome: Outcome?, note: String?, onChange: (Outcome?, String?) -> Unit) {
    var draft by rememberSaveable(note) { mutableStateOf(note.orEmpty()) }
    Column(Modifier.fillMaxWidth()) {
        SectionLabel("เกิดขึ้นจริงไหม")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Outcome.entries.forEach { o ->
                val selected = o == outcome
                Text(
                    outcomeLabel(o),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) ZenColors.PaperCream else ZenColors.BambooDeep,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) ZenColors.BambooDeep else ZenColors.PaperCream)
                        .border(1.dp, ZenColors.BambooGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { onChange(if (selected) null else o, draft) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = draft,
            onValueChange = { if (it.length <= 2000) draft = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("จดบันทึกส่วนตัว: สิ่งที่เกิดขึ้น สิ่งที่ได้เรียนรู้...", color = ZenColors.InkFaint) },
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ZenColors.current.outline,
                unfocusedBorderColor = ZenColors.current.outlineVariant,
                focusedContainerColor = ZenColors.Card,
                unfocusedContainerColor = ZenColors.Card,
            ),
        )
        if (draft != note.orEmpty()) {
            TextButton(onClick = { onChange(outcome, draft) }) { Text("บันทึก", color = ZenColors.CinnabarRed) }
        }
    }
}

/** A cast the server knows but the hermit never finished (left mid-reading, out of quota, or a failure). */
@Composable
private fun AskHermitCard(onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZenColors.CinnabarRed.copy(alpha = 0.07f))
            .padding(16.dp),
    ) {
        Text(
            "กวานี้ยังไม่ได้รับคำชี้แนะจากนักพรต หากคำทำนายถูกตัดกลางทาง ท่านรับต่อได้โดยไม่เสียสิทธิ์ซ้ำ",
            style = MaterialTheme.typography.bodySmall,
            color = ZenColors.InkBlack,
        )
        Spacer(Modifier.height(12.dp))
        ZenPrimaryButton("รับคำชี้แนะจากนักพรต", onClick = onClick, modifier = Modifier.fillMaxWidth())
    }
}
