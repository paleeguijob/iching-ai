package com.iching.ai.feature.library

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.Trigram
import com.iching.ai.domain.oracle.linePositionTh
import com.iching.ai.feature.reading.ScriptureBlock
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.InkDivider
import com.iching.ai.ui.components.LineGlyph
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.toThaiNumerals

@Composable
fun LibraryScreen(viewModel: LibraryViewModel, onOpenHexagram: (Int) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PaperBackground(showMountains = false) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text("คัมภีร์ ๖๔ กวา", style = MaterialTheme.typography.headlineLarge, color = ZenColors.InkBlack)
                    Text(
                        "สารานุกรมแห่งความเปลี่ยนแปลง สำหรับผู้ใคร่ศึกษาด้วยตนเอง",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZenColors.InkSoft,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = ZenColors.InkFaint) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Outlined.Close, "ล้าง", tint = ZenColors.InkFaint)
                                }
                            }
                        },
                        placeholder = { Text("ค้นหาด้วยชื่อ เลข หรือความหมาย", color = ZenColors.InkFaint) },
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ZenColors.BambooGreen,
                            unfocusedBorderColor = ZenColors.InkBlack.copy(alpha = 0.12f),
                            focusedContainerColor = ZenColors.PaperCream,
                            unfocusedContainerColor = ZenColors.PaperCream,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
            items(state.hexagrams, key = { it.id }) { hexagram ->
                HexagramTile(hexagram, onClick = { onOpenHexagram(hexagram.id) })
            }
            if (!state.isLoading && state.hexagrams.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "ไม่พบกวาที่ตรงกับ “${state.query}”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZenColors.InkFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HexagramTile(hexagram: Hexagram, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ZenColors.PaperCream.copy(alpha = 0.9f))
            .border(1.dp, ZenColors.InkBlack.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(hexagram.id.toThaiNumerals(), style = MaterialTheme.typography.labelMedium, color = ZenColors.CinnabarRed)
            Spacer(Modifier.weight(1f))
            Text(
                "${hexagram.upperTrigram.symbol}${hexagram.lowerTrigram.symbol}",
                style = MaterialTheme.typography.labelMedium,
                color = ZenColors.InkFaint,
            )
        }
        Spacer(Modifier.height(6.dp))
        HexagramFigure(hexagram.pattern, lineWidth = 56.dp, lineHeight = 7.dp, gap = 5.dp)
        Spacer(Modifier.height(10.dp))
        Text(hexagram.nameCn, style = MaterialTheme.typography.headlineSmall, color = ZenColors.InkBlack)
        Text(hexagram.shortNameTh, style = MaterialTheme.typography.titleSmall, color = ZenColors.InkBlack)
        Text(
            hexagram.meaningTh,
            style = MaterialTheme.typography.bodySmall,
            color = ZenColors.InkSoft,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(42.dp),
        )
    }
}

@Composable
fun HexagramDetailScreen(viewModel: HexagramDetailViewModel, onBack: () -> Unit) {
    val hexagram by viewModel.hexagram.collectAsStateWithLifecycle()
    PaperBackground(showMountains = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "กลับ", tint = ZenColors.InkBlack)
            }
            val h = hexagram ?: return@Column
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("กวาที่ ${h.id.toThaiNumerals()}", style = MaterialTheme.typography.labelLarge, color = ZenColors.CinnabarRed)
                Spacer(Modifier.height(16.dp))
                Box(contentAlignment = Alignment.TopEnd) {
                    HexagramFigure(h.pattern, lineWidth = 132.dp, lineHeight = 15.dp, gap = 11.dp, modifier = Modifier.padding(12.dp))
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(h.nameCn, style = MaterialTheme.typography.displayMedium, color = ZenColors.InkBlack)
                    Spacer(Modifier.width(12.dp))
                    CinnabarSeal(text = h.nameCn.take(2), size = 40.dp)
                }
                Text("${h.shortNameTh} · ${h.pinyin}", style = MaterialTheme.typography.titleMedium, color = ZenColors.InkSoft)
                Text(h.meaningTh, style = MaterialTheme.typography.titleLarge, color = ZenColors.BambooDeep, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrigramChip("ตรีแกรมบน", h.upperTrigram)
                    TrigramChip("ตรีแกรมล่าง", h.lowerTrigram)
                }
                if (h.keywords.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        h.keywords.joinToString("  ·  "),
                        style = MaterialTheme.typography.labelMedium,
                        color = ZenColors.InkFaint,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(24.dp))
                InkDivider()
                Spacer(Modifier.height(20.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ScriptureBlock("คำตัดสิน", h.judgment, ZenColors.CinnabarRed)
                    ScriptureBlock("ภาพลักษณ์", h.image)
                    ScriptureBlock("ภาพรวม", h.description)
                    SectionLabel("เส้นทั้งหก (จากบนลงล่าง)", color = ZenColors.InkSoft)
                    for (position in 6 downTo 1) {
                        LineRow(position, h.pattern[position - 1] == '1', h.lines[position - 1])
                    }
                    h.allLines?.let { ScriptureBlock("เมื่อเส้นทั้งหกแปร", it, ZenColors.CinnabarRed) }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun TrigramChip(label: String, trigram: Trigram) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ZenColors.BambooGreen.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(trigram.symbol, style = MaterialTheme.typography.titleLarge, color = ZenColors.BambooDeep)
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint)
            Text("${trigram.natureTh} ${trigram.nameCn}", style = MaterialTheme.typography.labelLarge, color = ZenColors.InkBlack)
        }
    }
}

@Composable
private fun LineRow(position: Int, isYang: Boolean, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.width(56.dp).padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LineGlyph(isYang, Modifier.size(width = 34.dp, height = 5.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(linePositionTh(position), style = MaterialTheme.typography.labelMedium, color = ZenColors.BambooDeep)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkBlack)
        }
    }
}
