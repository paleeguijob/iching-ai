package com.iching.ai.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
