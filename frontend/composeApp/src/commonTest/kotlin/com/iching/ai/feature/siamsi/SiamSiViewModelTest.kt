package com.iching.ai.feature.siamsi

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.repository.DivinationEvent
import com.iching.ai.domain.repository.JournalRepository
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.domain.repository.SiamSiRepository
import com.iching.ai.domain.siamsi.SiamSiLevel
import com.iching.ai.domain.siamsi.SiamSiPick
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiSetRef
import com.iching.ai.domain.siamsi.SiamSiStick
import com.iching.ai.domain.siamsi.SiamSiTopic
import com.iching.ai.domain.siamsi.SiamSiUsage
import com.iching.ai.feature.home.PreCastSheetKind
import com.iching.ai.navigation.SiamSiRoute
import com.iching.ai.platform.NativeBridges
import com.iching.ai.session.AppSession
import com.iching.ai.session.FakeAds
import com.iching.ai.session.FakeApi
import com.iching.ai.session.FakeAuth
import com.iching.ai.session.FakeJournal
import com.iching.ai.session.FakePurchases
import com.iching.ai.session.FirebaseApiSession
import com.iching.ai.session.LoggedAnalytics
import com.iching.ai.session.NoNotifications
import com.iching.ai.session.NoSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Answers each nextInt with the next scripted value: stick numbers for draws, 0..3 for pua tosses. */
private class ScriptedRandom(vararg values: Int) : Random() {
    private val queue = ArrayDeque(values.toList())
    override fun nextBits(bitCount: Int): Int = error("not scripted")
    override fun nextInt(until: Int): Int = queue.removeFirst()
    override fun nextInt(from: Int, until: Int): Int = queue.removeFirst()
}

private const val SAK = 0
private const val LAUGH = 2
private const val NO = 3

private fun stick(n: Int) = SiamSiStick(
    number = n, gradeOrig = "中吉", level = SiamSiLevel.FAIR, labelOrig = null,
    poemOrig = List(4) { "句$it" }, poemTh = List(4) { "วรรค$it" }, story = "s", summary = "sum",
    topics = SiamSiTopic.entries.associateWith { it.labelTh }, keywords = emptyList(),
)

private val GUANYIN = SiamSiSet("guanyin100", "เซียมซีเจ้าแม่กวนอิม", 3, "觀音靈籤", "n", (1..3).map(::stick))

private object OneSet : SiamSiRepository {
    override suspend fun available(refs: List<SiamSiSetRef>) = listOf(GUANYIN)
    override suspend fun set(id: String) = GUANYIN.takeIf { it.id == id }
}

private class SlipSettings(initial: AppSettings) : SettingsRepository {
    val flow = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = flow
    override suspend fun update(transform: (AppSettings) -> AppSettings) = flow.update(transform)
    override suspend fun clear() = Unit
}

/** Keeps every save, giving new rows ids the way Room does. */
private class RecordingJournal(private val base: FakeJournal = FakeJournal()) : JournalRepository by base {
    val rows = mutableMapOf<Long, Reading>()
    var saves = 0
    override fun observeAll(): Flow<List<Reading>> = emptyFlow()
    override suspend fun get(id: Long) = rows[id]
    override suspend fun save(reading: Reading): Long {
        saves++
        val id = if (reading.id == 0L) rows.size + 1L else reading.id
        rows[id] = reading.copy(id = id)
        return id
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SiamSiViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher)
    private val today = 20_400L
    private val api = FakeApi()
    private val ads = FakeAds()
    private val journal = RecordingJournal()
    private lateinit var settings: SlipSettings

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun viewModel(
        vararg random: Int,
        status: AccountStatus? = AccountStatus.UNKNOWN_FREE.copy(readingsLeft = 1, includedLeft = 1),
        usage: SiamSiUsage = SiamSiUsage(today, used = 1, pendingDraw = true),
    ): SiamSiViewModel {
        api.statusNow = status
        settings = SlipSettings(AppSettings(siamsiUsage = usage))
        val auth = FakeAuth()
        val session = AppSession(NativeBridges(auth, FakePurchases(), ads, LoggedAnalytics(), NoNotifications), FirebaseApiSession(auth), api, journal, NoSettings, scope)
        session.start()
        val vm = SiamSiViewModel(
            route = SiamSiRoute("guanyin100", "งานใหม่", QuestionCategory.WORK.wire),
            repository = OneSet,
            api = api,
            session = session,
            journal = journal,
            settings = settings,
            random = ScriptedRandom(*random),
            today = { today },
        )
        scheduler.advanceUntilIdle()
        return vm
    }

    private fun SiamSiViewModel.draw(stick: Int, pua: Int) {
        onShaken()
        scheduler.advanceUntilIdle()
        assertEquals(SiamSiStage.Fallen(stick), stage.value)
        onTossPua()
        scheduler.advanceUntilIdle()
    }

    private fun SiamSiViewModel.reachSlip(): SiamSiStage.Slip {
        draw(1, SAK)
        return assertIs<SiamSiStage.Slip>(stage.value)
    }

    @Test
    fun twoFailedTossesThenSakConfirmsTheThirdStick() {
        val vm = viewModel(1, LAUGH, 2, NO, 3, SAK)
        vm.draw(1, LAUGH)
        assertIs<SiamSiStage.Pua>(vm.stage.value)
        vm.onReshake()
        vm.draw(2, NO)
        vm.onReshake()
        vm.draw(3, SAK)
        assertEquals(SiamSiStage.Slip(stick(3), autoConfirmed = false), vm.stage.value)
    }

