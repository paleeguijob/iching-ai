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

    @Test
    fun campaignPushOpensTheYearScreen() {
        PendingNavigation.requestYear()
        assertTrue(PendingNavigation.year.value)
        assertFalse(PendingNavigation.daily.value)
        PendingNavigation.consumeYear()
        assertFalse(PendingNavigation.year.value)
    }
}
