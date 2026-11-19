package com.iching.ai.feature.home

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.PosterLink
import com.iching.ai.domain.model.PosterStatus
import com.iching.ai.domain.model.StreakState
import com.iching.ai.domain.oracle.bangkokEpochDay
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.domain.usecase.GetDailyHexagramUseCase
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import com.iching.ai.feature.poster.PosterImages
import com.iching.ai.feature.poster.PosterPopup
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

private object PosterHexagram : HexagramRepository {
    private val hexagram = Hexagram(
        id = 1, nameCn = "", pinyin = "", nameTh = "", pattern = "111111",
        keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
    )
    override suspend fun all() = listOf(hexagram)
    override suspend fun byId(id: Int) = hexagram
    override suspend fun byPattern(pattern: String) = hexagram
}

private class PosterHomeSettings(initial: AppSettings) : SettingsRepository {
    val flow = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = flow
    override suspend fun update(transform: (AppSettings) -> AppSettings) = flow.update(transform)
    override suspend fun clear() {
        flow.value = AppSettings()
    }
}

/** Preloads succeed per URL as [ok] says at the time of the call. */
private class FakeImages : PosterImages {
    var ok = true
    val calls = mutableListOf<String>()
    override suspend fun preload(url: String): Boolean {
        calls += url
        return ok
    }
}

private const val ENDS_AT = 1795194000000L
private const val URL = "https://w/content/posters/cny.webp"
private val WITH_POSTER = AccountStatus.UNKNOWN_FREE.copy(
    poster = PosterStatus("cny", URL, "ตรุษจีน", true, PosterLink.PAYWALL, null, ENDS_AT),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelPosterTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher)
    private val api = FakeApi()
    private val analytics = LoggedAnalytics()
    private val now = ENDS_AT - 86_400_000
    private val today = bangkokEpochDay(now)
    private val readToday = AppSettings(firstOpenEpochDay = today - 2, streak = StreakState(current = 1, lastEpochDay = today))
    private val settings = PosterHomeSettings(readToday)
    private val images = FakeImages()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun viewModel(repo: SettingsRepository = settings): HomeViewModel {
        api.statusNow = WITH_POSTER
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
            GetDailyHexagramUseCase(PosterHexagram), ObserveSettingsUseCase(repo), session,
            UpdateSettingsUseCase(repo), images, nowMillis = { now },
        )
        scheduler.advanceUntilIdle()
        return vm
    }

    private fun logged(event: String) = analytics.events.filter { it.first == event }.map { it.second }
    private val cny = mapOf("poster_id" to "cny", "surface" to "popup")

    @Test
    fun recordsFirstOpenOnce() {
        settings.flow.value = AppSettings()
        viewModel()
        assertEquals(today, settings.flow.value.firstOpenEpochDay)
        viewModel()
        assertEquals(today, settings.flow.value.firstOpenEpochDay)
    }

    @Test
    fun installDayNeverPops() {
        settings.flow.value = AppSettings(streak = readToday.streak)
        assertNull(viewModel().popup.value)
    }

    @Test
    fun heldPopupIsNotSeenUntilDisplayed() {
        // Home's view model lives on under the daily page (e.g. opened from the push): holding the popup there must
        // not spend it, or closing the app from that page loses the poster without the seeker ever seeing it.
        val vm = viewModel()
        assertEquals("cny", vm.popup.value?.id)
        assertEquals(emptyList(), logged("poster_shown"))
        assertEquals(emptyList(), settings.flow.value.seenPosterIds)
    }

    @Test
    fun popsAfterDailyReadAndImageLoads() {
        val vm = viewModel()
        assertEquals(PosterPopup("cny", URL, "ตรุษจีน", PosterLink.PAYWALL), vm.popup.value)
        vm.onPopupDisplayed("cny")
        vm.onPopupDisplayed("cny")
        scheduler.advanceUntilIdle()
        assertEquals(listOf(cny), logged("poster_shown"))
        assertEquals(listOf("cny"), settings.flow.value.seenPosterIds)
    }

    @Test
    fun popupStaysUntilClosedAfterSeenIsSaved() {
        val vm = viewModel()
        vm.onPopupDisplayed("cny")
        vm.onResumed()
        scheduler.advanceUntilIdle()
        assertEquals("cny", vm.popup.value?.id)
        assertEquals(1, logged("poster_shown").size)
    }

    @Test
    fun closeLogsAndClears() {
        val vm = viewModel()
        vm.onPopupClosed()
        scheduler.advanceUntilIdle()
        assertNull(vm.popup.value)
        assertEquals(listOf(cny), logged("poster_closed"))
        assertEquals(emptyList(), logged("poster_tapped"))
    }

    @Test
    fun tapReturnsLinkLogsAndClears() {
        val vm = viewModel()
        assertEquals(PosterLink.PAYWALL, vm.onPopupTapped())
        scheduler.advanceUntilIdle()
        assertNull(vm.popup.value)
        assertEquals(listOf(cny), logged("poster_tapped"))
        assertEquals(emptyList(), logged("poster_closed"))
    }

    @Test
    fun seenPosterNeverPopsWhileSettingsLoad() {
        // Settings that have not loaded yet, then arrive saying this poster already popped in an earlier launch.
        val stored = MutableStateFlow<AppSettings?>(null)
        val late = object : SettingsRepository {
            override val settings: Flow<AppSettings> = stored.filterNotNull()
            override suspend fun update(transform: (AppSettings) -> AppSettings) = stored.update { transform(it ?: AppSettings()) }
            override suspend fun clear() {
                stored.value = AppSettings()
            }
        }
        val vm = viewModel(late)
        val seen = mutableListOf<PosterPopup?>()
        vm.popup.onEach { seen += it }.launchIn(scope)
        scheduler.advanceUntilIdle()
        stored.value = readToday.copy(seenPosterIds = listOf("cny"))
        scheduler.advanceUntilIdle()
        assertEquals(emptyList(), seen.filterNotNull())
        assertEquals(emptyList(), logged("poster_shown"))
    }

    @Test
    fun failedImageNeverPopsThenRetries() {
        images.ok = false
        val vm = viewModel()
        assertNull(vm.popup.value)
        images.ok = true
        vm.onResumed()
        scheduler.advanceUntilIdle()
        assertEquals("cny", vm.popup.value?.id)
    }

    @Test
    fun seenPosterDoesNotPopInNewViewModel() {
        viewModel().run {
            onPopupDisplayed("cny")
            onPopupClosed()
        }
        scheduler.advanceUntilIdle()
        assertNull(viewModel().popup.value)
        assertEquals(1, logged("poster_shown").size)
    }
}
