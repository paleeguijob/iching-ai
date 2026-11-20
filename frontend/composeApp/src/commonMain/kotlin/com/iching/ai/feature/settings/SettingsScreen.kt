package com.iching.ai.feature.settings

import com.iching.ai.platform.rememberTextSharer
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

import androidx.compose.ui.semantics.selected
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.components.ZenTextButton
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenCardDivider
import com.iching.ai.ui.components.ZenCard
import com.iching.ai.ui.components.CinnabarSeal
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
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
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.paletteFor
import com.iching.ai.ui.strings.LocalStrings
import com.iching.ai.ui.strings.SettingsStrings
import kotlinx.coroutines.delay
import kotlin.time.Clock

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenYear: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenLanguage: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val yearResult by viewModel.yearResult.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    var confirmDelete by remember { mutableStateOf(false) }
    val notificationsGranted by viewModel.notificationsGranted.collectAsStateWithLifecycle()
    val pushBlocked by viewModel.pushBlocked.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val englishEnabled by viewModel.englishEnabled.collectAsStateWithLifecycle()
    val textSharer = rememberTextSharer()
    val strings = LocalStrings.current
    val st = strings.settings
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshNotificationPermission()
        onPauseOrDispose { }
    }
    val current = settings ?: return

    if (pushBlocked) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPushBlocked,
            title = { Text(st.pushBlockedTitle) },
            text = { Text(st.pushBlockedBody) },
            confirmButton = { TextButton(onClick = viewModel::openNotificationSettings) { Text(strings.divination.openSettings) } },
            dismissButton = { TextButton(onClick = viewModel::dismissPushBlocked) { Text(strings.divination.notNow) } },
        )
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(4000)
            viewModel.messageShown()
        }
    }

    var page by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
    // System Back from a sub-page returns to the main settings page instead of leaving settings.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = page != SettingsPage.MAIN,
        onBackCompleted = { page = SettingsPage.MAIN },
    )
    val provider = account?.provider ?: SignInProvider.ANONYMOUS

    PaperBackground(showMountains = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (page == SettingsPage.MAIN) onBack() else page = SettingsPage.MAIN }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.common.back, tint = ZenColors.InkBlack)
                }
                Text(page.title(st), style = MaterialTheme.typography.headlineSmall)
            }
            message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenColors.PaperCream,
                    modifier = Modifier
                        .padding(horizontal = ZenSpacing.screen)
                        .padding(bottom = ZenSpacing.sm)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ZenColors.BambooDeep)
                        .padding(12.dp),
                )
            }
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val forward = targetState != SettingsPage.MAIN
                    (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { if (forward) it / 10 else -it / 10 }) togetherWith
                        fadeOut(tween(120))
                },
                label = "settingsPage",
            ) { shown ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = ZenSpacing.screen)
                        .padding(top = ZenSpacing.xs, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(ZenSpacing.md),
                ) {
                    when (shown) {
                        SettingsPage.MAIN -> {
                            Card {
                                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    CinnabarSeal(text = "易", size = 36.dp, tilt = 0f)
                                    Spacer(Modifier.width(ZenSpacing.md))
                                    Column(Modifier.weight(1f)) {
                                        Text(if (status.premium) st.planPremium else st.planFree, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            buildString {
                                                append(st.readingsLeftToday(status.readingsLeftToday))
                                                if (!status.premium || status.coins > 0) append(" · ${st.coinsNote(status.coins)}")
                                                if (status.premium) status.premiumUntilMillis?.let { append(" · ${st.until(it)}") }
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ZenColors.InkFaint,
                                        )
                                    }
                                    if (status.premium) {
                                        ZenTextButton(st.details, onClick = onOpenPaywall)
                                    } else {
                                        ZenPrimaryButton(st.upgrade, onClick = onOpenPaywall, showSeal = false, compact = true)
                                    }
                                }
                            }

                            Card {
                                SwitchRow(st.sound, checked = current.soundEnabled, onCheckedChange = viewModel::setSound)
                                if (viewModel.dailyPushSupported) {
                                    Divider()
                                    SwitchRow(
                                        st.dailyPush,
                                        subtitle = st.dailyPushSubtitle,
                                        checked = current.dailyPushEnabled && notificationsGranted,
                                        onCheckedChange = viewModel::setDailyPush,
                                    )
                                }
                                yearResult?.let { year ->
                                    Divider()
                                    LinkRow(st.yearRow(year), onClick = onOpenYear)
                                }
                                if (englishEnabled) {
                                    Divider()
                                    LinkRow(st.language, onClick = onOpenLanguage)
                                }
                            }

                            Card {
                                Row(
                                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(st.theme, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(
                                        st.streak(current.streak.currentOn(bangkokEpochDay(Clock.System.now().toEpochMilliseconds())), current.streak.best),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ZenColors.InkFaint,
                                    )
                                }
                                ThemePicker(
                                    selected = current.selectedTheme,
                                    unlocked = current.streak.unlocked,
                                    hintFor = { lockedThemeHint(it, status.campaign?.active == true, st) },
                                    lockedSuffix = st.lockedSuffix,
                                    onSelect = viewModel::selectTheme,
                                )
                            }

                            Card {
                                LinkRow(
                                    SettingsPage.ACCOUNT.title(st),
                                    subtitle = when (provider) {
                                        SignInProvider.APPLE -> st.linkedApple
                                        SignInProvider.GOOGLE -> st.linkedGoogle
                                        SignInProvider.ANONYMOUS -> st.notLinked
                                    },
                                ) { page = SettingsPage.ACCOUNT }
                                Divider()
                                LinkRow(SettingsPage.FEEDBACK.title(st)) { page = SettingsPage.FEEDBACK }
                                Divider()
                                LinkRow(SettingsPage.ABOUT.title(st)) { page = SettingsPage.ABOUT }
                            }

                            VersionLine(st.version(viewModel.appVersion))
                        }

                        SettingsPage.ACCOUNT -> {
                            SectionLabel(st.sectionAccount)
                            Card {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text(
                                        when (provider) {
                                            SignInProvider.APPLE -> st.linkedApple
                                            SignInProvider.GOOGLE -> st.linkedGoogle
                                            SignInProvider.ANONYMOUS -> st.localAccount
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        account?.email ?: st.accountId(account?.uid?.take(8) ?: "—"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ZenColors.InkFaint,
                                    )
                                    if (provider == SignInProvider.ANONYMOUS) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            st.linkHint,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ZenColors.InkSoft,
                                        )
                                    }
                                }
                                if (provider == SignInProvider.ANONYMOUS) {
                                    if (viewModel.supportsApple) {
                                        Divider()
                                        LinkRow(st.signInApple, enabled = !busy) { viewModel.link(SignInProvider.APPLE) }
                                    }
                                    if (viewModel.supportsGoogle) {
                                        Divider()
                                        LinkRow(st.signInGoogle, enabled = !busy) { viewModel.link(SignInProvider.GOOGLE) }
                                    }
                                }
                                Divider()
                                LinkRow(st.syncJournal, enabled = !busy) { viewModel.syncJournal() }
                            }

                            SectionLabel(st.sectionPurchases, modifier = Modifier.padding(top = ZenSpacing.sm))
                            Card {
                                LinkRow(st.restore, enabled = !busy) { viewModel.restore() }
                                Divider()
                                LinkRow(st.manageSubscriptions) { runCatching { uri.openUri(viewModel.manageSubscriptionsUrl) } }
                            }

                            Spacer(Modifier.height(ZenSpacing.lg))
                            Card {
                                LinkRow(st.deleteAccount, color = ZenColors.CinnabarRed, enabled = !busy) { confirmDelete = true }
                            }
                        }

                        SettingsPage.FEEDBACK -> {
                            Card {
                                val storeUrls = viewModel.storeUrls
                                if (storeUrls.isNotEmpty()) {
                                    LinkRow(strings.review.rateApp, subtitle = strings.review.rateAppSubtitle) {
                                        viewModel.onRateAppTapped()
                                        openFirst(storeUrls, uri::openUri)
                                    }
                                    Divider()
                                }
                                viewModel.shareAppText?.let { shareText ->
                                    LinkRow(st.shareApp, subtitle = st.shareAppSubtitle) {
                                        viewModel.onShareAppTapped()
                                        textSharer.share(shareText)
                                    }
                                    Divider()
                                }
                                LinkRow(st.sendFeedback, subtitle = st.sendFeedbackSubtitle, onClick = viewModel::openFeedback)
                            }
                        }

                        SettingsPage.ABOUT -> {
                            Text(
                                st.aboutBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = ZenColors.InkSoft,
                            )
                            Card {
                                LinkRow(st.howToCast, onClick = onOpenGuide)
                                Divider()
                                LinkRow(st.privacy) { runCatching { uri.openUri("${BuildConfig.WEB_BASE_URL}/privacy") } }
                                Divider()
                                LinkRow(st.terms) { runCatching { uri.openUri("${BuildConfig.WEB_BASE_URL}/terms") } }
                            }
                            VersionLine(st.version(viewModel.appVersion))
                        }
                    }
                }
            }
        }
    }

    if (feedback.open) {
        FeedbackSheet(
            ui = feedback,
            onTextChange = viewModel::onFeedbackText,
            onSend = viewModel::sendFeedback,
            onDismiss = viewModel::closeFeedback,
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = ZenColors.PaperCream,
            title = { Text(st.deleteTitle, style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    st.deleteBody,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteAccount(onDone = onBack)
                }) { Text(st.deleteConfirm, color = ZenColors.CinnabarRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(strings.common.cancel, color = ZenColors.InkSoft) } },
        )
    }
}

