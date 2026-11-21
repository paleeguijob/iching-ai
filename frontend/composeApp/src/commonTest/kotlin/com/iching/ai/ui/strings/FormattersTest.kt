package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage.EN
import com.iching.ai.domain.model.AppLanguage.TH
import com.iching.ai.domain.model.Hexagram
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

    @Test
    fun month() {
        assertEquals("พฤศจิกายน 2569", formatMonth(millis(), TH, bangkok))
        assertEquals("November 2026", formatMonth(millis(), EN, bangkok))
    }

    private fun hex(nameTh: String, nameEn: String = "") = Hexagram(
        id = 1, nameCn = "乾", pinyin = "Qián", nameTh = nameTh, pattern = "111111",
        keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" }, nameEn = nameEn,
    )

    @Test
    fun hexagramNames() {
        val th = hex("เฉียน — ความเข้มแข็ง")
        assertEquals("乾 เฉียน", th.displayName(TH))
        assertEquals("เฉียน", th.shortName(TH))
        assertEquals("ความเข้มแข็ง", th.meaning(TH))
        val en = hex("Heaven — Strong and unceasing", "Heaven")
        assertEquals("乾 Qián · Heaven", en.displayName(EN))
        assertEquals("Heaven", en.shortName(EN))
        assertEquals("Strong and unceasing", en.meaning(EN))
        // Overlay missing (blank nameEn, ruling R25): the Thai data is shown.
        val fallback = hex("เฉียน — ความเข้มแข็ง")
        assertEquals("乾 เฉียน", fallback.displayName(EN))
        assertEquals("เฉียน", fallback.shortName(EN))
        assertEquals("ความเข้มแข็ง", fallback.meaning(EN))
    }

    @Test
    fun dailyBannerNamesTheHexagramOnce() {
        val wind = hex("Wind — Wind, gentle penetration", "Wind").copy(id = 57)
        assertEquals("57 · Wind — gentle penetration", wind.dailyBannerLine(EN))
        val waiting = hex("Waiting — Waiting", "Waiting").copy(id = 5)
        assertEquals("5 · Waiting", waiting.dailyBannerLine(EN))
        val heaven = hex("Heaven — Strong and unceasing", "Heaven").copy(id = 1)
        assertEquals("1 · Heaven — Strong and unceasing", heaven.dailyBannerLine(EN))
        val kun = hex("ขุน — ความตีบตัน, ถูกล้อม").copy(id = 47)
        assertEquals("๔๗ · ขุน ความตีบตัน, ถูกล้อม", kun.dailyBannerLine(TH))
        assertEquals("47 · ขุน ความตีบตัน, ถูกล้อม", kun.dailyBannerLine(EN))
    }
}