    @Test
    fun threeFailedTossesConfirmTheLastStickAnyway() {
        val vm = viewModel(1, NO, 2, LAUGH, 3, NO)
        vm.draw(1, NO)
        vm.onReshake()
        vm.draw(2, LAUGH)
        vm.onReshake()
        vm.draw(3, NO)
        assertEquals(SiamSiStage.Slip(stick(3), autoConfirmed = true), vm.stage.value)
    }

    @Test
    fun reshakingNeitherPlaysAnAdNorSpendsADraw() {
        val vm = viewModel(1, NO, 2, SAK)
        vm.draw(1, NO)
        vm.onReshake()
        vm.draw(2, SAK)
        assertEquals(1, settings.flow.value.siamsiUsage.used)
        assertEquals(0, api.siamsiCalls.size)
    }

    @Test
    fun theSlipSpendsThePendingDrawAndIsSavedOnce() {
        val vm = viewModel(1, SAK)
        vm.reachSlip()
        assertEquals(false, settings.flow.value.siamsiUsage.pendingDraw)
        assertEquals(1, journal.saves)
        val row = journal.rows.values.single()
        assertEquals(SiamSiPick("guanyin100", 1), row.siamsi)
        assertEquals(emptyList(), row.lines)
        assertEquals(0, row.primaryId)
        assertEquals(InterpretationSource.SCRIPTURE, row.source)
        assertEquals("งานใหม่", row.question)
        assertEquals(QuestionCategory.WORK, row.category)
    }

    @Test
    fun withoutAPaidDrawThereIsNothingToShake() {
        val vm = viewModel(usage = SiamSiUsage(today, used = 1, pendingDraw = false))
        assertEquals(SiamSiStage.Unpaid, vm.stage.value)
        vm.onShaken()
        scheduler.advanceUntilIdle()
        assertEquals(SiamSiStage.Unpaid, vm.stage.value)
    }

    @Test
    fun interpretingOfflineSaysSoAndKeepsTheSlip() {
        val vm = viewModel(1, SAK, status = null)
        val slip = vm.reachSlip()
        vm.onInterpret()
        scheduler.advanceUntilIdle()
        assertEquals(0, api.siamsiCalls.size)
        assertEquals("ต้องเชื่อมต่ออินเทอร์เน็ตเพื่อแปลความหมาย", vm.interpret.value.message)
        assertEquals(slip, vm.stage.value)
    }

    @Test
    fun noCreditButAnAdLeftOpensTheAdSheet() {
        val vm = viewModel(1, SAK, status = AccountStatus.UNKNOWN_FREE.copy(readingsLeft = 0, adsWatchableLeft = 2))
        vm.reachSlip()
        vm.onInterpret()
        scheduler.advanceUntilIdle()
        assertEquals(PreCastSheetKind.AD, vm.interpret.value.sheet?.kind)
        assertEquals(0, api.siamsiCalls.size)
    }

    @Test
    fun aStreamedInterpretationUpdatesTheSameJournalRow() {
        val vm = viewModel(1, SAK)
        vm.reachSlip()
        api.siamsiEvents = listOf(
            DivinationEvent.Created("r1"),
            DivinationEvent.Delta("ใบนี้"),
            DivinationEvent.Delta("ดี"),
            DivinationEvent.Done(null),
        )
        vm.onInterpret()
        scheduler.advanceUntilIdle()
        assertEquals("ใบนี้ดี", vm.interpret.value.text)
        assertEquals(false, vm.interpret.value.streaming)
        val row = journal.rows.values.single()
        assertEquals("r1", row.serverId)
        assertEquals("ใบนี้ดี", row.interpretation)
        assertEquals(InterpretationSource.AI, row.source)
        assertEquals(listOf<Pair<SiamSiPick, String?>>(SiamSiPick("guanyin100", 1) to null), api.siamsiCalls)
    }

    @Test
    fun aCrisisQuestionShowsTheSafetyMessageInsteadOfAnInterpretation() {
        val vm = viewModel(1, SAK)
        vm.reachSlip()
        api.siamsiEvents = listOf(DivinationEvent.Safety("โทร 1323"))
        vm.onInterpret()
        scheduler.advanceUntilIdle()
        assertEquals("โทร 1323", vm.interpret.value.safety)
        assertEquals("", vm.interpret.value.text)
        val row = journal.rows.values.single()
        assertEquals("", row.interpretation)
        assertEquals(InterpretationSource.SCRIPTURE, row.source)
    }

    @Test
    fun aRetryAfterACutStreamReusesTheServerReading() {
        val vm = viewModel(1, SAK)
        vm.reachSlip()
        api.siamsiEvents = listOf(DivinationEvent.Created("r1"), DivinationEvent.Failed(com.iching.ai.domain.repository.FailureReason.NETWORK))
        vm.onInterpret()
        scheduler.advanceUntilIdle()
        assertNotNull(vm.interpret.value.message)
        api.siamsiEvents = listOf(DivinationEvent.Delta("ต่อ"), DivinationEvent.Done(null))
        vm.onInterpret()
        scheduler.advanceUntilIdle()
        assertEquals("r1", api.siamsiCalls.last().second)
        assertNull(vm.interpret.value.message)
        assertTrue(vm.interpret.value.text.isNotEmpty())
    }
}