/** Why a theme is still locked: days in a row for streak themes, the year hexagram for the campaign one. */
internal fun lockedThemeHint(theme: AppTheme, campaignActive: Boolean, strings: SettingsStrings): String = when {
    theme.unlockDays >= 0 -> strings.lockedStreak(theme.unlockDays)
    campaignActive -> strings.lockedCampaign
    else -> strings.lockedSpecial
}

/**
 * One row of theme dots. The selected dot gets an ink ring; locked dots carry a small lock, and tapping
 * one says how to unlock it underneath instead of switching.
 */
@Composable
private fun ThemePicker(
    selected: AppTheme,
    unlocked: Set<AppTheme>,
    hintFor: (AppTheme) -> String,
    lockedSuffix: String,
    onSelect: (AppTheme) -> Unit,
) {
    val common = LocalStrings.current.common
    var hinted by remember { mutableStateOf<AppTheme?>(null) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AppTheme.entries.forEach { theme ->
                val isUnlocked = theme in unlocked
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClickLabel = common.theme(theme)) {
                            if (isUnlocked) {
                                hinted = null
                                onSelect(theme)
                            } else {
                                hinted = theme
                            }
                        }
                        .semantics {
                            contentDescription = common.theme(theme) + if (!isUnlocked) lockedSuffix else ""
                            this.selected = theme == selected
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .then(if (theme == selected) Modifier.border(2.dp, ZenColors.InkBlack, CircleShape) else Modifier)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(paletteFor(theme).paperCream)
                            // The theme's own ink as a rim, so similar papers (ink paper, bamboo) stay tellable apart.
                            .border(4.dp, paletteFor(theme).inkBlack, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!isUnlocked) {
                            Icon(Icons.Outlined.Lock, null, tint = paletteFor(theme).inkFaint, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        val h = hinted
        Text(
            if (h != null) "${common.theme(h)} · ${hintFor(h)}" else common.theme(selected),
            style = MaterialTheme.typography.labelSmall,
            color = if (h != null) ZenColors.InkSoft else ZenColors.InkFaint,
        )
    }
}

/** The main settings page and the rarely-used groups that open from it, so the main page fits one screen. */
private enum class SettingsPage {
    MAIN, ACCOUNT, FEEDBACK, ABOUT;

    fun title(strings: SettingsStrings): String = when (this) {
        MAIN -> strings.pageMain
        ACCOUNT -> strings.pageAccount
        FEEDBACK -> strings.pageFeedback
        ABOUT -> strings.pageAbout
    }
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, subtitle: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = ZenColors.InkBlack, checkedThumbColor = ZenColors.PaperCream),
        )
    }
}

@Composable
private fun VersionLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = ZenColors.InkFaint,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    ZenCard(contentPadding = PaddingValues(0.dp), content = content)
}

@Composable
private fun Divider() {
    ZenCardDivider()
}

@Composable
private fun LinkRow(
    text: String,
    color: Color = ZenColors.InkBlack,
    enabled: Boolean = true,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (subtitle == null) 14.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = if (enabled) color else color.copy(alpha = 0.4f))
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = ZenColors.InkFaint)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = ZenColors.InkFaint)
    }
}
