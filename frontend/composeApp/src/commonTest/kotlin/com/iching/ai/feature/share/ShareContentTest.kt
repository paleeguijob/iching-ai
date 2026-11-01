package com.iching.ai.feature.share

import com.iching.ai.domain.model.Hexagram
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
    fun aiCardIsLabelledAndBounded() {
        val card = readingShareContent(hexagram, "**คำทำนาย** " + "ก".repeat(1000))
        assertEquals(hexagram.judgment, card.judgment)
        assertTrue(card.body.length <= 200, "body is ${card.body.length} chars")
        assertFalse("*" in card.body)
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
}
