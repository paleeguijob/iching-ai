package com.iching.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.iching.ai.feature.divination.DivinationScreen
import com.iching.ai.feature.home.HomeScreen
import com.iching.ai.feature.journal.JournalEntryScreen
import com.iching.ai.feature.journal.JournalScreen
import com.iching.ai.feature.library.HexagramDetailScreen
import com.iching.ai.feature.library.LibraryScreen
import com.iching.ai.feature.settings.SettingsScreen
import com.iching.ai.navigation.DivinationRoute
import com.iching.ai.navigation.HexagramRoute
import com.iching.ai.navigation.HomeRoute
import com.iching.ai.navigation.JournalEntryRoute
import com.iching.ai.navigation.JournalRoute
import com.iching.ai.navigation.LibraryRoute
import com.iching.ai.navigation.PaywallRoute
import com.iching.ai.navigation.SettingsRoute
import com.iching.ai.feature.onboarding.OnboardingScreen
import com.iching.ai.feature.paywall.PaywallScreen
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import com.iching.ai.session.AppSession
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.reflect.KClass

private data class TopLevelTab(val route: Any, val routeClass: KClass<*>, val label: String, val icon: ImageVector)

private val TABS = listOf(
    TopLevelTab(HomeRoute, HomeRoute::class, "ถามเต๋า", Icons.Outlined.Spa),
    TopLevelTab(LibraryRoute, LibraryRoute::class, "คัมภีร์", Icons.AutoMirrored.Outlined.MenuBook),
    TopLevelTab(JournalRoute, JournalRoute::class, "บันทึกชะตา", Icons.Outlined.HistoryEdu),
)

@Composable
fun App() {
    ZenTheme {
        val observeSettings = koinInject<ObserveSettingsUseCase>()
        val updateSettings = koinInject<UpdateSettingsUseCase>()
        val session = koinInject<AppSession>()
        val settings by remember { observeSettings() }.collectAsState(initial = null)
        val scope = rememberCoroutineScope()
        val current = settings
        when {
            current == null -> Box(Modifier.fillMaxSize().background(ZenColors.PaperCream))
            !current.onboardingAccepted -> OnboardingScreen(onAccept = {
                scope.launch { updateSettings { it.copy(onboardingAccepted = true) } }
            })
            else -> {
                LaunchedEffect(Unit) { session.start() }
                MainNavigation()
            }
        }
    }
}

/**
 * Pops [entry] only while it is the settled top screen. A second tap on a back button lands during
 * the exit transition, and popping again would remove the screen below, even Home, leaving it blank.
 */
private fun NavController.popFrom(entry: NavBackStackEntry) {
    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) popBackStack()
}

@Composable
private fun MainNavigation() {
    run {
        val nav = rememberNavController()
        val backStack by nav.currentBackStackEntryAsState()
        val destination = backStack?.destination
        val currentTab = TABS.firstOrNull { tab -> destination?.hasRoute(tab.routeClass) == true }

        fun openTab(tab: TopLevelTab) {
            nav.navigate(tab.route) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        Box(Modifier.fillMaxSize().background(ZenColors.PaperCream)) {
            NavHost(nav, startDestination = HomeRoute, modifier = Modifier.fillMaxSize()) {
                composable<HomeRoute> {
                    HomeScreen(
                        viewModel = koinViewModel(),
                        onCast = { question, category -> nav.navigate(DivinationRoute(question, category?.wire)) },
                        onOpenHexagram = { nav.navigate(HexagramRoute(it)) },
                        onOpenSettings = { nav.navigate(SettingsRoute) },
                    )
                }
                composable<DivinationRoute> { entry ->
                    val route = entry.toRoute<DivinationRoute>()
                    DivinationScreen(
                        viewModel = koinViewModel { parametersOf(route.question, QuestionCategory.fromWire(route.category)) },
                        onBack = { nav.popFrom(entry) },
                        onOpenHexagram = { nav.navigate(HexagramRoute(it)) },
                        onOpenPaywall = { nav.navigate(PaywallRoute) },
                        onDone = { nav.popFrom(entry) },
                    )
                }
                composable<LibraryRoute> {
                    LibraryScreen(viewModel = koinViewModel(), onOpenHexagram = { nav.navigate(HexagramRoute(it)) })
                }
                composable<HexagramRoute> { entry ->
                    val route = entry.toRoute<HexagramRoute>()
                    HexagramDetailScreen(
                        viewModel = koinViewModel(key = "hexagram-${route.id}") { parametersOf(route.id) },
                        onBack = { nav.popFrom(entry) },
                    )
                }
                composable<JournalRoute> {
                    JournalScreen(
                        viewModel = koinViewModel(),
                        onOpenEntry = { nav.navigate(JournalEntryRoute(it)) },
                        onStartCasting = { openTab(TABS.first()) },
                        onOpenPaywall = { nav.navigate(PaywallRoute) },
                    )
                }
                composable<JournalEntryRoute> { entry ->
                    val route = entry.toRoute<JournalEntryRoute>()
                    JournalEntryScreen(
                        viewModel = koinViewModel { parametersOf(route.id) },
                        onBack = { nav.popFrom(entry) },
                        onOpenHexagram = { nav.navigate(HexagramRoute(it)) },
                    )
                }
                composable<SettingsRoute> { entry ->
                    SettingsScreen(
                        viewModel = koinViewModel(),
                        onBack = { nav.popFrom(entry) },
                        onOpenPaywall = { nav.navigate(PaywallRoute) },
                    )
                }
                composable<PaywallRoute> { entry ->
                    PaywallScreen(viewModel = koinViewModel(), onClose = { nav.popFrom(entry) })
                }
            }

            AnimatedVisibility(
                visible = currentTab != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                ZenTabBar(selected = currentTab, onSelect = ::openTab)
            }
        }
    }
}

/** Floating pill tab bar, like a strip of rice paper. */
@Composable
private fun ZenTabBar(selected: TopLevelTab?, onSelect: (TopLevelTab) -> Unit) {
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
            .shadow(16.dp, RoundedCornerShape(32.dp), ambientColor = ZenColors.InkBlack, spotColor = ZenColors.InkBlack)
            .clip(RoundedCornerShape(32.dp))
            .background(ZenColors.PaperCream)
            .border(1.dp, ZenColors.InkBlack.copy(alpha = 0.06f), RoundedCornerShape(32.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TABS.forEach { tab ->
            val isSelected = tab == selected
            Column(
                Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (isSelected) ZenColors.InkBlack else ZenColors.PaperCream)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tab.label,
                    tint = if (isSelected) ZenColors.PaperCream else ZenColors.InkSoft,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) ZenColors.PaperCream else ZenColors.InkSoft,
                )
            }
        }
    }
}
