package com.iching.ai.feature.paywall

import com.iching.ai.ui.theme.ZenSpacing
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iching.ai.data.BuildConfig
import com.iching.ai.domain.model.ProductKind
import com.iching.ai.domain.model.StoreProduct
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenSecondaryButton
import com.iching.ai.ui.strings.LocalStrings
import com.iching.ai.ui.theme.ZenColors
import kotlinx.coroutines.delay

@Composable
fun PaywallScreen(viewModel: PaywallViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    val strings = LocalStrings.current
    val paywall = strings.paywall
    LaunchedEffect(state.purchased) {
        if (state.purchased) {
            delay(1600)
            onClose()
        }
    }

    PaperBackground(showMountains = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = strings.common.close, tint = ZenColors.InkBlack) }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = ZenSpacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CinnabarSeal(text = "道", size = 56.dp)
                Spacer(Modifier.height(14.dp))
                Text(paywall.title, style = MaterialTheme.typography.headlineMedium, color = ZenColors.InkBlack)
                Text(
                    paywall.tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZenColors.InkSoft,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    paywall.benefits.forEach { benefit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = ZenColors.BambooDeep, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(benefit, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))

                if (state.status.premium) {
                    Text(
                        paywall.memberLine(state.status.premiumUntilMillis),
                        style = MaterialTheme.typography.titleSmall,
                        color = ZenColors.BambooDeep,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                when {
                    state.loading -> CircularProgressIndicator(color = ZenColors.BambooDeep)
                    state.products.isEmpty() -> ZenSecondaryButton(paywall.reload, onClick = viewModel::load)
                    else -> {
                        if (!state.status.premium) {
                            state.subscriptions.forEach { product ->
                                PlanCard(product, selected = product == state.selected, onClick = { viewModel.select(product) })
                                Spacer(Modifier.height(10.dp))
                            }
                            val selected = state.selected
                            if (selected != null && selected.kind != ProductKind.COINS) {
                                Spacer(Modifier.height(6.dp))
                                ZenPrimaryButton(
                                    text = if (state.busy) strings.common.working else paywall.trialLabel(selected)?.let(paywall::startTrial) ?: paywall.subscribe(selected),
                                    onClick = { viewModel.buy() },
                                    enabled = !state.busy,
                                    color = ZenColors.CinnabarRed,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    paywall.renewalTerms(selected),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZenColors.InkFaint,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        state.coins?.let { coins ->
                            Spacer(Modifier.height(24.dp))
                            SectionLabel(paywall.coinsHeading)
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ZenColors.Card)
                                    .border(1.dp, ZenColors.current.outlineVariant, RoundedCornerShape(16.dp))
                                    .clickable(enabled = !state.busy) { viewModel.buy(coins) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(strings.common.coinPack, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        paywall.coinsNote(state.status.coins),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ZenColors.InkFaint,
                                    )
                                }
                                Text(coins.price, style = MaterialTheme.typography.titleMedium, color = ZenColors.CinnabarRed)
                            }
                        }
                    }
                }

                state.message?.let {
                    Spacer(Modifier.height(14.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = ZenColors.CinnabarRed, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = viewModel::restore, enabled = !state.busy) {
                        Text(paywall.restore, color = ZenColors.InkSoft, style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = { runCatching { uri.openUri(viewModel.manageSubscriptionsUrl) } }) {
                        Text(paywall.manage, color = ZenColors.InkSoft, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { runCatching { uri.openUri("${BuildConfig.WEB_BASE_URL}${paywall.termsPath}") } }) {
                        Text(paywall.terms, color = ZenColors.InkFaint, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { runCatching { uri.openUri("${BuildConfig.WEB_BASE_URL}${paywall.privacyPath}") } }) {
                        Text(paywall.privacy, color = ZenColors.InkFaint, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PlanCard(product: StoreProduct, selected: Boolean, onClick: () -> Unit) {
    val paywall = LocalStrings.current.paywall
    val border = if (selected) ZenColors.InkBlack else ZenColors.current.outlineVariant
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZenColors.Card)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (product.kind == ProductKind.YEARLY) paywall.planYearly else paywall.planMonthly, style = MaterialTheme.typography.titleSmall)
                if (product.kind == ProductKind.YEARLY) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        paywall.bestValue,
                        style = MaterialTheme.typography.labelSmall,
                        color = ZenColors.PaperCream,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ZenColors.CinnabarRed).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            paywall.trialLabel(product)?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = ZenColors.BambooDeep) }
        }
        Text(paywall.pricePerPeriod(product), style = MaterialTheme.typography.titleMedium, color = ZenColors.InkBlack)
    }
}
