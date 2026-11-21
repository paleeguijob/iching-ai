package com.iching.ai.feature.journal

import com.iching.ai.navigation.SiamSiRoute
import com.iching.ai.ui.components.ZenCardRow
import com.iching.ai.ui.theme.tabContentBottomPadding
import com.iching.ai.ui.theme.LocalTabBarHeight
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.strings.displayName
import com.iching.ai.ui.strings.LocalAppLanguage
import com.iching.ai.ui.strings.formatShortDate
import com.iching.ai.ui.strings.formatTime
import com.iching.ai.ui.strings.JournalStrings
import com.iching.ai.ui.strings.LocalStrings
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
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.feature.siamsi.InterpretUi
import com.iching.ai.feature.siamsi.SiamSiSlipContent
import com.iching.ai.feature.share.SiamSiShareSheet
import com.iching.ai.feature.share.siamsiShareContent
import com.iching.ai.ui.components.ZenChip
import com.iching.ai.ui.components.ZenSecondaryButton
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.theme.ZenColors

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onOpenEntry: (Long) -> Unit,
    onStartCasting: () -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    PaperBackground(showMountains = false) {
        when (val s = state) {
            JournalUiState.Loading -> Unit
            is JournalUiState.Loaded -> if (s.isEmpty && filter == JournalFilter.ALL) {
                EmptyJournal(onStartCasting)
            } else {
                Timeline(s.months, s.entryCount, s.hiddenCount, s.journalDays, onOpenEntry, onOpenPaywall, s.hasSiamSi, filter, viewModel::onFilterSelected)
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
    hasSiamSi: Boolean,
    filter: JournalFilter,
    onFilter: (JournalFilter) -> Unit,
) {
    val bottom = tabContentBottomPadding(LocalTabBarHeight.current)
    val strings = LocalStrings.current.journal
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = ZenSpacing.screen, end = ZenSpacing.screen, top = ZenSpacing.md, bottom = bottom),
    ) {
        item {
            Text(strings.title, style = MaterialTheme.typography.displaySmall, color = ZenColors.InkBlack)
            Text(
                strings.subtitle(journalDays, entryCount),
                style = MaterialTheme.typography.bodyMedium,
                color = ZenColors.InkSoft,
            )
            if (hasSiamSi) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JournalFilter.entries.forEach { f ->
                        val label = when (f) {
                            JournalFilter.ALL -> strings.filterAll
                            JournalFilter.ICHING -> strings.filterIching
                            JournalFilter.SIAMSI -> strings.filterSiamsi
                        }
                        ZenChip(label, f == filter, onClick = { onFilter(f) })
                    }
                }
            }
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
                        Text(strings.hiddenTitle(hiddenCount), style = MaterialTheme.typography.titleSmall)
                        Text(
                            strings.hiddenBody,
                            style = MaterialTheme.typography.labelSmall,
                            color = ZenColors.InkSoft,
                        )
                    }
                    Spacer(Modifier.width(ZenSpacing.md))
                    Text(strings.seePlans, style = MaterialTheme.typography.labelMedium, color = ZenColors.CinnabarRed)
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

