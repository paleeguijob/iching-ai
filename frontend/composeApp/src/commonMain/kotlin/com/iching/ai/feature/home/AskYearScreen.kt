package com.iching.ai.feature.home

import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.components.ZenChip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.theme.ZenColors

/**
 * The home screen's question box for "ask the hermit about this year": the category is fixed to
 * new year and the pre-cast offer works as it does at home. [onCast] opens the year reading.
 */
@Composable
fun AskYearScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onCast: (String) -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val deciding by viewModel.deciding.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.castRequests.collect { onCast(it.question) }
    }
    sheet?.let { ui ->
        PreCastSheet(
            ui = ui,
            onWatchAd = viewModel::onWatchAd,
            onBuyCoins = viewModel::onBuyCoins,
            onScriptureOnly = viewModel::onScriptureOnly,
            onPremium = {
                viewModel.onPremium()
                onOpenPaywall()
            },
            onCastFromReady = viewModel::onCastFromReady,
            onDismiss = viewModel::onSheetDismissed,
        )
    }
    var question by rememberSaveable { mutableStateOf(QuestionCategory.NEWYEAR.sampleTh) }

    PaperBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "กลับ", tint = ZenColors.InkBlack)
                }
                Text("ถามนักพรตเรื่องปีนี้", style = MaterialTheme.typography.headlineSmall, color = ZenColors.InkBlack)
            }
            Column(Modifier.padding(horizontal = ZenSpacing.screen)) {
                Spacer(Modifier.height(24.dp))
                QuestionField(question, onChange = { question = it })
                Spacer(Modifier.height(12.dp))
                ZenChip(QuestionCategory.NEWYEAR.labelTh, selected = true, onClick = {})
                Spacer(Modifier.height(24.dp))
                GuidanceQuotaBar(guidanceQuota(status))
                Spacer(Modifier.height(12.dp))
                ZenPrimaryButton(
                    text = startButtonText(deciding),
                    onClick = { viewModel.onStart(question.trim(), QuestionCategory.NEWYEAR) },
                    enabled = !deciding,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
