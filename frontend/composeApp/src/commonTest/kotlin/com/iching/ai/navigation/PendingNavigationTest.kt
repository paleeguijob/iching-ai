package com.iching.ai.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PendingNavigationTest {

    @Test
    fun requestWaitsUntilSomeoneConsumesIt() {
        PendingNavigation.requestDaily()
        // Still pending while the disclaimer is showing and nothing is listening yet.
        assertTrue(PendingNavigation.daily.value)
        PendingNavigation.consumeDaily()
        assertFalse(PendingNavigation.daily.value)
    }
}
