package com.iching.ai.feature.home

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.CampaignStatus
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.YearHexagram
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.usecase.GetDailyHexagramUseCase
import com.iching.ai.domain.usecase.GetHexagramUseCase
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private object Hexagrams : HexagramRepository {
    private val hexagram = Hexagram(
        id = 11, nameCn = "", pinyin = "", nameTh = "", pattern = "111111",
        keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
    )
    override suspend fun all() = listOf(hexagram)
    override suspend fun byId(id: Int) = hexagram
    override suspend fun byPattern(pattern: String) = hexagram
}

private val ACTIVE = AccountStatus.UNKNOWN_FREE.copy(campaign = CampaignStatus("cny-2027", 2027, active = true, endsAtMillis = 0))
private val STORED = YearHexagram(2027, 11, listOf(7, 8, 7, 8, 7, 8), emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelCampaignTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher)
    private val api = FakeApi()
    private val analytics = LoggedAnalytics()
    private val seen = mutableListOf<CampaignCard>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    /** Observes the card the way the screen does, logging each card as it is shown. */
    private lateinit var session: AppSession

    private fun viewModel(status: AccountStatus = ACTIVE): HomeViewModel {
        api.statusNow = status
        val auth = FakeAuth()
        session = AppSession(
            NativeBridges(auth, FakePurchases(), FakeAds(), analytics, NoNotifications),
            FirebaseApiSession(auth),
            api,
            FakeJournal(),
            NoSettings,
            scope,
        )
        session.start()
        val vm = HomeViewModel(GetDailyHexagramUseCase(Hexagrams), ObserveSettingsUseCase(NoSettings), session, api, GetHexagramUseCase(Hexagrams))
        vm.campaignCard.onEach {
            seen += it
            vm.onCampaignCardShown(it)
        }.launchIn(scope)
        scheduler.advanceUntilIdle()
        return vm
    }

    private fun bannerStates() = analytics.events.filter { it.first == "campaign_banner_shown" }.map { it.second["state"] }

    @Test
    fun aUserWithAStoredResultNeverSeesNew() {
        api.yearHexagramNow = STORED
        val vm = viewModel()
        assertIs<CampaignCard.Cast>(vm.campaignCard.value)
        assertTrue(seen.none { it is CampaignCard.New }, "the card must not flash 'new' before the lookup answers")
        assertEquals(listOf<String?>("cast"), bannerStates())
    }

    @Test
    fun aUserWithoutOneSeesNewAfterTheLookupAnswersNull() {
        api.yearHexagramNow = null
        val vm = viewModel()
        assertEquals(CampaignCard.New(2027), vm.campaignCard.value)
        assertEquals(listOf<String?>("new"), bannerStates())
    }

    @Test
    fun theCardStaysHiddenWhileTheLookupFails() {
        api.yearFailure = FailureReason.NETWORK
        val vm = viewModel()
        assertEquals(CampaignCard.Hidden, vm.campaignCard.value)
        assertEquals(emptyList(), bannerStates())
    }

    @Test
    fun noYearLookupWhileTheCampaignIsInactiveAndOneOnceItTurnsActive() {
        val inactive = ACTIVE.copy(campaign = ACTIVE.campaign!!.copy(active = false))
        api.yearHexagramNow = STORED
        val vm = viewModel(inactive)
        assertEquals(0, api.yearHexagramCalls)
        assertEquals(CampaignCard.Hidden, vm.campaignCard.value)

        api.statusNow = ACTIVE
        vm.onResumed()
        scope.launch { session.refreshStatus() }
        scheduler.advanceUntilIdle()
        assertEquals(1, api.yearHexagramCalls)
        assertIs<CampaignCard.Cast>(vm.campaignCard.value)
    }
}
