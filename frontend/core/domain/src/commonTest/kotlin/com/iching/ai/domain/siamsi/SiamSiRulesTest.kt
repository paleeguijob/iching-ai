package com.iching.ai.domain.siamsi

import com.iching.ai.domain.model.QuestionCategory
import kotlin.math.pow
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SiamSiRulesTest {

    @Test
    fun drawStickCoversEveryNumberAndIsUniform() {
        for (count in listOf(28, 60, 100)) {
            val r = Random(42)
            val hits = IntArray(count + 1)
            repeat(count * 2000) { hits[drawStick(count, r)]++ }
            assertEquals(0, hits[0])
            assertTrue((1..count).all { hits[it] > 0 })
            val expected = 2000.0
            val chi = (1..count).sumOf { (hits[it] - expected).pow(2) / expected }
            assertTrue(chi < count * 1.5, "chi-square $chi too high for $count")
        }
    }

    @Test
    fun puaIsHalfSakQuarterEach() {
        val r = Random(7)
        val n = 40_000
        val results = List(n) { tossPua(r) }
        fun share(p: PuaResult) = results.count { it == p }.toDouble() / n
        assertTrue(share(PuaResult.SAK) in 0.48..0.52)
        assertTrue(share(PuaResult.LAUGH) in 0.23..0.27)
        assertTrue(share(PuaResult.NO) in 0.23..0.27)
    }

    @Test
    fun gradesMapToLevels() {
        assertEquals(SiamSiLevel.EXCELLENT, gradeToLevel("上上"))
        listOf("上", "上吉", "大吉").forEach { assertEquals(SiamSiLevel.GOOD, gradeToLevel(it)) }
        listOf("中", "中吉", "中平").forEach { assertEquals(SiamSiLevel.FAIR, gradeToLevel(it)) }
        listOf("下", "下下").forEach { assertEquals(SiamSiLevel.CAUTION, gradeToLevel(it)) }
        assertNull(gradeToLevel("??"))
    }

    @Test
    fun sakConfirmsAndThirdRoundAutoConfirms() {
        assertEquals(17, PuaProgress(listOf(PuaRound(17, PuaResult.SAK))).confirmedStick)
        val failing = PuaProgress(listOf(PuaRound(3, PuaResult.NO), PuaRound(9, PuaResult.LAUGH)))
        assertNull(failing.confirmedStick)
        assertTrue(failing.needsReshake)
        val third = PuaProgress(failing.rounds + PuaRound(21, PuaResult.NO))
        assertEquals(21, third.confirmedStick)
        assertTrue(third.autoConfirmed)
        assertFalse(third.needsReshake)
    }

    @Test
    fun aRoundNotYetTossedNeedsNoReshake() {
        val p = PuaProgress(listOf(PuaRound(5, null)))
        assertNull(p.confirmedStick)
        assertFalse(p.needsReshake)
    }

    @Test
    fun chosenCategoryComesFirst() {
        assertEquals(SiamSiTopic.HEALTH, topicOrder(QuestionCategory.MIND).first())
        assertEquals(SiamSiTopic.WORK, topicOrder(QuestionCategory.WORK).first())
        assertEquals(SiamSiTopic.entries, topicOrder(QuestionCategory.DECISION))
        assertEquals(SiamSiTopic.entries, topicOrder(null))
        assertEquals(7, topicOrder(QuestionCategory.LOVE).distinct().size)
    }
}
