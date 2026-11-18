package com.iching.ai.feature.daily

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.PosterCardText
import com.iching.ai.domain.model.PosterLink
import com.iching.ai.domain.model.PosterStatus
import com.iching.ai.domain.model.withPosterCardDismissed
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.domain.usecase.ClaimPushPromptUseCase
import com.iching.ai.domain.usecase.GetDailyHexagramUseCase
import com.iching.ai.domain.usecase.RecordDailyViewUseCase
import com.iching.ai.feature.poster.PosterCard
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

private object PosterDailyHexagrams : HexagramRepository {
    private val hexagram = Hexagram(
        id = 1, nameCn = "", pinyin = "", nameTh = "", pattern = "111111",
        keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
    )
    override suspend fun all() = listOf(hexagram)
    override suspend fun byId(id: Int) = hexagram
    override suspend fun byPattern(pattern: String) = hexagram
}

private class PosterDailySettings : SettingsRepository {
    val flow = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = flow
    override suspend fun update(transform: (AppSettings) -> AppSettings) = flow.update(transform)
    override suspend fun clear() {
        flow.value = AppSettings()
    }
}

private const val ENDS_AT = 1795194000000L
private val WITH_CARD = AccountStatus.UNKNOWN_FREE.copy(
    poster = PosterStatus(
        "cny", "https://w/content/posters/cny.webp", "ตรุษจีน", true, PosterLink.PAYWALL,
        PosterCardText("โปรพิเศษ", "Premium รายปี ลด ๔๐%", "ดูโปร", true), ENDS_AT,
    ),
)

@OptIn(ExperimentalCoroutinesApi::class)
class DailyViewModelPosterTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher)
    private val settings = PosterDailySettings()
    private val events = mutableListOf<Pair<String, Map<String, String>>>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun viewModel(stored: Flow<AppSettings> = settings.flow): DailyViewModel {
        val vm = DailyViewModel(
            getDaily = GetDailyHexagramUseCase(PosterDailyHexagrams),
            recordView = RecordDailyViewUseCase(settings),
            claimPushPrompt = ClaimPushPromptUseCase(settings),
            log = { name, params -> events += name to params },
            pushSupported = { false },
            enablePush = {},
            now = { ENDS_AT - 86_400_000 },
            status = MutableStateFlow(WITH_CARD),
            settings = stored,
            dismissPosterCard = { id -> settings.update { it.withPosterCardDismissed(id) } },
        )
        vm.posterCard.launchIn(scope)
        scheduler.advanceUntilIdle()
        return vm
    }

    private fun logged(event: String) = events.filter { it.first == event }.map { it.second }
    private val cny = mapOf("poster_id" to "cny", "surface" to "card")

    @Test
    fun showsCardFromStatus() = assertEquals("cny", viewModel().posterCard.value?.id)

    @Test
    fun closeHidesAndRemembers() {
        val vm = viewModel()
        vm.onPosterCardClosed("cny")
        scheduler.advanceUntilIdle()
        assertNull(vm.posterCard.value)
        assertEquals(listOf("cny"), settings.flow.value.dismissedPosterCardIds)
        assertEquals(listOf(cny), logged("poster_closed"))
    }

    @Test
    fun shownIsLoggedOnce() {
        val vm = viewModel()
        vm.onPosterCardShown("cny")
        vm.onPosterCardShown("cny")
        assertEquals(listOf(cny), logged("poster_shown"))
    }

    @Test
    fun tapIsLogged() {
        viewModel().onPosterCardTapped("cny")
        assertEquals(listOf(cny), logged("poster_tapped"))
    }

    @Test
    fun closedCardNeverFlashesWhileSettingsLoad() {
        // Settings that have not loaded yet, then arrive saying this card was closed in an earlier launch.
        val stored = MutableStateFlow<AppSettings?>(null)
        val vm = viewModel(stored.filterNotNull())
        val seen = mutableListOf<PosterCard?>()
        vm.posterCard.onEach { seen += it }.launchIn(scope)
        scheduler.advanceUntilIdle()
        stored.value = AppSettings(dismissedPosterCardIds = listOf("cny"))
        scheduler.advanceUntilIdle()
        assertEquals(emptyList(), seen.filterNotNull())
    }
}
