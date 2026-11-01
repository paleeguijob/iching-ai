package com.iching.ai.feature.divination

import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.repository.DivinationEvent
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.repository.OracleApi
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

private object AnyHexagram : HexagramRepository {
    private fun hexagram(id: Int, pattern: String) = Hexagram(
        id = id, nameCn = "", pinyin = "", nameTh = "", pattern = pattern,
        keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
    )
    override suspend fun all() = emptyList<Hexagram>()
    override suspend fun byId(id: Int) = hexagram(id, "000000")
    override suspend fun byPattern(pattern: String) = hexagram(1, pattern)
}

/** Offline, so the coins are cast on the device and the test needs no server events. */
private class OfflineApi : OracleApi by FakeApi() {
    override fun divine(question: String, category: QuestionCategory?): Flow<DivinationEvent> =
        flowOf(DivinationEvent.Failed(FailureReason.NETWORK))
}

@OptIn(ExperimentalCoroutinesApi::class)
class DivinationViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): DivinationViewModel {
        val api = OfflineApi()
        val auth = FakeAuth()
        val session = AppSession(
            NativeBridges(auth, FakePurchases(), NoAds, NoAnalytics, NoNotifications),
            FirebaseApiSession(auth),
            api,
            FakeJournal(),
            NoSettings,
            CoroutineScope(dispatcher),
        )
        return DivinationViewModel(
            question = "",
            category = null,
            tossCoins = TossCoinsUseCase(),
            resolveCast = ResolveCastUseCase(AnyHexagram),
            saveReading = SaveReadingUseCase(FakeJournal()),
            api = api,
            session = session,
            observeSettings = ObserveSettingsUseCase(NoSettings),
        )
    }

    private fun advance(millis: Long) {
        scheduler.advanceTimeBy(millis)
        scheduler.runCurrent()
    }

    private fun assertHoldsThenReveals(vm: DivinationViewModel) {
        assertEquals(6, vm.state.value.tosses.size)
        assertFalse(vm.state.value.isTossing, "the last line's label shows only once the toss has landed")
        assertFalse(vm.state.value.canToss)
        assertEquals(DivinationPhase.CASTING, vm.state.value.phase)
        advance(DivinationViewModel.LAST_TOSS_HOLD_MILLIS - 1)
        assertEquals(DivinationPhase.CASTING, vm.state.value.phase)
        advance(1)
        assertEquals(DivinationPhase.REVEAL, vm.state.value.phase)
    }

    @Test
    fun tappingSixTimesHoldsOnTheLastTossBeforeTheReveal() {
        val vm = viewModel()
        repeat(6) {
            vm.onToss()
            scheduler.runCurrent()
            advance(DivinationViewModel.TOSS_ANIMATION_MILLIS)
        }
        assertHoldsThenReveals(vm)
    }

    @Test
    fun tossingTheRestHoldsOnTheLastTossBeforeTheReveal() {
        val vm = viewModel()
        vm.onTossRemaining()
        scheduler.runCurrent()
        var waited = 0L
        while (vm.state.value.tosses.size < 6) {
            assertTrue(waited < 60_000, "the sixth toss never landed")
            advance(1)
            waited++
        }
        assertHoldsThenReveals(vm)
    }
}
