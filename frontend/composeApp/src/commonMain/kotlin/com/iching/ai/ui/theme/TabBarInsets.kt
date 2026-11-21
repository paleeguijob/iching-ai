package com.iching.ai.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max

/**
 * The tab bar's measured height, navigation-bar inset included. It grows with the system font
 * scale, so tab screens pad by this instead of guessing a constant.
 */
val LocalTabBarHeight = compositionLocalOf { ZenSpacing.tabBar }

/** Bottom padding that keeps Home's pinned button a gap above whichever is taller: the keyboard or the tab bar. */
fun ctaBottomPadding(imeBottom: Dp, tabBarHeight: Dp): Dp = max(imeBottom, tabBarHeight) + ZenSpacing.md

/** Bottom content padding for a scrolling tab screen, so its last row ends above the tab bar. */
fun tabContentBottomPadding(tabBarHeight: Dp): Dp = tabBarHeight + ZenSpacing.lg
