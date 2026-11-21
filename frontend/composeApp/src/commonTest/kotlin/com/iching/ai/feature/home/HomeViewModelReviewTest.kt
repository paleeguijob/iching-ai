package com.iching.ai.feature.home

import com.iching.ai.ui.strings.thaiLanguageHolder
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.PosterLink
import com.iching.ai.domain.model.PosterStatus
import com.iching.ai.domain.model.StreakState
import com.iching.ai.domain.oracle.bangkokEpochDay
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.usecase.GetDailyHexagramUseCase
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import com.iching.ai.feature.poster.PosterImages
import com.iching.ai.platform.NativeBridges
import com.iching.ai.session.AppSession
import com.iching.ai.session.CountingSettings
import com.iching.ai.session.FakeAds
import com.iching.ai.session.FakeApi
import com.iching.ai.session.FakeAuth
import com.iching.ai.session.FakeJournal
import com.iching.ai.session.FakePurchases
import com.iching.ai.session.FakeReview
import com.iching.ai.session.FirebaseApiSession
import com.iching.ai.session.LoggedAnalytics
import com.iching.ai.session.NoNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private object ReviewHexagram : HexagramRepository {
    private val hexagram = Hexagram(
        id = 1, nameCn = "", pinyin = "", nameTh = "", pattern = "111111",
        keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
    )
    override suspend fun all() = listOf(hexagram)
    override suspend fun byId(id: Int) = hexagram
    override suspend fun byPattern(pattern: String) = hexagram
}

private object ReadyImages : PosterImages {
    override suspend fun preload(url: String) = true
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelReviewTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher)
    private val api = FakeApi()
    private val analytics = LoggedAnalytics()
    private val review = FakeReview()
    private val now = 1795194000000L - 86_400_000
    private val today = bangkokEpochDay(now)
    /** One reading short of asking, installed long enough ago. */
    private val settings = CountingSettings(AppSettings(completedReadings = 2, firstOpenEpochDay = today - 5))
    private lateinit var session: AppSession

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun viewModel(status: AccountStatus = AccountStatus.UNKNOWN_FREE): HomeViewModel {
        api.statusNow = status
        val auth = FakeAuth()
        session = AppSession(
            NativeBridges(auth, FakePurchases(), FakeAds(), analytics, NoNotifications, review),
            FirebaseApiSession(auth), api, FakeJournal(), settings, scope, nowMillis = { now },
        )
        session.start()
        val vm = HomeViewModel(
            GetDailyHexagramUseCase(ReviewHexagram), ObserveSettingsUseCase(settings), session,
            UpdateSettingsUseCase(settings), ReadyImages, thaiLanguageHolder(), nowMillis = { now },
        )
        scheduler.advanceUntilIdle()
        return vm
    }

    private fun finishReading() {
        scope.launch { session.onReadingCompleted() }
        scheduler.advanceUntilIdle()
    }

    private fun HomeViewModel.resume() {
        onResumed()
        scheduler.advanceUntilIdle()
    }

    @Test
    fun asksAfterReturningFromAFinishedReading() {
        val vm = viewModel()
        finishReading()
        vm.resume()
        assertEquals(1, review.requests)
        assertEquals(today, settings.state.value.lastReviewRequestEpochDay)
        assertEquals(1, analytics.events.count { it.first == "review_prompt_requested" })
    }

    @Test
    fun doesNotAskOnColdLaunch() {
        settings.state.value = settings.state.value.copy(completedReadings = 5)
        viewModel().resume()
        assertEquals(0, review.requests)
    }

    @Test
    fun doesNotAskWhenAPosterShowedThisRun() {
        val poster = PosterStatus("cny", "https://w/p.webp", "ตรุษจีน", true, PosterLink.PAYWALL, null, 1795194000000L)
        settings.state.value = settings.state.value.copy(streak = StreakState(current = 1, lastEpochDay = today))
        val vm = viewModel(AccountStatus.UNKNOWN_FREE.copy(poster = poster))
        vm.onPopupDisplayed("cny")
        vm.onPopupClosed()
        finishReading()
        vm.resume()
        assertEquals(0, review.requests)
    }

    @Test
    fun aFailedAskKeepsTheCooldownFree() {
        review.error = "no foreground activity"
        val vm = viewModel()
        finishReading()
        vm.resume()
        assertEquals(1, review.requests)
        assertEquals(null, settings.state.value.lastReviewRequestEpochDay)
        assertEquals(0, analytics.events.count { it.first == "review_prompt_requested" })
    }

    @Test
    fun asksAtMostOncePerCooldown() {
        val vm = viewModel()
        finishReading()
        vm.resume()
        finishReading()
        vm.resume()
        assertEquals(1, review.requests)
    }
}