/** A reading row; the rows of one month read as a single card, rounded only at its ends. */
@Composable
private fun TimelineItem(entry: JournalEntry, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val reading = entry.reading
    val strings = LocalStrings.current.journal
    val lang = LocalAppLanguage.current
    ZenCardRow(first, last) {
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
                        append("${formatShortDate(reading.createdAtMillis, lang)} · ${formatTime(reading.createdAtMillis, lang).substringBefore(" ")}")
                        if (reading.siamsi != null) append(" · ${strings.siamsiTag}")
                        else if (reading.source == InterpretationSource.SCRIPTURE) append(" · ${strings.scriptureTag}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenColors.InkFaint,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    reading.question.ifBlank { strings.prayedInHeart },
                    style = MaterialTheme.typography.titleSmall,
                    color = ZenColors.InkBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.siamsi?.label ?: buildString {
                        entry.primary?.let { append(it.displayName(lang)) }
                        entry.relating?.let { append(" → ${it.displayName(lang)}") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenColors.BambooDeep,
                )
                if (reading.yearOf != null) {
                    Text(strings.yearReading, style = MaterialTheme.typography.labelSmall, color = ZenColors.CinnabarRed)
                }
                reading.outcome?.let {
                    Text("✓ ${outcomeLabel(it, strings)}", style = MaterialTheme.typography.labelSmall, color = ZenColors.CinnabarRed)
                }
            }
            Spacer(Modifier.width(12.dp))
            val primary = entry.primary
            if (primary != null) {
                HexagramFigure(
                    pattern = primary.pattern,
                    lines = reading.lines,
                    lineWidth = 34.dp,
                    lineHeight = 5.dp,
                    gap = 3.dp,
                )
            } else {
                SiamSiStickIcon()
            }
        }
    }
}

@Composable
private fun EmptyJournal(onStartCasting: () -> Unit) {
    val strings = LocalStrings.current.journal
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CinnabarSeal(text = "命錄", size = 64.dp)
        Spacer(Modifier.height(24.dp))
        Text(strings.emptyTitle, style = MaterialTheme.typography.headlineSmall, color = ZenColors.InkBlack)
        Spacer(Modifier.height(8.dp))
        Text(
            strings.emptyBody,
            style = MaterialTheme.typography.bodyMedium,
            color = ZenColors.InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        ZenPrimaryButton(strings.emptyAction, onClick = onStartCasting)
    }
}

@Composable
fun JournalEntryScreen(
    viewModel: JournalEntryViewModel,
    onBack: () -> Unit,
    onOpenHexagram: (Int) -> Unit,
    onAskHermit: (Reading) -> Unit,
    onInterpretSiamSi: (SiamSiRoute) -> Unit,
) {
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val journal = strings.journal
    var confirmDelete by remember { mutableStateOf(false) }
    PaperBackground(showMountains = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.common.back, tint = ZenColors.InkBlack)
                }
                Text(journal.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = journal.deleteDescription, tint = ZenColors.InkSoft)
                }
            }
            val e = entry ?: return@Column
            val slip = e.siamsi
            if (slip != null) {
                SiamSiJournalDetail(e.reading, slip, viewModel::setOutcome, viewModel::logEvent, onInterpretSiamSi)
                return@Column
            }
            val primary = e.primary ?: return@Column
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
                    primary = primary,
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
                    SectionLabel(strings.divination.followUpTitle, color = ZenColors.CinnabarRed)
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
                ScriptureSection(primary, e.relating, e.reading.changingPositions)
                Spacer(Modifier.height(40.dp))
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(journal.deleteTitle) },
            text = { Text(journal.deleteBody) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text(journal.delete, color = ZenColors.CinnabarRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(strings.common.cancel) } },
            containerColor = ZenColors.PaperCream,
        )
    }
}

