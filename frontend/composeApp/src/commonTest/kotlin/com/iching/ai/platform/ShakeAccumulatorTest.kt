package com.iching.ai.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShakeAccumulatorTest {

    /** Feeds a sample every [step] ms from [from] to [to] inclusive and returns the times it fired. */
    private fun ShakeAccumulator.feed(g: Float, from: Long, to: Long, step: Long = 100): List<Long> =
        (from..to step step).filter { onSample(g, it) }

    @Test
    fun samplesBelowThresholdNeverAccumulate() {
        val acc = ShakeAccumulator()
        assertEquals(emptyList(), acc.feed(1.2f, 0, 5_000))
    }

    @Test
    fun firesOnceAfterOneAndAHalfSecondsOfShaking() {
        val acc = ShakeAccumulator()
        assertEquals(listOf(1_500L), acc.feed(2.0f, 0, 4_000))
    }

    @Test
    fun aPauseLongerThan600msStartsOver() {
        val acc = ShakeAccumulator()
        assertEquals(emptyList(), acc.feed(2.0f, 0, 1_000))
        assertFalse(acc.onSample(2.0f, 1_700))
        assertEquals(listOf(3_200L), acc.feed(2.0f, 1_800, 3_400))
    }

    @Test
    fun aShortPauseKeepsTheCount() {
        val acc = ShakeAccumulator()
        acc.feed(2.0f, 0, 1_000)
        assertTrue(acc.onSample(2.0f, 1_500))
    }

    @Test
    fun resetAllowsAnotherShake() {
        val acc = ShakeAccumulator()
        acc.feed(2.0f, 0, 1_500)
        acc.reset()
        assertEquals(listOf(11_500L), acc.feed(2.0f, 10_000, 12_000))
    }
}
