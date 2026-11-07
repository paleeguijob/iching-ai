package com.iching.ai.feature.share

import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.YearMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShareContentTest {
    private val hexagram = Hexagram(
        id = 1, nameCn = "乾", pinyin = "Qián", nameTh = "เฉียน — ฟ้า", pattern = "111111",
        keywords = emptyList(), judgment = "ยิ่งใหญ่ตั้งแต่ต้นกำเนิด", image = "", description = "",
        lines = List(6) { "" }, dailyMessage = "ฟ้าไม่เคยหยุดเคลื่อน",
        yearMessage = YearMessage("ภาพรวม", "งาน", "รัก", "เงิน", "ขอให้ปีนี้ฟ้าเปิดทาง"),
    )

    @Test
    fun dailyCardUsesTheDailyMessage() {
        val card = dailyShareContent(hexagram)
        assertEquals(hexagram.dailyMessage, card.body)
        assertNull(card.judgment)
        assertFalse(card.aiLabel)
        assertEquals("daily", card.source)
    }

    @Test
    fun aiCardShowsTheWholeSummaryWithItsLabel() {
        val card = readingShareContent(hexagram, "**ใจนิ่ง** ย่อมเห็นทาง")
        assertEquals(hexagram.judgment, card.judgment)
        assertEquals("ใจนิ่ง ย่อมเห็นทาง", card.body)
        assertFalse("…" in card.body)
        assertTrue(card.aiLabel)
        assertEquals("reading", card.source)
    }

    @Test
    fun scriptureOnlyCardHasNoAiLabel() {
        listOf(null, "", "   ").forEach { text ->
            val card = readingShareContent(hexagram, text)
            assertEquals(hexagram.judgment, card.body)
            assertNull(card.judgment)
            assertFalse(card.aiLabel)
            assertEquals("reading", card.source)
        }
    }

    @Test
    fun shareTextLinksToTheStore() {
        assertTrue("https://play.google.com/store/apps/details?id=com.iching.ai" in SHARE_TEXT)
    }

    @Test
    fun yearCardShowsTheBlessingWithoutAnAiLabelOrAQuestion() {
        val card = yearShareContent(hexagram, "๒๕๗๐")
        assertEquals("ขอให้ปีนี้ฟ้าเปิดทาง", card.body)
        assertNull(card.judgment)
        assertFalse(card.aiLabel)
        assertEquals(CardStyle.NEW_YEAR, card.style)
        assertEquals("กวาประจำปีมะแม ๒๕๗๐", card.yearLabel)
    }

    @Test
    fun yearCardSourceIsYear() {
        assertEquals("year", yearShareContent(hexagram, "๒๕๗๐").source)
    }

    @Test
    fun readingCardInTheCampaignKeepsItsAiLabel() {
        val card = readingShareContent(hexagram, "ใจนิ่ง ย่อมเห็นทาง", newYear = true)
        assertEquals(CardStyle.NEW_YEAR, card.style)
        assertTrue(card.aiLabel)
        assertNull(card.yearLabel)
        assertEquals(CardStyle.CLASSIC, readingShareContent(hexagram, "ใจนิ่ง").style)
        assertEquals(CardStyle.CLASSIC, dailyShareContent(hexagram).style)
    }
}
