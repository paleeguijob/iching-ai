package com.iching.ai.feature.settings

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.needsLanguagePick
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import com.iching.ai.platform.NativeBridges
import com.iching.ai.session.AppSession
import com.iching.ai.session.CountingSettings
import com.iching.ai.session.FakeApi
import com.iching.ai.session.FakeAuth
import com.iching.ai.session.FakeJournal
import com.iching.ai.session.FakePurchases
import com.iching.ai.session.FirebaseApiSession
import com.iching.ai.session.LoggedAnalytics
import com.iching.ai.session.NoAds
import com.iching.ai.session.NoNotifications
import com.iching.ai.ui.strings.AppLanguageHolder
import com.iching.ai.ui.strings.effectiveEnglishEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class LanguageSettingsTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher)
    private val api = FakeApi()
    private val analytics = LoggedAnalytics()
    private val settings = CountingSettings()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun session(): AppSession {
        val auth = FakeAuth()
        return AppSession(
            NativeBridges(auth, FakePurchases(), NoAds, analytics, NoNotifications),
            FirebaseApiSession(auth), api, FakeJournal(), settings, scope,
        )
    }

    private fun holder(enabled: kotlinx.coroutines.flow.Flow<Boolean>) =
        AppLanguageHolder(ObserveSettingsUseCase(settings), enabled, scope)

    @Test
    fun settingLanguageSavesItAndLogsTheSource() {
        val session = session()
        val vm = SettingsViewModel(
            ObserveSettingsUseCase(settings), UpdateSettingsUseCase(settings), session, api,
            holder(MutableStateFlow(true)),
        )
        vm.setLanguage(AppLanguage.EN)
        scheduler.advanceUntilIdle()
        assertEquals(AppLanguage.EN, settings.state.value.language)
        assertTrue(analytics.events.contains("language_selected" to mapOf("lang" to "en", "source" to "settings")))
    }

    @Test
    fun serverSwitchingEnglishOffMakesEveryoneThaiAndOnBringsItBack() {
        settings.state.value = AppSettings(language = AppLanguage.EN, onboardingAccepted = true)
        val enabled = MutableStateFlow(true)
        val holder = holder(enabled)
        scheduler.advanceUntilIdle()
        assertEquals(AppLanguage.EN, holder.language.value)
        enabled.value = false
        scheduler.advanceUntilIdle()
        assertEquals(AppLanguage.TH, holder.language.value)
        assertFalse(holder.englishEnabled.value)
        enabled.value = true
        scheduler.advanceUntilIdle()
        assertEquals(AppLanguage.EN, holder.language.value)
    }

    @Test
    fun beforeAnyStatusTheLastKnownFlagDecides() {
        settings.state.value = AppSettings(language = AppLanguage.EN, onboardingAccepted = true, lastEnglishEnabled = false)
        val session = session()
        val holder = holder(effectiveEnglishEnabled(session.online, session.status, ObserveSettingsUseCase(settings)()))
        scheduler.advanceUntilIdle()
        // The status default says English is on, but the server has not answered yet.
        assertTrue(session.status.value.englishEnabled)
        assertEquals(AppLanguage.TH, holder.language.value)

        session.updateStatus(AccountStatus.UNKNOWN_FREE.copy(englishEnabled = true))
        scheduler.advanceUntilIdle()
        assertEquals(AppLanguage.EN, holder.language.value)
    }

    @Test
    fun eachStatusIsRememberedForTheNextLaunch() {
        val session = session()
        session.updateStatus(AccountStatus.UNKNOWN_FREE.copy(englishEnabled = false))
        scheduler.advanceUntilIdle()
        assertFalse(settings.state.value.lastEnglishEnabled)
        session.updateStatus(AccountStatus.UNKNOWN_FREE.copy(englishEnabled = true))
        scheduler.advanceUntilIdle()
        assertTrue(settings.state.value.lastEnglishEnabled)
    }

    @Test
    fun pickerShowsOnlyOnANewInstallWithEnglishOn() {
        assertTrue(AppSettings().needsLanguagePick(true))
        assertFalse(AppSettings().needsLanguagePick(false))
        assertFalse(AppSettings(onboardingAccepted = true).needsLanguagePick(true))
        assertFalse(AppSettings(language = AppLanguage.TH).needsLanguagePick(true))
    }
}
