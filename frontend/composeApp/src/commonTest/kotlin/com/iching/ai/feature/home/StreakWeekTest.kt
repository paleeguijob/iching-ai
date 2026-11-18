package com.iching.ai.feature.home

import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.StreakState
import kotlin.test.Test
import kotlin.test.assertEquals

class StreakWeekTest {
    private val today = 20402L

    private fun week(current: Int, lastBack: Long?, best: Int = current) =
        streakWeek(StreakState(current = current, best = best, lastEpochDay = lastBack?.let { today - it }), today)

    @Test
    fun noStreakShowsDayOneAsTodayAndInvitesToStart() {
        val w = week(0, null)
        assertEquals(listOf("๑", "๒", "๓", "๔", "๕", "๖", "๗"), w.days.map { it.label })
        assertEquals(List(7) { false }, w.days.map { it.done })
        assertEquals(listOf(true, false, false, false, false, false, false), w.days.map { it.isToday })
        assertEquals("เปิดอ่านกวาวันนี้ เพื่อเริ่มนับวันแรก", w.caption)
    }

    @Test
    fun countedTodayTicksUpToTodaysNumber() {
        val w = week(3, lastBack = 0)
        assertEquals(listOf(true, true, true, false, false, false, false), w.days.map { it.done })
        assertEquals(2, w.days.indexOfFirst { it.isToday })
        assertEquals("ต่อเนื่อง ๓ วัน · อีก ๔ วันปลดล็อกธีม${AppTheme.BAMBOO.labelTh}", w.caption)
    }

    @Test
    fun notYetTodayRingsTheNextNumber() {
        val w = week(2, lastBack = 1)
        assertEquals(listOf(true, true, false, false, false, false, false), w.days.map { it.done })
        assertEquals(2, w.days.indexOfFirst { it.isToday })
        assertEquals("ต่อเนื่อง ๒ วัน · เปิดอ่านกวาวันนี้เพื่อไม่ให้ขาด", w.caption)
    }

    @Test
    fun brokenRunStartsAgainAtOne() {
        val w = week(4, lastBack = 2)
        assertEquals(List(7) { false }, w.days.map { it.done })
        assertEquals(0, w.days.indexOfFirst { it.isToday })
    }

    @Test
    fun seventhDayIsTheBambooMilestone() =
        assertEquals(listOf(false, false, false, false, false, false, true), week(0, null).days.map { it.milestone })

    @Test
    fun afterSevenTheNextBlockOfSevenShows() {
        val w = week(7, lastBack = 1)
        assertEquals(listOf("๘", "๙", "๑๐", "๑๑", "๑๒", "๑๓", "๑๔"), w.days.map { it.label })
        assertEquals(List(7) { false }, w.days.map { it.done })
        assertEquals(0, w.days.indexOfFirst { it.isToday })
    }

    @Test
    fun blockHoldingThirtyMarksIt() {
        val w = week(29, lastBack = 0)
        assertEquals(listOf("๒๙", "๓๐", "๓๑", "๓๒", "๓๓", "๓๔", "๓๕"), w.days.map { it.label })
        assertEquals(listOf(false, true, false, false, false, false, false), w.days.map { it.milestone })
        assertEquals(listOf(true, false, false, false, false, false, false), w.days.map { it.done })
    }

    @Test
    fun longRunShowsBestOnceAllThemesAreUnlocked() =
        assertEquals("ต่อเนื่อง ๑๒๐ วัน · ดีที่สุด ๑๕๐ วัน", week(120, lastBack = 0, best = 150).caption)
}
