package com.iching.ai.domain

import com.iching.ai.domain.model.CoinFace.HEADS
import com.iching.ai.domain.model.CoinFace.TAILS
import com.iching.ai.domain.model.CoinToss
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.LineType.OLD_YANG
import com.iching.ai.domain.model.LineType.OLD_YIN
import com.iching.ai.domain.model.LineType.YOUNG_YANG
import com.iching.ai.domain.model.LineType.YOUNG_YIN
import com.iching.ai.domain.model.Outcome
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.oracle.ScriptureReadingComposer
import com.iching.ai.domain.model.Trigram
import com.iching.ai.domain.repository.ApiException
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.repository.JournalRepository
import com.iching.ai.domain.siamsi.SiamSiPick
import com.iching.ai.domain.usecase.DeleteReadingUseCase
import com.iching.ai.domain.usecase.FilterJournalUseCase
import com.iching.ai.domain.usecase.GetReadingUseCase
import com.iching.ai.domain.usecase.SaveOutcomeUseCase
import com.iching.ai.domain.usecase.ResolveCastUseCase
import com.iching.ai.domain.usecase.TossCoinsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
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

private fun fakeReading(id: Long, serverId: String?) = Reading(
    id = id, question = "q", createdAtMillis = 0, lines = List(6) { YOUNG_YANG }, primaryId = 1, relatingId = null,
    interpretation = "text", source = InterpretationSource.AI, serverId = serverId,
)

private class FakeJournal(vararg initial: Reading) : JournalRepository {
    val readings = initial.toMutableList()
    val pending = mutableListOf<String>()
    override fun observeAll(): Flow<List<Reading>> = flowOf(readings)
    override suspend fun get(id: Long) = readings.firstOrNull { it.id == id }
    override suspend fun save(reading: Reading): Long {
        readings.removeAll { it.id == reading.id }
        readings += reading
        return reading.id
    }
    override suspend fun delete(id: Long) {
        readings.removeAll { it.id == id }
    }
    override suspend fun markDeleted(serverId: String) {
        pending += serverId
    }
    override suspend fun pendingDeletions() = pending.toList()
    override suspend fun deletionSynced(serverId: String) {
        pending -= serverId
    }
    val unsynced = mutableListOf<String>()
    override suspend fun markOutcomeUnsynced(serverId: String) {
        unsynced += serverId
    }
    override suspend fun unsyncedOutcomes() = readings.filter { it.serverId in unsynced }
    override suspend fun outcomeSynced(serverId: String) {
        unsynced -= serverId
    }
    override suspend fun mergeFromServer(readings: List<Reading>) = 0
    override suspend fun clear() = readings.clear()
}

class DeleteReadingTest {
    @Test
    fun deletesOnTheServerToo() = runTest {
        val journal = FakeJournal(fakeReading(1, "s1"))
        val deleted = mutableListOf<String>()
        DeleteReadingUseCase(journal) { deleted += it }(1)
        assertEquals(emptyList(), journal.readings)
        assertEquals(listOf("s1"), deleted)
        assertEquals(emptyList(), journal.pending)
    }

    @Test
    fun offlineDeleteStaysPendingForLater() = runTest {
        val journal = FakeJournal(fakeReading(1, "s1"))
        DeleteReadingUseCase(journal) { throw ApiException(FailureReason.NETWORK, "offline") }(1)
        assertEquals(emptyList(), journal.readings)
        assertEquals(listOf("s1"), journal.pending)
    }

    @Test
    fun readingCastOnThisDeviceNeverCallsTheServer() = runTest {
        val journal = FakeJournal(fakeReading(1, null))
        DeleteReadingUseCase(journal) { error("no server copy exists") }(1)
        assertEquals(emptyList(), journal.readings)
        assertEquals(emptyList(), journal.pending)
    }
}

class SaveOutcomeTest {
    private val answered = fakeReading(1, "s1").copy(outcome = Outcome.YES, outcomeNote = "note")

