package com.iching.ai.ui

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class ThaiDatesTest {
    private fun millis(year: Int, month: Int, day: Int) =
        LocalDateTime(year, month, day, 20, 27).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    @Test
    fun shortDateUsesThaiDigitsAndAbbreviation() {
        assertEquals("๖ พ.ย.", formatThaiShortDate(millis(2026, 11, 6)))
        assertEquals("๑๕ ม.ค.", formatThaiShortDate(millis(2026, 1, 15)))
    }
}
