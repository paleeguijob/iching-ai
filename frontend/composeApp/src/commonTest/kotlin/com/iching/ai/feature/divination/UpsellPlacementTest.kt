package com.iching.ai.feature.divination

import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.ResolveCastUseCase
import com.iching.ai.domain.usecase.SaveReadingUseCase
import com.iching.ai.domain.usecase.TossCoinsUseCase
import com.iching.ai.platform.NativeAnalytics
import com.iching.ai.platform.NativeBridges
import com.iching.ai.session.AppSession
import com.iching.ai.session.FakeApi
import com.iching.ai.session.FakeAuth
import com.iching.ai.session.FakeJournal
import com.iching.ai.session.FakePurchases
import com.iching.ai.session.FirebaseApiSession
import com.iching.ai.session.NoAds
import com.iching.ai.session.NoNotifications
import com.iching.ai.session.NoSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private object NoHexagrams : HexagramRepository {
    private val h = Hexagram(1, "", "", "", "111111", emptyList(), "", "", "", List(6) { "" })
    override suspend fun all() = listOf(h)
    override suspend fun byId(id: Int) = h
    override suspend fun byPattern(pattern: String) = h
}

private class RecordingAnalytics : NativeAnalytics {
    val events = mutableListOf<Pair<String, Map<String, String>>>()
    override fun logEvent(name: String, params: Map<String, String>) {
        events += name to params
    }
    override fun setUserId(uid: String?) = Unit
    override fun recordError(message: String) = Unit
}

/** The after-cast upsell is told apart from the pre-cast sheet's by `placement`. */
@OptIn(ExperimentalCoroutinesApi::class)
class UpsellPlacementTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun afterCastUpsellEventsSayReading() {
        val analytics = RecordingAnalytics()
        val auth = FakeAuth()
        val api = FakeApi()
        val session = AppSession(
            NativeBridges(auth, FakePurchases(), NoAds, analytics, NoNotifications),
            FirebaseApiSession(auth), api, FakeJournal(), NoSettings, CoroutineScope(dispatcher),
        )
        val vm = DivinationViewModel(
            "", null, TossCoinsUseCase(), ResolveCastUseCase(NoHexagrams), SaveReadingUseCase(FakeJournal()),
            api, session, ObserveSettingsUseCase(NoSettings),
        )
        vm.onQuotaUpsellShown()
        vm.onUpsellPremium()
        val upsell = analytics.events.filter { it.first.startsWith("quota_upsell_") }
        assertEquals(listOf("quota_upsell_shown", "quota_upsell_tap"), upsell.map { it.first })
        upsell.forEach { assertEquals("reading", it.second["placement"], it.first) }
    }
}
