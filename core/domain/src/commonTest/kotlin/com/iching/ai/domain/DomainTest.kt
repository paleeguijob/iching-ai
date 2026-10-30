package com.iching.ai.domain

import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.CoinFace.HEADS
import com.iching.ai.domain.model.CoinFace.TAILS
import com.iching.ai.domain.model.CoinToss
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.LineType.OLD_YANG
import com.iching.ai.domain.model.LineType.OLD_YIN
import com.iching.ai.domain.model.LineType.YOUNG_YANG
import com.iching.ai.domain.model.LineType.YOUNG_YIN
import com.iching.ai.domain.model.Trigram
import com.iching.ai.domain.oracle.OraclePrompt
import com.iching.ai.domain.oracle.OraclePromptBuilder
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.repository.OracleException
import com.iching.ai.domain.repository.OracleRepository
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.domain.usecase.FallbackReason
import com.iching.ai.domain.usecase.InterpretReadingUseCase
import com.iching.ai.domain.usecase.InterpretationUpdate
import com.iching.ai.domain.usecase.ResolveCastUseCase
import com.iching.ai.domain.usecase.TossCoinsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun fakeHexagram(id: Int, pattern: String) = Hexagram(
    id = id, nameCn = "卦$id", pinyin = "p$id", nameTh = "กวา$id — ความหมาย$id", pattern = pattern,
    keywords = emptyList(), judgment = "judgment-$id", image = "image-$id", description = "desc-$id",
    lines = List(6) { "line-$id-${it + 1}" },
)

/** Every pattern maps to a fake hexagram whose id is the binary value + 1. */
private object FakeHexagrams : HexagramRepository {
    private val all = (0 until 64).map { n ->
        val pattern = (0 until 6).joinToString("") { bit -> if (n shr bit and 1 == 1) "1" else "0" }
        fakeHexagram(n + 1, pattern)
    }
    override suspend fun all() = all
    override suspend fun byId(id: Int) = all.first { it.id == id }
    override suspend fun byPattern(pattern: String) = all.first { it.pattern == pattern }
}

private class FakeSettings(initial: AppSettings) : SettingsRepository {
    override val settings = MutableStateFlow(initial)
    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        settings.value = transform(settings.value)
    }
}

private class FakeOracle(private val answer: () -> Flow<String>) : OracleRepository {
    var lastPrompt: OraclePrompt? = null
    override fun interpret(prompt: OraclePrompt, apiKey: String, model: String): Flow<String> {
        lastPrompt = prompt
        return answer()
    }
}

class DomainTest {

    @Test
    fun coinSumsMapToTraditionalLineValues() {
        assertEquals(OLD_YIN, CoinToss(listOf(TAILS, TAILS, TAILS)).line)
        assertEquals(YOUNG_YANG, CoinToss(listOf(HEADS, TAILS, TAILS)).line)
        assertEquals(YOUNG_YIN, CoinToss(listOf(HEADS, HEADS, TAILS)).line)
        assertEquals(OLD_YANG, CoinToss(listOf(HEADS, HEADS, HEADS)).line)
    }

    @Test
    fun changingLinesFlipOnlyAfterChange() {
        assertTrue(OLD_YANG.isYang && !OLD_YANG.isYangAfterChange)
        assertTrue(!OLD_YIN.isYang && OLD_YIN.isYangAfterChange)
        assertTrue(YOUNG_YANG.isYangAfterChange)
        assertTrue(!YOUNG_YIN.isYangAfterChange)
    }

    @Test
    fun tossProducesAllFourLineTypesWithExpectedFrequencies() {
        val toss = TossCoinsUseCase(Random(42))
        val counts = List(16_000) { toss().line }.groupingBy { it }.eachCount()
        // Expected probabilities: 6 → 1/8, 7 → 3/8, 8 → 3/8, 9 → 1/8
        assertTrue(counts.getValue(OLD_YIN) in 1_700..2_300)
        assertTrue(counts.getValue(YOUNG_YANG) in 5_600..6_400)
        assertTrue(counts.getValue(YOUNG_YIN) in 5_600..6_400)
        assertTrue(counts.getValue(OLD_YANG) in 1_700..2_300)
    }

    @Test
    fun resolveBuildsPrimaryAndRelatingFromBottomUp() = runTest {
        val lines = listOf(OLD_YANG, YOUNG_YIN, YOUNG_YANG, YOUNG_YIN, YOUNG_YIN, OLD_YIN)
        val cast = ResolveCastUseCase(FakeHexagrams)(lines)
        assertEquals("101000", cast.primary.pattern)
        assertEquals("001001", cast.relating?.pattern)
        assertEquals(listOf(1, 6), cast.changingPositions)
    }

