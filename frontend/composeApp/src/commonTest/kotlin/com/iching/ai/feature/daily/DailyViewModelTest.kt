package com.iching.ai.feature.daily

import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.StreakState
import com.iching.ai.domain.oracle.dailyHexagramId
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.domain.usecase.ClaimPushPromptUseCase
import com.iching.ai.domain.usecase.GetDailyHexagramUseCase
import com.iching.ai.domain.usecase.RecordDailyViewUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val DAY_MS = 86_400_000L

private class MemorySettings(initial: AppSettings = AppSettings()) : SettingsRepository {
    val flow = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = flow
    override suspend fun update(transform: (AppSettings) -> AppSettings) = flow.update(transform)
    override suspend fun clear() {
        flow.value = AppSettings()
    }
}

private object FakeHexagrams : HexagramRepository {
    private val all = (1..64).map { id ->
        Hexagram(
            id = id, nameCn = "$id", pinyin = "", nameTh = "$id", pattern = id.toString(2).padStart(6, '0'),
            keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
            dailyMessage = "message $id",
        )
    }
    override suspend fun all() = all
    override suspend fun byId(id: Int) = all[id - 1]
    override suspend fun byPattern(pattern: String) = all.first { it.pattern == pattern }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DailyViewModelTest {
    private val day = 20_000L
    private var now = day * DAY_MS
    private val events = mutableListOf<Pair<String, Map<String, String>>>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        settings: SettingsRepository = MemorySettings(),
        pushSupported: Boolean = true,
        enablePush: suspend () -> Unit = {},
    ) = DailyViewModel(
        getDaily = GetDailyHexagramUseCase(FakeHexagrams),
        recordView = RecordDailyViewUseCase(settings),
        claimPushPrompt = ClaimPushPromptUseCase(settings),
        log = { name, params -> events += name to params },
        pushSupported = { pushSupported },
        enablePush = enablePush,
        now = { now },
    )

    @Test
    fun openingCountsOnceAndLogs() {
        val vm = viewModel()
        vm.refresh()
        assertEquals(1, vm.state.value.streak.current)
        assertEquals(1, events.count { it.first == "daily_view" })
    }

    @Test
    fun refreshAfterMidnightMovesToTheNewDay() {
        val vm = viewModel()
        now += DAY_MS
        vm.refresh()
        assertEquals(dailyHexagramId(day + 1), vm.state.value.hexagram?.id)
        assertEquals(2, vm.state.value.streak.current)
    }

    @Test
    fun pushPromptShowsOnlyOnce() {
        val settings = MemorySettings()
        val first = viewModel(settings)
        assertEquals(true, first.state.value.showPushPrompt)
        first.dismissPushPrompt()
        first.refresh()
        assertEquals(false, first.state.value.showPushPrompt)
        assertEquals(false, viewModel(settings).state.value.showPushPrompt)
    }

    @Test
    fun pushPromptIsHiddenWhenThePlatformCannotPush() {
        val settings = MemorySettings()
        assertEquals(false, viewModel(settings, pushSupported = false).state.value.showPushPrompt)
        assertEquals(false, settings.flow.value.pushPromptShown)
    }

    @Test
    fun acceptingThePromptEnablesPush() {
        var enabled = 0
        val vm = viewModel(enablePush = { enabled++ })
        vm.acceptPushPrompt()
        assertEquals(1, enabled)
        assertEquals(false, vm.state.value.showPushPrompt)
    }

    @Test
    fun milestoneLogsAndReportsUnlock() {
        val settings = MemorySettings(AppSettings(streak = StreakState(6, 6, day - 1)))
        val vm = viewModel(settings)
        assertEquals(listOf(AppTheme.BAMBOO), vm.state.value.newlyUnlocked)
        assertEquals(listOf("streak_milestone" to mapOf("days" to "7")), events.filter { it.first == "streak_milestone" })
        vm.unlockNoticeShown()
        vm.refresh()
        assertEquals(emptyList(), vm.state.value.newlyUnlocked)
    }
}
