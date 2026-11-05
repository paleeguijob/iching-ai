package com.iching.ai.feature.divination

import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.Outcome
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.repository.DivinationEvent
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.repository.JournalRepository
import com.iching.ai.domain.repository.OracleApi
import com.iching.ai.domain.usecase.GetReadingUseCase
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.ResolveCastUseCase
import com.iching.ai.domain.usecase.SaveReadingUseCase
import com.iching.ai.domain.usecase.TossCoinsUseCase
import com.iching.ai.platform.NativeBridges
import com.iching.ai.session.AppSession
import com.iching.ai.session.FakeApi
import com.iching.ai.session.FakeAuth
import com.iching.ai.session.FakeJournal
import com.iching.ai.session.FakePurchases
import com.iching.ai.session.FirebaseApiSession
import com.iching.ai.session.NoAds
import com.iching.ai.session.NoAnalytics
import com.iching.ai.session.NoNotifications
import com.iching.ai.session.NoSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private object SomeHexagram : HexagramRepository {
    private fun hexagram(id: Int, pattern: String) = Hexagram(
        id = id, nameCn = "", pinyin = "", nameTh = "", pattern = pattern,
        keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
    )
    override suspend fun all() = emptyList<Hexagram>()
    override suspend fun byId(id: Int) = hexagram(id, "000000")
    override suspend fun byPattern(pattern: String) = hexagram(pattern.toInt(2) + 1, pattern)
}

/** Keeps what is saved, like the Room journal: id 0 inserts, any other id replaces. */
private class RecordingJournal : JournalRepository by FakeJournal() {
    val rows = linkedMapOf<Long, Reading>()
    private var nextId = 1L
    override suspend fun get(id: Long) = rows[id]
    override suspend fun save(reading: Reading): Long {
        val id = if (reading.id == 0L) nextId++ else reading.id
        rows[id] = reading.copy(id = id)
        return id
    }
}

/** The server's stream is fed by the test, one event at a time. */
private class StreamingApi : OracleApi by FakeApi() {
    val divineEvents = Channel<DivinationEvent>(Channel.UNLIMITED)
    var againEvents: List<DivinationEvent> = emptyList()
    val againCalls = mutableListOf<String>()
    override fun divine(question: String, category: QuestionCategory?): Flow<DivinationEvent> = divineEvents.receiveAsFlow()
    override fun interpretAgain(readingId: String): Flow<DivinationEvent> {
        againCalls += readingId
        return flowOf(*againEvents.toTypedArray())
    }
}

private val LINES = listOf(7, 8, 9, 6, 7, 8).map(LineType::fromValue)

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingSurvivesLeavingTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val api = StreamingApi()
    private val journal = RecordingJournal()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(resumeId: Long? = null): DivinationViewModel {
        val auth = FakeAuth()
        val session = AppSession(
            NativeBridges(auth, FakePurchases(), NoAds, NoAnalytics, NoNotifications),
            FirebaseApiSession(auth),
            api,
            FakeJournal(),
            NoSettings,
            CoroutineScope(dispatcher),
        )
        val resolve = ResolveCastUseCase(SomeHexagram)
        return DivinationViewModel(
            question = "งานใหม่ดีไหม",
            category = null,
            tossCoins = TossCoinsUseCase(),
            resolveCast = resolve,
            saveReading = SaveReadingUseCase(journal),
            api = api,
            session = session,
            observeSettings = ObserveSettingsUseCase(NoSettings),
            resumeReadingId = resumeId,
            getReading = GetReadingUseCase(journal, resolve),
        )
    }

    private fun advance(millis: Long) {
        scheduler.advanceTimeBy(millis)
        scheduler.runCurrent()
    }

    @Test
    fun theServerCastIsInTheJournalBeforeTheHermitFinishes() {
        val vm = viewModel()
        assertFalse(vm.state.value.leavingInterrupts, "nothing is lost before the first toss")
        vm.onToss()
        api.divineEvents.trySend(DivinationEvent.Cast("r1", LINES, emptyList()))
        api.divineEvents.trySend(DivinationEvent.Delta("ถ้อยคำแรก"))
        scheduler.runCurrent()

        assertTrue(vm.state.value.leavingInterrupts)
        val saved = journal.rows.values.single()
        assertEquals("r1", saved.serverId)
        assertEquals(LINES, saved.lines)
        assertEquals(InterpretationSource.SCRIPTURE, saved.source)
        assertEquals("", saved.interpretation, "an unfinished reading is saved without the hermit's partial words")
        assertEquals("งานใหม่ดีไหม", saved.question)
    }

    @Test
    fun theFinishedReadingReplacesTheEarlyEntry() {
        val vm = viewModel()
        vm.onTossRemaining()
        api.divineEvents.trySend(DivinationEvent.Cast("r1", LINES, emptyList()))
        api.divineEvents.trySend(DivinationEvent.Delta("คำชี้แนะ"))
        api.divineEvents.trySend(DivinationEvent.Done(null))
        api.divineEvents.close()
        advance(60_000)

        assertEquals(DivinationPhase.COMPLETE, vm.state.value.phase)
        assertFalse(vm.state.value.leavingInterrupts)
        val saved = journal.rows.values.single()
        assertEquals(InterpretationSource.AI, saved.source)
        assertEquals("คำชี้แนะ", saved.interpretation)
        assertEquals(vm.state.value.savedReadingId, saved.id)
        assertTrue(saved.createdAtMillis > 0)
    }

    @Test
    fun resumingAskesTheHermitAboutTheSavedCastAndKeepsTheNotes() {
        journal.rows[5] = Reading(
            id = 5, question = "งานใหม่ดีไหม", createdAtMillis = 1_000, lines = LINES, primaryId = 1, relatingId = null,
            interpretation = "", source = InterpretationSource.SCRIPTURE, serverId = "r1",
            outcome = Outcome.PARTLY, outcomeNote = "ได้งานแต่ช้า",
        )
        api.againEvents = listOf(DivinationEvent.Cast("r1", LINES, emptyList()), DivinationEvent.Delta("คำชี้แนะ"), DivinationEvent.Done(null))
        val vm = viewModel(resumeId = 5)
        scheduler.runCurrent()

        assertEquals(listOf("r1"), api.againCalls)
        assertEquals(DivinationPhase.COMPLETE, vm.state.value.phase)
        assertEquals(6, vm.state.value.tosses.size)
        val saved = journal.rows.values.single()
        assertEquals(5, saved.id)
        assertEquals(InterpretationSource.AI, saved.source)
        assertEquals("คำชี้แนะ", saved.interpretation)
        assertEquals(1_000, saved.createdAtMillis)
        assertEquals(Outcome.PARTLY, saved.outcome)
        assertEquals("ได้งานแต่ช้า", saved.outcomeNote)
    }
}
