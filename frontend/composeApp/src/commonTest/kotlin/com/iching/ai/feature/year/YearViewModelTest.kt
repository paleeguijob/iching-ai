package com.iching.ai.feature.year

import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.CampaignStatus
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.YearHexagram
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.domain.usecase.GetHexagramUseCase
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.feature.home.CampaignCard
import com.iching.ai.feature.home.campaignCard
import com.iching.ai.session.FakeApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private fun hexagram(id: Int) = Hexagram(
    id = id, nameCn = "$id", pinyin = "", nameTh = "ชื่อ$id — ความหมาย", pattern = "111111",
    keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
)

private object Hexagrams : HexagramRepository {
    private val all = (1..64).map(::hexagram)
    override suspend fun all() = all
    override suspend fun byId(id: Int) = all[id - 1]
    override suspend fun byPattern(pattern: String) = all.first()
}

private object Settings : SettingsRepository {
    override val settings: Flow<AppSettings> = MutableStateFlow(AppSettings())
    override suspend fun update(transform: (AppSettings) -> AppSettings) = Unit
    override suspend fun clear() = Unit
}

private val SERVER_RESULT = YearHexagram(2027, 11, listOf(7, 8, 9, 6, 7, 8), List(6) { listOf(1, 1, 0) })
private val CAMPAIGN = CampaignStatus("cny-2027", 2027, active = true, endsAtMillis = 0)

@OptIn(ExperimentalCoroutinesApi::class)
class YearViewModelTest {
    private val api = FakeApi()
    private val events = mutableListOf<Pair<String, Map<String, String>>>()
    private var streakSyncs = 0

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = YearViewModel(
        api = api,
        getHexagram = GetHexagramUseCase(Hexagrams),
        observeSettings = ObserveSettingsUseCase(Settings),
        log = { name, params -> events += name to params },
        campaignId = { CAMPAIGN.id },
        syncStreak = { streakSyncs++ },
    )

    private fun YearViewModel.tossSix() = repeat(6) { onToss() }

    @Test
    fun existingResultSkipsTheRitual() {
        api.yearHexagramNow = SERVER_RESULT
        val vm = viewModel()
        val ui = assertIs<YearUi.Result>(vm.state.value)
        assertEquals(11, ui.hexagram.id)
        assertEquals(SERVER_RESULT.lines, ui.lines)
        assertEquals(2027, ui.year)
        assertEquals(false, ui.firstTime)
        assertEquals(0, api.castYearCalls)
        assertEquals(0, streakSyncs)
    }

    @Test
    fun noResultYetStartsTheRitual() {
        assertEquals(YearUi.Ritual(0), viewModel().state.value)
    }

    @Test
    fun ritualCallsTheServerOnceAfterSixTossesAndShowsTheResultWithFirstTime() {
        api.castYearResult = SERVER_RESULT
        val vm = viewModel()
        repeat(5) { vm.onToss() }
        assertEquals(YearUi.Ritual(5), vm.state.value)
        assertEquals(0, api.castYearCalls)

        vm.onToss()
        vm.onToss() // a stray extra tap changes nothing
        assertEquals(1, api.castYearCalls)
        val ui = assertIs<YearUi.Result>(vm.state.value)
        assertEquals(11, ui.hexagram.id)
        assertEquals(true, ui.firstTime)
        assertEquals(1, streakSyncs) // the server granted the theme; the streak sync brings it here

        vm.onThemeNoticeShown()
        assertEquals(false, assertIs<YearUi.Result>(vm.state.value).firstTime)
    }

    @Test
    fun networkFailureShowsOfflineAndRetryWorks() {
        api.yearFailure = FailureReason.NETWORK
        val vm = viewModel()
        assertEquals(YearUi.Offline, vm.state.value)

        api.yearFailure = null
        vm.retry()
        assertEquals(YearUi.Ritual(0), vm.state.value)
    }

    @Test
    fun losingTheConnectionOnTheCastRetriesTheCastNotTheTosses() {
        api.castYearResult = SERVER_RESULT
        api.castYearFailure = FailureReason.NETWORK
        val vm = viewModel()
        vm.tossSix()
        assertEquals(YearUi.Offline, vm.state.value)

        api.castYearFailure = null
        vm.retry()
        assertIs<YearUi.Result>(vm.state.value)
        assertEquals(2, api.castYearCalls)
    }

    @Test
    fun campaignInactiveOnTheCastShowsInactive() {
        api.castYearFailure = FailureReason.CAMPAIGN_INACTIVE
        val vm = viewModel()
        vm.tossSix()
        assertEquals(YearUi.Inactive, vm.state.value)
    }

    @Test
    fun logsYearCastWithHexagramIdAndYearView() {
        api.castYearResult = SERVER_RESULT
        val vm = viewModel()
        vm.tossSix()
        assertEquals(
            listOf(
                "year_cast" to mapOf("campaign_id" to "cny-2027", "hexagram_id" to "11"),
                "year_view" to mapOf("campaign_id" to "cny-2027"),
            ),
            events,
        )
    }

    @Test
    fun openingAnExistingResultLogsYearViewOnly() {
        api.yearHexagramNow = SERVER_RESULT
        viewModel()
        assertEquals(listOf("year_view" to mapOf("campaign_id" to "cny-2027")), events)
    }
}

class CampaignCardTest {
    private val hexagram = hexagram(11)

    @Test
    fun hiddenWithoutACampaign() {
        assertEquals(CampaignCard.Hidden, campaignCard(null, null, null))
    }

    @Test
    fun hiddenWhenTheCampaignIsOverAndThereIsNoResult() {
        assertEquals(CampaignCard.Hidden, campaignCard(CAMPAIGN.copy(active = false), null, null))
    }

    @Test
    fun hiddenOnceTheCampaignIsOverEvenWithAResult() {
        assertEquals(CampaignCard.Hidden, campaignCard(CAMPAIGN.copy(active = false), SERVER_RESULT, hexagram))
    }

    @Test
    fun newWhileActiveAndNothingCast() {
        assertEquals(CampaignCard.New(2027), campaignCard(CAMPAIGN, null, null))
    }

    @Test
    fun castWhenAResultExists() {
        assertEquals(CampaignCard.Cast(2027, hexagram), campaignCard(CAMPAIGN, SERVER_RESULT, hexagram))
    }

    @Test
    fun aResultWhoseHexagramIsNotLoadedYetShowsAsNew() {
        assertEquals(CampaignCard.New(2027), campaignCard(CAMPAIGN, SERVER_RESULT, null))
    }
}
