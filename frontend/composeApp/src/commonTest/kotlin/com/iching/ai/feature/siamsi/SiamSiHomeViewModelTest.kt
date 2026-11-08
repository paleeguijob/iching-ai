package com.iching.ai.feature.siamsi

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.SiamSiStatus
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.domain.repository.SiamSiRepository
import com.iching.ai.domain.siamsi.DivinationMode
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiSetRef
import com.iching.ai.domain.siamsi.SiamSiUsage
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import com.iching.ai.navigation.SiamSiRoute
import com.iching.ai.platform.NativeAds
import com.iching.ai.platform.ResultCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

private class HomeSettings(initial: AppSettings) : SettingsRepository {
    val flow = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = flow
    override suspend fun update(transform: (AppSettings) -> AppSettings) = flow.update(transform)
    override suspend fun clear() {
        flow.value = AppSettings()
    }
}

private class AnswerAds(val answer: String) : NativeAds {
    override fun showRewarded(callback: ResultCallback) = callback.onResult(answer, null)
    override fun preload() = Unit
}

private fun sampleSet(id: String) = SiamSiSet(id, id, 1, "t", "n", emptyList())

/** Hands out the bundled sets the server lists, in its order. */
private object Sets : SiamSiRepository {
    private val bundled = listOf("guanyin100", "jiazi60")
    override suspend fun available(refs: List<SiamSiSetRef>) = refs.map { it.id }.filter { it in bundled }.map(::sampleSet)
    override suspend fun set(id: String) = if (id in bundled) sampleSet(id) else null
}

private val BOTH = listOf(SiamSiSetRef("guanyin100", 1), SiamSiSetRef("jiazi60", 1))
private fun statusWith(enabled: Boolean, sets: List<SiamSiSetRef> = BOTH) =
    AccountStatus.UNKNOWN_FREE.copy(siamsi = SiamSiStatus(enabled = enabled, sets = sets, drawsPerDay = 1))

@OptIn(ExperimentalCoroutinesApi::class)
class SiamSiHomeViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher)
    private val today = 20_400L
    private val events = mutableListOf<Pair<String, Map<String, String>>>()
    private val navigation = mutableListOf<SiamSiNav>()
    private lateinit var settings: HomeSettings

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun viewModel(
        status: AccountStatus = statusWith(enabled = true),
        stored: AppSettings = AppSettings(),
        online: Boolean = true,
        ad: String = "rewarded",
    ): SiamSiHomeViewModel {
        settings = HomeSettings(stored)
        val statusFlow = MutableStateFlow(status)
        val onlineFlow = MutableStateFlow(online)
        val log = { name: String, params: Map<String, String> -> events += name to params }
        val gate = SiamSiDrawGate(settings, AnswerAds(ad), statusFlow, onlineFlow, log, { today })
        val vm = SiamSiHomeViewModel(ObserveSettingsUseCase(settings), UpdateSettingsUseCase(settings), Sets, gate, statusFlow, onlineFlow, log)
        listOf(vm.mode, vm.siamsiSets, vm.selectedSet, vm.drawGate, vm.enabled).forEach { it.launchIn(scope) }
        vm.navigation.onEach { navigation += it }.launchIn(scope)
        scheduler.advanceUntilIdle()
        return vm
    }

    @Test
    fun disabledForcesIChingWithoutForgettingTheChoice() {
        val vm = viewModel(status = statusWith(enabled = false), stored = AppSettings(divinationMode = DivinationMode.SIAMSI))
        assertEquals(false, vm.enabled.value)
        assertEquals(DivinationMode.ICHING, vm.mode.value)
        assertEquals(DivinationMode.SIAMSI, settings.flow.value.divinationMode)
    }

    @Test
    fun enabledWithNoSetsStaysHidden() {
        val vm = viewModel(status = statusWith(enabled = true, sets = emptyList()), stored = AppSettings(divinationMode = DivinationMode.SIAMSI))
        assertEquals(false, vm.enabled.value)
        assertEquals(DivinationMode.ICHING, vm.mode.value)
    }

    @Test
    fun remembersTheModeAcrossLaunches() {
        val vm = viewModel(stored = AppSettings(divinationMode = DivinationMode.SIAMSI))
        assertEquals(DivinationMode.SIAMSI, vm.mode.value)
        vm.onModeSelected(DivinationMode.ICHING)
        scheduler.advanceUntilIdle()
        assertEquals(DivinationMode.ICHING, settings.flow.value.divinationMode)
        assertEquals("siamsi_mode_selected", events.last().first)
    }

    @Test
    fun aRemovedSetFallsBackToTheFirst() {
        val vm = viewModel(stored = AppSettings(siamsiSetId = "thai28"))
        assertEquals("guanyin100", vm.selectedSet.value?.id)
        vm.onSetSelected("jiazi60")
        scheduler.advanceUntilIdle()
        assertEquals("jiazi60", vm.selectedSet.value?.id)
        assertEquals("jiazi60", settings.flow.value.siamsiSetId)
        assertEquals("siamsi_set_selected" to mapOf("set" to "jiazi60"), events.last())
    }

    @Test
    fun startAfterTheAdOpensTheShake() {
        val vm = viewModel()
        vm.onSiamSiStart("งานใหม่", QuestionCategory.entries.first())
        scheduler.advanceUntilIdle()
        assertEquals(listOf<SiamSiNav>(SiamSiNav.Draw(SiamSiRoute("guanyin100", "งานใหม่", QuestionCategory.entries.first().wire))), navigation)
    }

    @Test
    fun startWithNothingLeftOpensThePaywall() {
        val vm = viewModel(stored = AppSettings(siamsiUsage = SiamSiUsage(today, used = 1)))
        vm.onSiamSiStart("", null)
        scheduler.advanceUntilIdle()
        assertEquals(listOf<SiamSiNav>(SiamSiNav.Paywall), navigation)
    }

    @Test
    fun aDismissedAdStaysAndSaysWhy() {
        val vm = viewModel(ad = "dismissed")
        vm.onSiamSiStart("", null)
        scheduler.advanceUntilIdle()
        assertEquals(emptyList(), navigation)
        assertEquals("ต้องดูโฆษณาจนจบจึงจะเสี่ยงได้", vm.drawGate.value.line)
    }

    @Test
    fun offlineAtLaunchKeepsSiamSiIfItWasOnBefore() {
        val vm = viewModel(
            status = AccountStatus.UNKNOWN_FREE,
            online = false,
            stored = AppSettings(divinationMode = DivinationMode.SIAMSI, siamsiLastEnabled = true),
        )
        assertEquals(true, vm.enabled.value)
        assertEquals(DivinationMode.SIAMSI, vm.mode.value)
        assertEquals(listOf("guanyin100", "jiazi60"), vm.siamsiSets.value.map { it.id })
    }

    @Test
    fun theServerSwitchIsRememberedForOfflineLaunches() {
        viewModel(stored = AppSettings())
        assertEquals(true, settings.flow.value.siamsiLastEnabled)
        viewModel(status = statusWith(enabled = false), stored = AppSettings(siamsiLastEnabled = true))
        assertEquals(false, settings.flow.value.siamsiLastEnabled)
    }
}