/** A Siam Si entry: the slip read-only with its saved interpretation, or just its number when the set is gone. */
@Composable
private fun SiamSiJournalDetail(
    reading: Reading,
    slip: SiamSiJournal,
    onOutcome: (Outcome?, String?) -> Unit,
    log: (String, Map<String, String>) -> Unit,
    onInterpret: (SiamSiRoute) -> Unit,
) {
    var sharing by remember { mutableStateOf(false) }
    val strings = LocalStrings.current
    val journal = strings.journal
    val lang = LocalAppLanguage.current
    val interpretRoute = siamsiInterpretRoute(reading)
    val set = slip.set
    val stick = slip.stick
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            "${formatShortDate(reading.createdAtMillis, lang)} · ${formatTime(reading.createdAtMillis, lang)}",
            style = MaterialTheme.typography.bodySmall,
            color = ZenColors.InkFaint,
        )
        Spacer(Modifier.height(12.dp))
        if (set != null && stick != null) {
            SiamSiSlipContent(
                set = set,
                stick = stick,
                question = reading.question,
                category = reading.category,
                interpret = InterpretUi(text = reading.interpretation),
                status = AccountStatus.UNKNOWN_FREE,
                onInterpret = interpretRoute?.let { route -> { onInterpret(route) } },
                onSendFollowUp = null,
                onOpenPaywall = {},
                animateInterpretation = false,
                onShare = { sharing = true },
                showQuestion = true,
            )
            if (reading.interpretation.isBlank()) {
                Spacer(Modifier.height(16.dp))
                ZenSecondaryButton(journal.shareSlip, onClick = { sharing = true }, modifier = Modifier.fillMaxWidth())
            }
            if (sharing) {
                SiamSiShareSheet(siamsiShareContent(set, stick, strings.share, strings.common), log = log, onDismiss = { sharing = false })
            }
        } else {
            Text(slip.label, style = MaterialTheme.typography.titleMedium, color = ZenColors.InkBlack)
            Text(journal.setMissing, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkFaint)
            if (reading.interpretation.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                OracleSheet(text = reading.interpretation, isComplete = true, animate = false, source = reading.source)
            }
        }
        if (reading.followUps.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionLabel(strings.divination.followUpTitle, color = ZenColors.CinnabarRed)
            Spacer(Modifier.height(10.dp))
            reading.followUps.forEach { turn ->
                Text("“${turn.question}”", style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
                Spacer(Modifier.height(6.dp))
                Text(turn.answer, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkBlack)
                Spacer(Modifier.height(14.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        OutcomeSection(reading.outcome, reading.outcomeNote, onOutcome)
        Spacer(Modifier.height(40.dp))
    }
}

/** A small Siam Si stick in place of the hexagram figure on a journal row. */
@Composable
private fun SiamSiStickIcon() {
    Box(
        Modifier.size(width = 34.dp, height = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 7.dp, height = 38.dp)
                .graphicsLayer { rotationZ = 14f }
                .clip(RoundedCornerShape(3.dp))
                .background(ZenColors.current.scrollWood),
        )
    }
}

private fun outcomeLabel(outcome: Outcome, strings: JournalStrings) = when (outcome) {
    Outcome.YES -> strings.outcomeYes
    Outcome.PARTLY -> strings.outcomePartly
    Outcome.NO -> strings.outcomeNo
}

/** "Did it come true?" with a private note — the heart of the Destiny Journal. */
@Composable
private fun OutcomeSection(outcome: Outcome?, note: String?, onChange: (Outcome?, String?) -> Unit) {
    var draft by rememberSaveable(note) { mutableStateOf(note.orEmpty()) }
    val strings = LocalStrings.current.journal
    Column(Modifier.fillMaxWidth()) {
        SectionLabel(strings.outcomeQuestion)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Outcome.entries.forEach { o ->
                val selected = o == outcome
                Text(
                    outcomeLabel(o, strings),
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
            placeholder = { Text(strings.notePlaceholder, color = ZenColors.InkFaint) },
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
            TextButton(onClick = { onChange(outcome, draft) }) { Text(strings.save, color = ZenColors.CinnabarRed) }
        }
    }
}

/** A cast the server knows but the hermit never finished (left mid-reading, out of quota, or a failure). */
@Composable
private fun AskHermitCard(onClick: () -> Unit) {
    val strings = LocalStrings.current.journal
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZenColors.CinnabarRed.copy(alpha = 0.07f))
            .padding(16.dp),
    ) {
        Text(
            strings.askHermitBody,
            style = MaterialTheme.typography.bodySmall,
            color = ZenColors.InkBlack,
        )
        Spacer(Modifier.height(12.dp))
        ZenPrimaryButton(strings.askHermitAction, onClick = onClick, modifier = Modifier.fillMaxWidth())
    }
}
