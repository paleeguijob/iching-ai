package com.iching.ai.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A screen native code asked to open (tapping the daily push). It stays pending until the navigation
 * graph exists, which is after a cold start and after the disclaimer has been accepted.
 */
object PendingNavigation {
    private val _daily = MutableStateFlow(false)
    val daily: StateFlow<Boolean> = _daily.asStateFlow()

    fun requestDaily() {
        _daily.value = true
    }

    fun consumeDaily() {
        _daily.value = false
    }
}
