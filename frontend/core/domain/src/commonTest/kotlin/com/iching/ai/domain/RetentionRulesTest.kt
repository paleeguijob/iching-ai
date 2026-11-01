package com.iching.ai.domain

import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.StreakState
import com.iching.ai.domain.model.currentOn
import com.iching.ai.domain.model.recordView
import com.iching.ai.domain.oracle.bangkokEpochDay
import com.iching.ai.domain.oracle.dailyHexagramId
import com.iching.ai.domain.oracle.splitForSpeech
import com.iching.ai.domain.oracle.stripMarkup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RetentionRulesTest {

    @Test
    fun dailyIdMatchesSharedVectors() {
        mapOf(0L to 12, 20000L to 44, 20759L to 31, 20760L to 4, -1L to 39)
            .forEach { (day, id) -> assertEquals(id, dailyHexagramId(day)) }
        assertEquals((1..64).toSet(), (0L until 64L).map(::dailyHexagramId).toSet())
    }

    @Test
    fun epochDayRollsAtBangkokMidnight() {
        val h17 = 17 * 3_600_000L
        assertEquals(0L, bangkokEpochDay(h17 - 1))
        assertEquals(1L, bangkokEpochDay(h17))
        assertEquals(-1L, bangkokEpochDay(-7 * 3_600_000L - 1))
    }

    @Test
    fun firstViewStartsTheStreak() {
        val first = StreakState().recordView(10)
        assertEquals(1, first.state.current)
        assertEquals(1, first.state.best)
        assertTrue(first.counted)
    }

    @Test
    fun sameDayDoesNotCountAgain() {
        val first = StreakState().recordView(10).state
        val same = first.recordView(10)
        assertEquals(first, same.state)
        assertFalse(same.counted)
    }

    @Test
    fun nextDayExtendsAndGapRestartsButKeepsBest() {
        val second = StreakState().recordView(10).state.recordView(11).state
        assertEquals(2, second.current)
        val gap = second.recordView(13)
        assertEquals(1, gap.state.current)
        assertEquals(2, gap.state.best)
        assertTrue(gap.counted)
    }

    @Test
    fun aMissedDayShowsAsZeroBeforeTheNextView() {
        val state = StreakState(current = 5, best = 5, lastEpochDay = 10)
        assertEquals(5, state.currentOn(10))
        assertEquals(5, state.currentOn(11))
        assertEquals(0, state.currentOn(12))
    }

    @Test
    fun clockMovedBackChangesNothing() {
        val state = StreakState(current = 4, best = 4, lastEpochDay = 11)
        val back = state.recordView(5)
        assertEquals(state, back.state)
        assertFalse(back.counted)
    }

    @Test
    fun unlocksArePermanentAndReportedOnce() {
        val seven = StreakState(current = 6, best = 6, lastEpochDay = 6).recordView(7)
        assertEquals(listOf(AppTheme.BAMBOO), seven.newlyUnlocked)
        val broken = seven.state.recordView(20)
        assertTrue(AppTheme.BAMBOO in broken.state.unlocked)
        assertEquals(emptyList(), broken.newlyUnlocked)
    }

    @Test
    fun themeThresholdsMatchTheSpec() {
        assertEquals(listOf(0, 7, 30, 100), AppTheme.entries.map { it.unlockDays })
        assertEquals(AppTheme.INK_NIGHT, AppTheme.fromWire("ink_night"))
        assertNull(AppTheme.fromWire("nope"))
    }

    @Test
    fun stripMarkupRemovesSymbolsAndCollapsesSpaces() {
        assertEquals("ฟ้า ดิน", stripMarkup("**ฟ้า**   # ดิน"))
    }

    @Test
    fun speechSplitsAtSpacesAndHardCutsLongRuns() {
        assertEquals(listOf("หนึ่ง สอง", "สาม"), splitForSpeech("หนึ่ง สอง สาม", 9))
        val parts = splitForSpeech("ก".repeat(9000), 4000)
        assertTrue(parts.all { it.length <= 4000 })
        assertEquals(9000, parts.sumOf { it.length })
        assertEquals(emptyList(), splitForSpeech("   ", 4000))
    }
}
