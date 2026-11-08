package com.iching.ai.feature.journal

import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.siamsi.SiamSiLevel
import com.iching.ai.domain.siamsi.SiamSiPick
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiStick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val HEX = Hexagram(
    id = 1, nameCn = "乾", pinyin = "", nameTh = "เฉียน", pattern = "111111",
    keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
)

private fun iching(id: Long) = Reading(
    id = id, question = "q", createdAtMillis = id, lines = List(6) { LineType.YOUNG_YANG }, primaryId = 1, relatingId = null,
    interpretation = "", source = InterpretationSource.SCRIPTURE,
)

private fun siamsi(id: Long, set: String = "guanyin100", number: Int = 17, words: String = "") = Reading(
    id = id, question = "q", createdAtMillis = id, lines = emptyList(), primaryId = 0, relatingId = null,
    interpretation = words, source = if (words.isEmpty()) InterpretationSource.SCRIPTURE else InterpretationSource.AI,
    siamsi = SiamSiPick(set, number),
)

private fun stick(n: Int, level: SiamSiLevel, label: String? = null) =
    SiamSiStick(n, null, level, label, emptyList(), emptyList(), "", "", emptyMap(), emptyList())

private val GUANYIN = SiamSiSet("guanyin100", "เซียมซีเจ้าแม่กวนอิม", 100, "觀音靈籤", "", listOf(stick(17, SiamSiLevel.GOOD)))

class JournalFilterTest {
    private val all = listOf(iching(1), siamsi(2), iching(3), siamsi(4))

    @Test
    fun theThreeFilters() {
        assertEquals(listOf(1L, 2, 3, 4), filterJournal(all, JournalFilter.ALL).map { it.id })
        assertEquals(listOf(1L, 3), filterJournal(all, JournalFilter.ICHING).map { it.id })
        assertEquals(listOf(2L, 4), filterJournal(all, JournalFilter.SIAMSI).map { it.id })
    }

    @Test
    fun rowLabels() {
        assertEquals("กวนอิม ใบที่ ๑๗ · ดี · มีคำแปล", siamsiRowLabel("เซียมซีเจ้าแม่กวนอิม", 17, SiamSiLevel.GOOD, null, hasInterpretation = true))
        assertEquals("หกสิบกะจื๊อ ใบที่ ๓ 丙寅 · ดีมาก", siamsiRowLabel("เซียมซีหกสิบกะจื๊อ", 3, SiamSiLevel.EXCELLENT, "丙寅", hasInterpretation = false))
    }

    @Test
    fun aSiamSiReadingBecomesAnEntry() {
        val entry = assertNotNull(journalEntry(siamsi(2, words = "คำแปล"), mapOf(1 to HEX), mapOf("guanyin100" to GUANYIN)))
        assertNull(entry.primary)
        assertEquals("กวนอิม ใบที่ ๑๗ · ดี · มีคำแปล", entry.siamsi?.label)
        assertEquals(GUANYIN.stick(17), entry.siamsi?.stick)
    }

    @Test
    fun aSetNoLongerOnTheDeviceFallsBackToItsId() {
        val entry = assertNotNull(journalEntry(siamsi(2, set = "thai28", number = 5), mapOf(1 to HEX), emptyMap()))
        assertEquals("thai28 ใบที่ ๕", entry.siamsi?.label)
        assertNull(entry.siamsi?.stick)
        assertNull(entry.siamsi?.set)
    }

    @Test
    fun aStickMissingFromItsSetStillShowsItsNumber() {
        val entry = assertNotNull(journalEntry(siamsi(2, number = 99), emptyMap(), mapOf("guanyin100" to GUANYIN)))
        assertEquals("กวนอิม ใบที่ ๙๙", entry.siamsi?.label)
    }

    @Test
    fun anIChingReadingKeepsItsHexagram() {
        assertEquals(HEX, journalEntry(iching(1), mapOf(1 to HEX), emptyMap())?.primary)
        assertNull(journalEntry(iching(1), emptyMap(), emptyMap()))
    }

    @Test
    fun aSlipWithoutAnInterpretationCanBeInterpretedFromTheJournal() {
        val route = assertNotNull(siamsiInterpretRoute(siamsi(4)))
        assertEquals(com.iching.ai.navigation.SiamSiRoute("guanyin100", siamsi(4).question, siamsi(4).category?.wire, journalId = 4), route)
        assertNull(siamsiInterpretRoute(siamsi(4, words = "แปลแล้ว")))
        assertNull(siamsiInterpretRoute(iching(5)))
    }
}
