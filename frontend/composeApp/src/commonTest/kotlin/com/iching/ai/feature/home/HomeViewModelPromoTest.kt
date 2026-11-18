package com.iching.ai.feature.home

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.PromoStatus
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.domain.usecase.GetDailyHexagramUseCase
import com.iching.ai.domain.usecase.GetHexagramUseCase
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private object PromoHexagram : HexagramRepository {
    private val hexagram = Hexagram(
        id = 1, nameCn = "", pinyin = "", nameTh = "", pattern = "111111",
        keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
    )
    override suspend fun all() = listOf(hexagram)
    override suspend fun byId(id: Int) = hexagram
    override suspend fun byPattern(pattern: String) = hexagram
}

private class PromoSettings : SettingsRepository {
    val flow = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = flow
    override suspend fun update(transform: (AppSettings) -> AppSettings) = flow.update(transform)
    override suspend fun clear() {
        flow.value = AppSettings()
    }
}

private const val ENDS_AT = 1795194000000L
private val WITH_PROMO = AccountStatus.UNKNOWN_FREE.copy(
    promo = PromoStatus("1111-2026", "โปรพิเศษ", "Premium รายปี ลด ๔๐%", "ดูโปร", ENDS_AT, showEndDate = true),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelPromoTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher)
    private val api = FakeApi()
    private val analytics = LoggedAnalytics()
    private val settings = PromoSettings()
    private var now = ENDS_AT - 86_400_000

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun viewModel(repo: SettingsRepository = settings): HomeViewModel {
        api.statusNow = WITH_PROMO
        val auth = FakeAuth()
        val session = AppSession(
            NativeBridges(auth, FakePurchases(), FakeAds(), analytics, NoNotifications),
            FirebaseApiSession(auth),
            api,
            FakeJournal(),
            repo,
            scope,
        )
        session.start()
        val vm = HomeViewModel(
            GetDailyHexagramUseCase(PromoHexagram), ObserveSettingsUseCase(repo), session, api,
            GetHexagramUseCase(PromoHexagram), UpdateSettingsUseCase(repo), nowMillis = { now },
        )
        vm.promoCard.launchIn(scope)
        scheduler.advanceUntilIdle()
        return vm
    }

    @Test
    fun dismissedPromoNeverFlashesWhileSettingsLoad() {
        // Settings that have not loaded yet, then arrive saying this promo was closed in an earlier launch.
        val stored = MutableStateFlow<AppSettings?>(null)
        val late = object : SettingsRepository {
            override val settings: Flow<AppSettings> = stored.filterNotNull()
            override suspend fun update(transform: (AppSettings) -> AppSettings) = stored.update { transform(it ?: AppSettings()) }
            override suspend fun clear() {
                stored.value = AppSettings()
            }
        }
        val vm = viewModel(late)
        val seen = mutableListOf<PromoCard?>()
        vm.promoCard.onEach { seen += it }.launchIn(scope)
        scheduler.advanceUntilIdle()
        stored.value = AppSettings(dismissedPromoIds = listOf("1111-2026"))
        scheduler.advanceUntilIdle()
        assertEquals(emptyList(), seen.filterNotNull())
    }

    private fun logged(event: String) = analytics.events.filter { it.first == event }.map { it.second }

    @Test
    fun showsThePromoFromStatus() = assertEquals("1111-2026", viewModel().promoCard.value?.id)

    @Test
    fun dismissHidesAndRemembers() {
        val vm = viewModel()
        vm.onPromoDismissed("1111-2026")
        scheduler.advanceUntilIdle()
        assertNull(vm.promoCard.value)
        assertEquals(listOf("1111-2026"), settings.flow.value.dismissedPromoIds)
        assertEquals(listOf(mapOf("promo_id" to "1111-2026")), logged("promo_dismissed"))
    }

    @Test
    fun shownIsLoggedOnce() {
        val vm = viewModel()
        vm.onPromoShown("1111-2026")
        vm.onPromoShown("1111-2026")
        assertEquals(listOf(mapOf("promo_id" to "1111-2026")), logged("promo_shown"))
    }

    @Test
    fun tapIsLogged() {
        viewModel().onPromoTapped("1111-2026")
        assertEquals(listOf(mapOf("promo_id" to "1111-2026")), logged("promo_tapped"))
    }

    @Test
    fun cardDisappearsAfterEndOnResume() {
        val vm = viewModel()
        now = ENDS_AT
        vm.onResumed()
        scheduler.advanceUntilIdle()
        assertNull(vm.promoCard.value)
    }
}
