package com.iching.ai.feature.share

import com.iching.ai.domain.siamsi.SiamSiLevel
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiStick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SiamSiShareContentTest {
    private val stick = SiamSiStick(
        17, "中吉", SiamSiLevel.FAIR, null, listOf("一", "二", "三", "四"), listOf("ก", "ข", "ค", "ง"),
        "story", "summary", emptyMap(), emptyList(),
    )
    private val set = SiamSiSet("guanyin100", "เซียมซีเจ้าแม่กวนอิม", 100, "觀音靈籤", "", listOf(stick))

    @Test
    fun theCardCarriesTheSlipAndItsSource() {
        val card = siamsiShareContent(set, stick)
        assertEquals("เซียมซีเจ้าแม่กวนอิม", card.setName)
        assertEquals("๑๗", card.number)
        assertEquals("ปานกลาง · 中吉", card.level)
        assertEquals(listOf("一", "二", "三", "四"), card.poemOrig)
        assertEquals(listOf("ก", "ข", "ค", "ง"), card.poemTh)
        assertEquals("ตำรา 觀音靈籤", card.source)
        assertEquals("guanyin100", card.setId)
    }

    @Test
    fun theCardHasNoRoomForTheQuestion() {
        // Every field the card shows comes from the set; the seeker's question is not an input at all.
        val shown = siamsiShareContent(set, stick).toString()
        assertFalse("story" in shown || "summary" in shown)
    }

    @Test
    fun aThaiSetShowsItsPoemOnce() {
        val thai = stick.copy(gradeOrig = null, poemOrig = stick.poemTh)
        val card = siamsiShareContent(set, thai)
        assertEquals(emptyList(), card.poemOrig)
        assertEquals("ปานกลาง", card.level)
    }
}
