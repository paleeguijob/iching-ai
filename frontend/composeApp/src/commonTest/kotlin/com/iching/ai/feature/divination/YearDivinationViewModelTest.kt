package com.iching.ai.feature.divination

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.CampaignStatus
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.repository.DivinationEvent
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.repository.JournalRepository
import com.iching.ai.domain.repository.LimitReason
import com.iching.ai.domain.repository.OracleApi
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.ResolveCastUseCase
import com.iching.ai.domain.usecase.SaveReadingUseCase
import com.iching.ai.domain.usecase.TossCoinsUseCase
import com.iching.ai.platform.NativeAnalytics
import com.iching.ai.platform.NativeBridges
import com.iching.ai.session.AppSession
import com.iching.ai.session.FakeApi
import com.iching.ai.session.FakeAuth
import com.iching.ai.session.FakeJournal
import com.iching.ai.session.FakePurchases
import com.iching.ai.session.FirebaseApiSession
import com.iching.ai.session.NoAds
import com.iching.ai.session.NoNotifications
import com.iching.ai.session.NoSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private object YearHexagrams : HexagramRepository {
    private fun hexagram(id: Int, pattern: String) = Hexagram(
        id = id, nameCn = "", pinyin = "", nameTh = "", pattern = pattern,
        keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
    )
    override suspend fun all() = emptyList<Hexagram>()
    override suspend fun byId(id: Int) = hexagram(id, "000000")
    override suspend fun byPattern(pattern: String) = hexagram(pattern.toInt(2) + 1, pattern)
}

private class SavingJournal : JournalRepository by FakeJournal() {
    val rows = linkedMapOf<Long, Reading>()
    private var nextId = 1L
    override suspend fun get(id: Long) = rows[id]
    override suspend fun save(reading: Reading): Long {
        val id = if (reading.id == 0L) nextId++ else reading.id
        rows[id] = reading.copy(id = id)
        return id
    }
}

private class YearApi : OracleApi by FakeApi() {
    var events: List<DivinationEvent> = emptyList()
    val yearQuestions = mutableListOf<String>()
    var normalDivineCalls = 0
    override fun divineYear(question: String): Flow<DivinationEvent> {
        yearQuestions += question
        return flowOf(*events.toTypedArray())
    }
    override fun divine(question: String, category: QuestionCategory?): Flow<DivinationEvent> {
        normalDivineCalls++
        return emptyFlow()
    }
}

private class Analytics : NativeAnalytics {
    val events = mutableListOf<Pair<String, Map<String, String>>>()
    override fun logEvent(name: String, params: Map<String, String>) {
        events += name to params
    }
    override fun setUserId(uid: String?) = Unit
    override fun recordError(message: String) = Unit
}

private val YEAR_LINES = listOf(7, 8, 9, 6, 7, 8).map(LineType::fromValue)

private fun status(
    premium: Boolean = false,
    includedLeft: Int = 1,
    readingsLeft: Int = 1,
    adCreditsLeft: Int = 0,
    adsWatchableLeft: Int = 0,
) = AccountStatus(
    premium = premium, premiumUntilMillis = null, coins = 0, includedLeft = includedLeft, adCreditsLeft = adCreditsLeft,
    adsWatchableLeft = adsWatchableLeft, readingsLeft = readingsLeft, followUpsPerReading = 0, journalDays = 7, aiEnabled = true,
    campaign = CampaignStatus(id = "cny-2027", year = 2027, active = true, endsAtMillis = 0),
)

