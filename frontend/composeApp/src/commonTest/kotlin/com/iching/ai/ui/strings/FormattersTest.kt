package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage.EN
import com.iching.ai.domain.model.AppLanguage.TH
import com.iching.ai.feature.year.campaignYearLabel
import com.iching.ai.ui.formatThaiShortDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class FormattersTest {
    private val bangkok = TimeZone.of("Asia/Bangkok")
    private fun millis(h: Int = 14, min: Int = 5) =
        LocalDateTime(2026, 11, 20, h, min).toInstant(bangkok).toEpochMilliseconds()

    @Test
    fun numbers() {
        assertEquals("๒๕๗๐", formatNumber(2570, TH))
        assertEquals("2570", formatNumber(2570, EN))
    }

    @Test
    fun shortDate() {
        assertEquals("20 Nov 2026", formatShortDate(millis(), EN, bangkok))
        assertEquals(formatThaiShortDate(millis(), bangkok), formatShortDate(millis(), TH, bangkok))
    }

    @Test
    fun zodiacYear() {
        assertEquals("มะแม ๒๕๗๐", zodiacYearLabel(2027, TH))
        for (y in listOf(2024, 2026, 2027, 2031)) assertEquals(campaignYearLabel(y), zodiacYearLabel(y, TH))
        assertEquals("Year of the Goat 2027", zodiacYearLabel(2027, EN))
    }

    @Test
    fun time() {
        assertEquals("14:05", formatTime(millis(), EN, bangkok))
        assertEquals("14:05 น.", formatTime(millis(), TH, bangkok))
    }
}
