package com.iching.ai.feature.daily

import com.iching.ai.ui.strings.LocalStrings
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.components.ZenCard
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
import androidx.compose.runtime.LaunchedEffect
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
import com.iching.ai.feature.home.campaignLine
import com.iching.ai.feature.share.ShareCardSheet
import com.iching.ai.feature.share.dailyShareContent
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.InkDivider
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.domain.model.PosterLink
import com.iching.ai.feature.poster.PosterCardRow
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenSecondaryButton
import com.iching.ai.ui.theme.ZenColors

@Composable
fun DailyScreen(
    viewModel: DailyViewModel,
    onBack: () -> Unit,
    onOpenHexagram: (Int) -> Unit,
    onOpenLink: (PosterLink) -> Unit,
    onOpenYear: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val card by viewModel.posterCard.collectAsStateWithLifecycle()
    val campaign by viewModel.campaignCard.collectAsStateWithLifecycle()
    var sharing by remember { mutableStateOf(false) }
    val strings = LocalStrings.current.daily
    val common = LocalStrings.current.common

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    PaperBackground(showMountains = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = common.back, tint = ZenColors.InkBlack)
                }
                Text(strings.title, style = MaterialTheme.typography.titleLarge, color = ZenColors.InkBlack)
            }
            val hexagram = state.hexagram ?: return@Column
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = ZenSpacing.screen, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HexagramFigure(hexagram.pattern, lineWidth = 96.dp, lineHeight = 11.dp, gap = 8.dp)
                Spacer(Modifier.height(18.dp))
                Text(hexagram.nameCn, style = MaterialTheme.typography.displayMedium, color = ZenColors.InkBlack)
                Text(
                    "${common.number(hexagram.id)} · ${hexagram.shortNameTh}",
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
                val week = state.week
                if (week != null) {
                    StreakWeekRow(week)
                } else {
                    Text(
                        strings.streakLine(state.streak.current),
                        style = MaterialTheme.typography.labelMedium,
                        color = ZenColors.BambooDeep,
                    )
                }
                Spacer(Modifier.height(ZenSpacing.lg))
                ZenCard {
                    SectionLabel(strings.judgment, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(hexagram.judgment, style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkBlack, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(24.dp))
                ZenPrimaryButton(strings.shareToday, onClick = { sharing = true }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                ZenSecondaryButton(strings.readMore, onClick = { onOpenHexagram(hexagram.id) }, modifier = Modifier.fillMaxWidth())
                val year = campaign
                campaignLine(year, LocalStrings.current.home)?.let { line ->
                    LaunchedEffect(year::class) { viewModel.onCampaignCardShown(year) }
                    Spacer(Modifier.height(16.dp))
                    CampaignLineRow(line, onClick = {
                        viewModel.onCampaignCardTapped(year)
                        onOpenYear()
                    })
                }
                card?.let { c ->
                    LaunchedEffect(c.id) { viewModel.onPosterCardShown(c.id) }
                    Spacer(Modifier.height(16.dp))
                    PosterCardRow(
                        c,
                        onClick = {
                            viewModel.onPosterCardTapped(c.id)
                            onOpenLink(c.link)
                        },
                        onDismiss = { viewModel.onPosterCardClosed(c.id) },
                    )
                }
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
            title = { Text(strings.unlockTitle(unlocked.labelTh)) },
            text = { Text(strings.unlockBody(unlocked.unlockDays)) },
            confirmButton = { TextButton(onClick = viewModel::unlockNoticeShown) { Text(strings.acknowledge) } },
        )
        state.showPushPrompt -> AlertDialog(
            onDismissRequest = viewModel::dismissPushPrompt,
            title = { Text(strings.pushTitle) },
            text = { Text(strings.pushBody) },
            confirmButton = { TextButton(onClick = viewModel::acceptPushPrompt) { Text(strings.pushAccept) } },
            dismissButton = { TextButton(onClick = viewModel::dismissPushPrompt) { Text(strings.pushLater) } },
        )
    }
}