@OptIn(ExperimentalCoroutinesApi::class)
class YearDivinationViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val api = YearApi()
    private val journal = SavingJournal()
    private val analytics = Analytics()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(account: AccountStatus = status()): DivinationViewModel {
        val auth = FakeAuth()
        val session = AppSession(
            NativeBridges(auth, FakePurchases(), NoAds, analytics, NoNotifications),
            FirebaseApiSession(auth), api, FakeJournal(), NoSettings, CoroutineScope(dispatcher),
        )
        session.updateStatus(account)
        return DivinationViewModel(
            question = "ปีนี้การงานของข้าพเจ้าควรมุ่งไปทางใด",
            category = QuestionCategory.NEWYEAR,
            tossCoins = TossCoinsUseCase(),
            resolveCast = ResolveCastUseCase(YearHexagrams),
            saveReading = SaveReadingUseCase(journal),
            api = api,
            session = session,
            observeSettings = ObserveSettingsUseCase(NoSettings),
            yearOf = 2027,
        )
    }

    private fun advance(millis: Long) {
        scheduler.advanceTimeBy(millis)
        scheduler.runCurrent()
    }

    @Test
    fun yearModeSkipsTheCoinTossAndAsksForDivineYear() {
        api.events = listOf(DivinationEvent.Cast("r1", YEAR_LINES, emptyList()), DivinationEvent.Delta("ถ้อยคำ"))
        val vm = viewModel()
        scheduler.runCurrent()

        assertEquals(listOf("ปีนี้การงานของข้าพเจ้าควรมุ่งไปทางใด"), api.yearQuestions)
        assertEquals(0, api.normalDivineCalls)
        assertEquals(DivinationPhase.REVEAL, vm.state.value.phase)
        assertEquals(YEAR_LINES, vm.state.value.cast?.lines)
        assertTrue(vm.state.value.tosses.isEmpty(), "no coin was tossed")
        assertEquals("r1", vm.state.value.serverReadingId)
    }

    @Test
    fun yearModeQuotaLimitShowsTheSameUpsellAsANormalReading() {
        val empty = status(includedLeft = 0, readingsLeft = 0)
        api.events = listOf(
            DivinationEvent.Cast("r1", YEAR_LINES, emptyList()),
            DivinationEvent.Limited(LimitReason.QUOTA, null, empty),
        )
        val vm = viewModel(empty)
        scheduler.runCurrent()
        advance(DivinationViewModel.REVEAL_MILLIS)

        val s = vm.state.value
        assertEquals(DivinationPhase.COMPLETE, s.phase)
        assertEquals(ReadingNotice.Limited(LimitReason.QUOTA, null), s.notice)
        assertEquals(InterpretationSource.SCRIPTURE, s.source)
        assertTrue(offersQuotaUpsell(s.notice, s.status, canRetry = true))
    }

    @Test
    fun yearReadingIsSavedWithYearOfAndCategoryNewyear() {
        api.events = listOf(
            DivinationEvent.Cast("r1", YEAR_LINES, emptyList()),
            DivinationEvent.Delta("ปีนี้ควรเดินช้า ๆ"),
            DivinationEvent.Done(status(includedLeft = 0, readingsLeft = 0)),
        )
        val vm = viewModel()
        scheduler.runCurrent()
        advance(DivinationViewModel.REVEAL_MILLIS)

        assertEquals(DivinationPhase.COMPLETE, vm.state.value.phase)
        val saved = journal.rows.values.single()
        assertEquals(2027, saved.yearOf)
        assertEquals(QuestionCategory.NEWYEAR, saved.category)
        assertEquals("r1", saved.serverId)
        assertEquals(InterpretationSource.AI, saved.source)
        assertEquals("ปีนี้ควรเดินช้า ๆ", saved.interpretation)
    }

    @Test
    fun yearReadingKeepsItsYearWhenTheStatusHasNoCampaign() {
        api.events = listOf(DivinationEvent.Cast("r1", YEAR_LINES, emptyList()), DivinationEvent.Done(null))
        viewModel(status().copy(campaign = null))
        scheduler.runCurrent()
        advance(DivinationViewModel.REVEAL_MILLIS)

        assertEquals(2027, journal.rows.values.single().yearOf)
        assertEquals("cny-2027", analytics.events.single { it.first == "year_ai_done" }.second["campaign_id"])
    }

    @Test
    fun noYearHexagramShowsTheScriptureOnlyNoticeAndSavesNothing() {
        api.events = listOf(DivinationEvent.Failed(FailureReason.NO_YEAR_HEXAGRAM))
        val vm = viewModel()
        scheduler.runCurrent()

        val s = vm.state.value
        assertEquals(DivinationPhase.COMPLETE, s.phase)
        assertEquals(ReadingNotice.Failed(FailureReason.NO_YEAR_HEXAGRAM), s.notice)
        assertNull(s.cast)
        assertTrue(s.error != null, "the screen has words to show instead of a cast")
        assertTrue(journal.rows.isEmpty())
    }

    @Test
    fun logsYearAiDoneWithTheCharge() {
        api.events = listOf(
            DivinationEvent.Cast("r1", YEAR_LINES, emptyList()),
            DivinationEvent.Delta("ถ้อยคำ"),
            DivinationEvent.Done(status(includedLeft = 0, readingsLeft = 0)),
        )
        viewModel(status(includedLeft = 0, readingsLeft = 1, adCreditsLeft = 1))
        scheduler.runCurrent()
        assertTrue(analytics.events.none { it.first == "year_ai_done" }, "not done before the reveal ends")

        advance(DivinationViewModel.REVEAL_MILLIS)
        assertEquals(
            listOf("year_ai_done" to mapOf("campaign_id" to "cny-2027", "charge" to "ad")),
            analytics.events.filter { it.first == "year_ai_done" },
        )
    }

    @Test
    fun aFreeSeekersChargeIsLoggedAsFree() {
        api.events = listOf(DivinationEvent.Cast("r1", YEAR_LINES, emptyList()), DivinationEvent.Done(status(includedLeft = 0, readingsLeft = 0)))
        viewModel(status(includedLeft = 1))
        scheduler.runCurrent()
        advance(DivinationViewModel.REVEAL_MILLIS)
        assertEquals("free", analytics.events.single { it.first == "year_ai_done" }.second["charge"])
    }
}