    @Test
    fun resolveHasNoRelatingWhenNothingChanges() = runTest {
        val cast = ResolveCastUseCase(FakeHexagrams)(List(6) { YOUNG_YANG })
        assertEquals("111111", cast.primary.pattern)
        assertNull(cast.relating)
    }

    @Test
    fun trigramsSplitPatternLowerThenUpper() {
        val kun47 = fakeHexagram(47, "010110")
        assertEquals(Trigram.KAN, kun47.lowerTrigram)
        assertEquals(Trigram.DUI, kun47.upperTrigram)
        assertEquals("กวา47", kun47.shortNameTh)
        assertEquals("ความหมาย47", kun47.meaningTh)
    }

    @Test
    fun promptCarriesPersonaQuestionAndOnlyChangingLines() = runTest {
        val cast = ResolveCastUseCase(FakeHexagrams)(listOf(YOUNG_YANG, OLD_YIN, YOUNG_YANG, YOUNG_YIN, YOUNG_YIN, YOUNG_YIN))
        val prompt = OraclePromptBuilder.build("ควรเปลี่ยนงานไหม", cast)
        assertTrue(prompt.systemInstruction.contains("อาตมา"))
        assertTrue(prompt.userMessage.contains("ควรเปลี่ยนงานไหม"))
        assertTrue(prompt.userMessage.contains("line-${cast.primary.id}-2"))
        assertTrue(!prompt.userMessage.contains("line-${cast.primary.id}-1"))
        assertTrue(prompt.userMessage.contains("judgment-${cast.relating!!.id}"))
    }

    @Test
    fun interpretStreamsAccumulatedTextThenCompletesAsAi() = runTest {
        val cast = ResolveCastUseCase(FakeHexagrams)(List(6) { YOUNG_YIN })
        val oracle = FakeOracle { flowOf("สายน้ำ", "ย่อมไหล") }
        val updates = InterpretReadingUseCase(oracle, FakeSettings(AppSettings(apiKey = "k")))("q", cast).toList()
        assertEquals(InterpretationUpdate.Streaming("สายน้ำ"), updates[0])
        assertEquals(InterpretationUpdate.Streaming("สายน้ำย่อมไหล"), updates[1])
        assertEquals(InterpretationUpdate.Completed("สายน้ำย่อมไหล", InterpretationSource.AI), updates.last())
    }

    @Test
    fun interpretFallsBackToScriptureWithoutApiKey() = runTest {
        val cast = ResolveCastUseCase(FakeHexagrams)(List(6) { YOUNG_YIN })
        val oracle = FakeOracle { flowOf("never") }
        val result = InterpretReadingUseCase(oracle, FakeSettings(AppSettings()))("q", cast).toList().single()
        assertIs<InterpretationUpdate.Completed>(result)
        assertEquals(InterpretationSource.SCRIPTURE, result.source)
        assertEquals(FallbackReason.NO_API_KEY, result.fallbackReason)
        assertTrue(result.text.contains("judgment-${cast.primary.id}"))
        assertNull(oracle.lastPrompt)
    }

    @Test
    fun interpretFallsBackOnErrorBeforeAnyText() = runTest {
        val cast = ResolveCastUseCase(FakeHexagrams)(List(6) { YOUNG_YIN })
        val oracle = FakeOracle { flow { throw OracleException.InvalidApiKey() } }
        val result = InterpretReadingUseCase(oracle, FakeSettings(AppSettings(apiKey = "bad")))("q", cast).toList().last()
        assertIs<InterpretationUpdate.Completed>(result)
        assertEquals(FallbackReason.INVALID_API_KEY, result.fallbackReason)
    }

    @Test
    fun interpretKeepsPartialAnswerWhenStreamBreaks() = runTest {
        val cast = ResolveCastUseCase(FakeHexagrams)(List(6) { YOUNG_YIN })
        val oracle = FakeOracle {
            flow {
                emit("ครึ่งหนึ่ง")
                throw OracleException.Network()
            }
        }
        val result = InterpretReadingUseCase(oracle, FakeSettings(AppSettings(apiKey = "k")))("q", cast).toList().last()
        assertEquals(InterpretationUpdate.Completed("ครึ่งหนึ่ง", InterpretationSource.AI), result)
    }

    @Test
    fun lineValuesRoundTrip() {
        LineType.entries.forEach { assertEquals(it, LineType.fromValue(it.value)) }
    }
}
