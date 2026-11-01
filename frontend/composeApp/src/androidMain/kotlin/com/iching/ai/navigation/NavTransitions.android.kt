package com.iching.ai.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

private val slideSpec = tween<IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)

// Navigation's Android default is a 700 ms crossfade, which leaves the old screen on show for most of it.
actual val platformNavTransitions = NavTransitions(
    enter = { slideIntoContainer(SlideDirection.Start, slideSpec) },
    exit = { slideOutOfContainer(SlideDirection.Start, slideSpec, targetOffset = { it / 4 }) },
    popEnter = { slideIntoContainer(SlideDirection.End, slideSpec, initialOffset = { it / 4 }) },
    popExit = { slideOutOfContainer(SlideDirection.End, slideSpec) },
    predictivePopEnter = { slideIntoContainer(SlideDirection.End, slideSpec, initialOffset = { it / 4 }) },
    predictivePopExit = { slideOutOfContainer(SlideDirection.End, slideSpec) },
)
