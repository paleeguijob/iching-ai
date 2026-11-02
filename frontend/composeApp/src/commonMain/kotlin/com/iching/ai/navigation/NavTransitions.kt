package com.iching.ai.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavBackStackEntry

typealias NavScope = AnimatedContentTransitionScope<NavBackStackEntry>

/** Screen transitions for the NavHost, which differ per platform. */
class NavTransitions(
    val enter: NavScope.() -> EnterTransition,
    val exit: NavScope.() -> ExitTransition,
    val popEnter: NavScope.() -> EnterTransition,
    val popExit: NavScope.() -> ExitTransition,
    val predictivePopEnter: NavScope.(swipeEdge: Int) -> EnterTransition,
    val predictivePopExit: NavScope.(swipeEdge: Int) -> ExitTransition,
)

expect val platformNavTransitions: NavTransitions

/**
 * Keeps a screen in a texture while it slides. Two full screens of gradients and text otherwise
 * take a mid-range GPU more than twice the frame budget to redraw on every frame of the transition.
 */
@OptIn(ExperimentalAnimationApi::class)
fun Modifier.cachedWhileTransitioning(scope: AnimatedContentScope): Modifier = graphicsLayer {
    compositingStrategy = if (scope.transition.isRunning) CompositingStrategy.Offscreen else CompositingStrategy.Auto
}
