package com.iching.ai.feature.library

import com.iching.ai.ui.components.ZenCardRow
import com.iching.ai.ui.theme.tabContentBottomPadding
import com.iching.ai.ui.theme.LocalTabBarHeight
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.components.ZenChip
import com.iching.ai.ui.components.ZenCardDivider
import com.iching.ai.ui.components.ZenCard
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.foundation.layout.imePadding
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
import com.iching.ai.ui.strings.meaning
import com.iching.ai.ui.strings.shortName
import com.iching.ai.ui.strings.displayName
import com.iching.ai.ui.strings.LocalAppLanguage
import com.iching.ai.ui.strings.LocalStrings
import com.iching.ai.feature.reading.ScriptureBlock
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.InkDivider
import com.iching.ai.ui.components.LineGlyph
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.theme.ZenColors

@Composable
fun LibraryScreen(viewModel: LibraryViewModel, onOpenHexagram: (Int) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val visible = state.visible
    val strings = LocalStrings.current
    val library = strings.library
    val bottom = tabContentBottomPadding(LocalTabBarHeight.current)
    PaperBackground(showMountains = false) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding(),
            contentPadding = PaddingValues(start = ZenSpacing.screen, end = ZenSpacing.screen, top = ZenSpacing.md, bottom = bottom),
        ) {
            item(contentType = "header") {
                Column {
                    Text(library.title, style = MaterialTheme.typography.displaySmall, color = ZenColors.InkBlack)
                    Text(library.subtitle, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
                    Spacer(Modifier.height(ZenSpacing.lg))
                    TextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = ZenColors.InkFaint) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Outlined.Close, library.clear, tint = ZenColors.InkFaint)
                                }
                            }
                        },
                        placeholder = { Text(library.searchPlaceholder, color = ZenColors.InkFaint) },
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ZenColors.Card,
                            unfocusedContainerColor = ZenColors.Card,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = ZenColors.InkBlack,
                            unfocusedTextColor = ZenColors.InkBlack,
                            cursorColor = ZenColors.CinnabarRed,
                        ),
                    )
                    Spacer(Modifier.height(ZenSpacing.md))
                }
            }
            item(contentType = "filter") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(ZenSpacing.sm)) {
                    item { ZenChip(library.filterAll, state.trigram == null, onClick = { viewModel.onTrigramChange(null) }) }
                    items(Trigram.entries) { t ->
                        ZenChip("${t.symbol} ${strings.common.trigramNature(t)}", state.trigram == t, onClick = { viewModel.onTrigramChange(t) })
                    }
                }
                Spacer(Modifier.height(ZenSpacing.md))
            }
            itemsIndexed(visible, key = { _, h -> h.id }, contentType = { _, _ -> "hexagram" }) { index, hexagram ->
                HexagramRow(
                    hexagram,
                    first = index == 0,
                    last = index == visible.lastIndex,
                    onClick = { onOpenHexagram(hexagram.id) },
                )
            }
            if (!state.isLoading && visible.isEmpty()) {
                item(contentType = "empty") {
                    Text(
                        if (state.query.isBlank()) library.noResults else library.noResultsFor(state.query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZenColors.InkSoft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
        }
    }
}

/** One row of the list; consecutive rows read as a single card (rounded only at the ends). */
@Composable
private fun HexagramRow(hexagram: Hexagram, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val lang = LocalAppLanguage.current
    ZenCardRow(first, last) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                LocalStrings.current.common.number(hexagram.id),
                style = MaterialTheme.typography.labelMedium,
                color = ZenColors.CinnabarRed,
                modifier = Modifier.width(24.dp),
            )
            HexagramFigure(hexagram.pattern, lineWidth = 30.dp, lineHeight = 4.dp, gap = 3.dp)
            Spacer(Modifier.width(ZenSpacing.md))
            Column(Modifier.weight(1f)) {
                Text(hexagram.displayName(lang), style = MaterialTheme.typography.titleSmall, color = ZenColors.InkBlack)
                val meaning = hexagram.meaning(lang)
                if (meaning.isNotEmpty()) {
                    Text(
                        meaning,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZenColors.InkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun HexagramDetailScreen(viewModel: HexagramDetailViewModel, onBack: () -> Unit) {
    val hexagram by viewModel.hexagram.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val lang = LocalAppLanguage.current
    val library = strings.library
    PaperBackground(showMountains = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.common.back, tint = ZenColors.InkBlack)
            }
            val h = hexagram ?: return@Column
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = ZenSpacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(library.hexagramNumber(h.id), style = MaterialTheme.typography.labelLarge, color = ZenColors.CinnabarRed)
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
                Text("${h.shortName(lang)} · ${h.pinyin}", style = MaterialTheme.typography.titleMedium, color = ZenColors.InkSoft)
                val meaning = h.meaning(lang)
                if (meaning.isNotEmpty()) {
                    Text(meaning, style = MaterialTheme.typography.titleLarge, color = ZenColors.BambooDeep, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrigramChip(library.upperTrigram, h.upperTrigram)
                    TrigramChip(library.lowerTrigram, h.lowerTrigram)
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
                Spacer(Modifier.height(ZenSpacing.lg))
                ZenCard(contentPadding = PaddingValues(20.dp)) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ScriptureBlock(strings.reading.judgment, h.judgment, ZenColors.CinnabarRed)
                    ScriptureBlock(strings.reading.image, h.image)
                    ScriptureBlock(library.overview, h.description)
                    SectionLabel(library.sixLines, color = ZenColors.InkSoft)
                    for (position in 6 downTo 1) {
                        LineRow(position, h.pattern[position - 1] == '1', h.lines[position - 1])
                    }
                    h.allLines?.let { ScriptureBlock(library.whenAllLinesChange, it, ZenColors.CinnabarRed) }
                }
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
            .clip(RoundedCornerShape(12.dp))
            .background(ZenColors.Card)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(trigram.symbol, style = MaterialTheme.typography.titleLarge, color = ZenColors.BambooDeep)
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint)
            Text("${LocalStrings.current.common.trigramNature(trigram)} ${trigram.nameCn}", style = MaterialTheme.typography.labelLarge, color = ZenColors.InkBlack)
        }
    }
}

@Composable
private fun LineRow(position: Int, isYang: Boolean, text: String) {
    val library = LocalStrings.current.library
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.width(56.dp).padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LineGlyph(isYang, Modifier.size(width = 34.dp, height = 5.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(library.linePosition(position), style = MaterialTheme.typography.labelMedium, color = ZenColors.BambooDeep)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkBlack)
        }
    }
}
