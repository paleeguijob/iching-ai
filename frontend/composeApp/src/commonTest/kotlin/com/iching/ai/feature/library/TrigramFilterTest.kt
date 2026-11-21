package com.iching.ai.feature.library

import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.Trigram
import kotlin.test.Test
import kotlin.test.assertEquals

class TrigramFilterTest {
    private fun hexagram(id: Int, nameCn: String, pattern: String) = Hexagram(
        id = id, nameCn = nameCn, pinyin = "", nameTh = "", pattern = pattern,
        keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
    )

    private val qian = hexagram(1, "乾", "111111")
    private val kun = hexagram(2, "坤", "000000")
    private val zhun = hexagram(3, "屯", "100010") // lower 震 100, upper 坎 010
    private val all = listOf(qian, kun, zhun)

    @Test
    fun nullTrigramKeepsAll() = assertEquals(all, filterByTrigram(all, null))

    @Test
    fun matchesLowerTrigram() = assertEquals(listOf(zhun), filterByTrigram(all, Trigram.ZHEN))

    @Test
    fun matchesUpperTrigram() = assertEquals(listOf(zhun), filterByTrigram(all, Trigram.KAN))

    @Test
    fun matchesEitherPositionOnce() = assertEquals(listOf(qian), filterByTrigram(all, Trigram.QIAN))

    @Test
    fun noMatchIsEmpty() = assertEquals(emptyList(), filterByTrigram(all, Trigram.LI))

    @Test
    fun visibleAppliesTheSelectedTrigram() =
        assertEquals(listOf(kun), LibraryUiState(hexagrams = all, trigram = Trigram.KUN).visible)
}
