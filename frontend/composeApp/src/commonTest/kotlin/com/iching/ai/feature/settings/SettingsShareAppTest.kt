package com.iching.ai.feature.settings

import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.ui.strings.thaiLanguageHolder
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import com.iching.ai.platform.NativeBridges
import com.iching.ai.session.AppSession
import com.iching.ai.session.CountingSettings
import com.iching.ai.session.FakeApi
import com.iching.ai.session.FakeAuth
import com.iching.ai.session.FakeJournal
import com.iching.ai.session.FakePurchases
import com.iching.ai.session.FakeReview
import com.iching.ai.session.FirebaseApiSession
import com.iching.ai.session.LoggedAnalytics
import com.iching.ai.session.NoAds
import com.iching.ai.session.NoNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsShareAppTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher)
    private val api = FakeApi()
    private val analytics = LoggedAnalytics()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun viewModel(review: FakeReview = FakeReview()): SettingsViewModel {
        val auth = FakeAuth()
        val settings = CountingSettings()
        val session = AppSession(
            NativeBridges(auth, FakePurchases(), NoAds, analytics, NoNotifications, review),
            FirebaseApiSession(auth), api, FakeJournal(), settings, scope,
        )
        val vm = SettingsViewModel(ObserveSettingsUseCase(settings), UpdateSettingsUseCase(settings), session, api, thaiLanguageHolder())
        scheduler.advanceUntilIdle()
        return vm
    }

    @Test
    fun shareTextCarriesTheWebStoreLink() {
        val text = viewModel().shareAppText
        assertTrue(text!!.endsWith("https://play/app"))
        assertFalse(text.contains("market://"))
    }

    @Test
    fun noWebStoreLinkHidesShareApp() {
        assertNull(viewModel(FakeReview(urls = emptyList())).shareAppText)
    }

    @Test
    fun loggingShareAppTap() {
        viewModel().onShareAppTapped()
        assertEquals(1, analytics.events.count { it.first == "share_app_tap" })
    }
}