    @Test
    fun savesOnTheServerToo() = runTest {
        val journal = FakeJournal(fakeReading(1, "s1"))
        val sent = mutableListOf<Triple<String, Outcome?, String?>>()
        SaveOutcomeUseCase(journal) { id, outcome, note -> sent += Triple(id, outcome, note) }(answered)
        assertEquals(listOf(Triple<String, Outcome?, String?>("s1", Outcome.YES, "note")), sent)
        assertEquals(emptyList(), journal.unsynced)
    }

    @Test
    fun offlineOutcomeStaysOnTheDeviceAndMarkedForLater() = runTest {
        val journal = FakeJournal(fakeReading(1, "s1"))
        SaveOutcomeUseCase(journal) { _, _, _ -> throw ApiException(FailureReason.NETWORK, "offline") }(answered)
        assertEquals(Outcome.YES, journal.get(1)?.outcome)
        assertEquals(listOf("s1"), journal.unsynced)
    }

    @Test
    fun readingCastOnThisDeviceNeverCallsTheServer() = runTest {
        val journal = FakeJournal(fakeReading(1, null))
        SaveOutcomeUseCase(journal) { _, _, _ -> error("no server copy exists") }(fakeReading(1, null).copy(outcome = Outcome.NO))
        assertEquals(Outcome.NO, journal.get(1)?.outcome)
        assertEquals(emptyList(), journal.unsynced)
    }
}

class GetReadingTest {
    private fun useCase(journal: FakeJournal) = GetReadingUseCase(journal, ResolveCastUseCase(FakeHexagrams))

    @Test
    fun aReadingFetchedWithoutTextShowsTheScripture() = runTest {
        val blank = fakeReading(1, "s1").copy(interpretation = "", source = InterpretationSource.SCRIPTURE)
        val text = useCase(FakeJournal(blank))(1)?.interpretation.orEmpty()
        assertTrue("judgment-64" in text, text)
    }

    @Test
    fun aSiamSiSlipWithoutInterpretationOpensAsItIs() = runTest {
        val slip = fakeReading(1, null).copy(
            lines = emptyList(), primaryId = 0, interpretation = "", siamsi = SiamSiPick("guanyin100", 62),
        )
        assertEquals(slip, useCase(FakeJournal(slip))(1))
    }

    @Test
    fun aReadingWithTextIsLeftAlone() = runTest {
        assertEquals("text", useCase(FakeJournal(fakeReading(1, "s1")))(1)?.interpretation)
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
    fun scriptureFallbackIncludesChangingLinesAndRelatingHexagram() = runTest {
        val cast = ResolveCastUseCase(FakeHexagrams)(listOf(YOUNG_YANG, OLD_YIN, YOUNG_YANG, YOUNG_YIN, YOUNG_YIN, YOUNG_YIN))
        val text = ScriptureReadingComposer.compose(cast)
        assertTrue(text.contains("judgment-${cast.primary.id}"))
        assertTrue(text.contains("line-${cast.primary.id}-2"))
        assertTrue(!text.contains("line-${cast.primary.id}-1"))
        assertTrue(text.contains("judgment-${cast.relating!!.id}"))
    }

    @Test
    fun freeJournalShowsOnlyRecentDays() {
        val day = 24L * 60 * 60 * 1000
        val now = 100 * day
        fun reading(ageDays: Int) = Reading(
            question = "q", createdAtMillis = now - ageDays * day, lines = List(6) { YOUNG_YANG },
            primaryId = 1, relatingId = null, interpretation = "", source = InterpretationSource.AI,
        )
        val all = listOf(reading(0), reading(6), reading(8), reading(30))
        val (visible, hidden) = FilterJournalUseCase()(all, 7, now)
        assertEquals(2, visible.size)
        assertEquals(2, hidden)
        assertEquals(all to 0, FilterJournalUseCase()(all, null, now))
    }

    @Test
    fun lineValuesRoundTrip() {
        LineType.entries.forEach { assertEquals(it, LineType.fromValue(it.value)) }
    }
}
