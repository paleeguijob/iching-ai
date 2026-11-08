package com.iching.ai.domain.siamsi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SiamSiQuotaTest {
    private val day = 20_400L

    @Test
    fun drawsLeftCountsTodayOnly() {
        assertEquals(1, drawsLeft(SiamSiUsage(), day, perDay = 1))
        assertEquals(10, drawsLeft(SiamSiUsage(), day, perDay = 10))
        val used = SiamSiUsage().grant(day)
        assertEquals(0, drawsLeft(used, day, 1))
        assertEquals(9, drawsLeft(used, day, 10))
        assertEquals(1, drawsLeft(used, day + 1, 1)) // resets next day
    }

    @Test
    fun grantCountsFromAFreshDay() {
        val yesterday = SiamSiUsage(day - 1, used = 5, pendingDraw = true)
        assertEquals(SiamSiUsage(day, used = 1, pendingDraw = true), yesterday.grant(day))
    }

    @Test
    fun pendingDrawSurvivesUntilConsumedSameDay() {
        val u = SiamSiUsage().grant(day)
        assertTrue(u.on(day).pendingDraw)
        assertFalse(u.consumePending().pendingDraw)
        assertEquals(1, u.consumePending().used)
        assertFalse(u.on(day + 1).pendingDraw) // expires with the day
    }
}
