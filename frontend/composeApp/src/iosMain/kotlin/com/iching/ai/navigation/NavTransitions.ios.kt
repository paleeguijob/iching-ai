package com.iching.ai.navigation

import androidx.navigation.compose.DefaultNavTransitions

// The iOS defaults already slide and follow the edge-swipe back gesture.
actual val platformNavTransitions = NavTransitions(
    enter = DefaultNavTransitions.enterTransition,
    exit = DefaultNavTransitions.exitTransition,
    popEnter = DefaultNavTransitions.popEnterTransition(DefaultNavTransitions.enterTransition),
    popExit = DefaultNavTransitions.popExitTransition(DefaultNavTransitions.exitTransition),
    predictivePopEnter = DefaultNavTransitions.predictivePopEnterTransition,
    predictivePopExit = DefaultNavTransitions.predictivePopExitTransition,
)
