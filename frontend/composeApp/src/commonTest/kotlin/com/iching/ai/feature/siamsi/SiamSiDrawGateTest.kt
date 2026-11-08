package com.iching.ai.feature.siamsi

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.SiamSiStatus
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.domain.siamsi.SiamSiUsage
import com.iching.ai.platform.NativeAds
import com.iching.ai.platform.ResultCallback
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class MemorySettings(initial: AppSettings = AppSettings()) : SettingsRepository {
    val flow = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = flow
    override suspend fun update(transform: (AppSettings) -> AppSettings) = flow.update(transform)
    override suspend fun clear() {
        flow.value = AppSettings()
    }
}

/** An ad that ends with [answer] ("rewarded" / "dismissed"); null fails to load. Held ads finish on [finish]. */
private class ScriptedAds(var answer: String? = "rewarded", private val hold: Boolean = false) : NativeAds {
    var shown = 0
    var preloads = 0
    private var pending: ResultCallback? = null
    override fun showRewarded(callback: ResultCallback) {
        shown++
        if (hold) pending = callback else reply(callback)
    }
    fun finish() = pending!!.let { pending = null; reply(it) }
    private fun reply(callback: ResultCallback) = answer?.let { callback.onResult(it, null) } ?: callback.onResult(null, "no fill")
    override fun preload() {
        preloads++
    }
}

class SiamSiDrawGateTest {
    private val today = 20_400L
    private val events = mutableListOf<Pair<String, Map<String, String>>>()

    private fun status(premium: Boolean) = AccountStatus.UNKNOWN_FREE.copy(
        premium = premium,
        adCreditsLeft = 0,
        siamsi = SiamSiStatus(enabled = true, drawsPerDay = if (premium) 10 else 1),
    )

    private fun gate(
        settings: MemorySettings,
        ads: NativeAds,
        premium: Boolean = false,
        online: Boolean = true,
    ) = SiamSiDrawGate(
        settings = settings,
        ads = ads,
        status = MutableStateFlow(status(premium)),
        online = MutableStateFlow(online),
        log = { name, params -> events += name to params },
        today = { today },
    )

    private fun usage(settings: MemorySettings) = settings.flow.value.siamsiUsage

    @Test
    fun everyRowOfTheQuotaTable() {
        assertEquals(DrawGateUi(DrawButton.WATCH_AD, "ดูโฆษณาสั้น ๆ ก่อนเสี่ยง · วันนี้เหลือ ๑ ครั้ง"), drawGateUi(false, 1, false, false, false))
        assertEquals(DrawGateUi(DrawButton.WATCH_AD, "ต้องดูโฆษณาจนจบจึงจะเสี่ยงได้"), drawGateUi(false, 1, false, false, true))
        assertEquals(DrawGateUi(DrawButton.SHAKE, "ยังไม่มีโฆษณาในตอนนี้ เสี่ยงได้เลย"), drawGateUi(false, 1, false, true, false))
        assertEquals(DrawGateUi(DrawButton.PAYWALL, "วันนี้เสี่ยงครบแล้ว กลับมาใหม่พรุ่งนี้"), drawGateUi(false, 0, false, false, false))
        assertEquals(DrawGateUi(DrawButton.SHAKE, "วันนี้เหลือ ๑๐ ครั้ง"), drawGateUi(true, 10, false, false, false))
        assertEquals(DrawGateUi(DrawButton.DISABLED, "วันนี้เสี่ยงครบแล้ว กลับมาใหม่พรุ่งนี้"), drawGateUi(true, 0, false, false, false))
        // A draw already paid for is never asked for again, even with nothing left today.
        assertEquals(DrawButton.SHAKE, drawGateUi(false, 0, true, false, false).button)
        assertEquals(DrawButton.SHAKE, drawGateUi(true, 0, true, false, false).button)
    }

    @Test
    fun rewardedAdGrantsAPendingDraw() = runTest {
        val settings = MemorySettings()
        val ads = ScriptedAds("rewarded")
        assertEquals(DrawOutcome.Ready, gate(settings, ads).requestDraw())
        assertEquals(SiamSiUsage(today, used = 1, pendingDraw = true), usage(settings))
        assertEquals(1, ads.preloads)
        assertEquals(listOf("siamsi_ad_start", "siamsi_ad_result"), events.map { it.first })
        assertEquals("rewarded", events.last().second["result"])
        assertEquals("siamsi_draw", events.first().second["ad_placement"])
    }

    @Test
    fun dismissedAdCountsNothing() = runTest {
        val settings = MemorySettings()
        val gate = gate(settings, ScriptedAds("dismissed"))
        assertEquals(DrawOutcome.Dismissed, gate.requestDraw())
        assertEquals(0, usage(settings).used)
        assertEquals("ต้องดูโฆษณาจนจบจึงจะเสี่ยงได้", gate.ui.first().line)
    }

    @Test
    fun unavailableAdStillDrawsAndCounts() = runTest {
        val settings = MemorySettings()
        assertEquals(DrawOutcome.Ready, gate(settings, ScriptedAds(answer = null)).requestDraw())
        assertEquals(SiamSiUsage(today, used = 1, pendingDraw = true), usage(settings))
        assertEquals("siamsi_ad_unavailable", events.last().first)
    }

    @Test
    fun offlineShowsTheNoAdLine() = runTest {
        val ui = gate(MemorySettings(), ScriptedAds(), online = false).ui.first()
        assertEquals(DrawGateUi(DrawButton.SHAKE, "ยังไม่มีโฆษณาในตอนนี้ เสี่ยงได้เลย"), ui)
    }

    @Test
    fun noDrawsLeftNeverPlaysAnAd() = runTest {
        val settings = MemorySettings(AppSettings(siamsiUsage = SiamSiUsage(today, used = 1)))
        val ads = ScriptedAds()
        val gate = gate(settings, ads)
        assertEquals(DrawOutcome.LimitReached, gate.requestDraw())
        assertEquals(0, ads.shown)
        assertEquals("siamsi_limit_reached", events.single().first)
        assertEquals(DrawButton.PAYWALL, gate.ui.first().button)
    }

    @Test
    fun yesterdaysUseDoesNotCountToday() = runTest {
        val settings = MemorySettings(AppSettings(siamsiUsage = SiamSiUsage(today - 1, used = 1, pendingDraw = true)))
        val ads = ScriptedAds()
        assertEquals(DrawOutcome.Ready, gate(settings, ads).requestDraw())
        assertEquals(1, ads.shown)
        assertEquals(SiamSiUsage(today, used = 1, pendingDraw = true), usage(settings))
    }

    @Test
    fun premiumNeverPlaysAnAd() = runTest {
        val settings = MemorySettings()
        val ads = ScriptedAds()
        val gate = gate(settings, ads, premium = true)
        assertEquals(DrawOutcome.Ready, gate.requestDraw())
        assertEquals(0, ads.shown)
        assertEquals(SiamSiUsage(today, used = 1, pendingDraw = true), usage(settings))
        assertEquals("วันนี้เหลือ ๙ ครั้ง", gate.ui.first().line)
    }

    @Test
    fun pendingDrawSkipsTheAd() = runTest {
        val settings = MemorySettings(AppSettings(siamsiUsage = SiamSiUsage(today, used = 1, pendingDraw = true)))
        val ads = ScriptedAds()
        assertEquals(DrawOutcome.Ready, gate(settings, ads).requestDraw())
        assertEquals(0, ads.shown)
        assertEquals(1, usage(settings).used)
    }

    @Test
    fun doubleTapPlaysOneAdAndCountsOneDraw() = runTest {
        val settings = MemorySettings()
        val ads = ScriptedAds("rewarded", hold = true)
        val gate = gate(settings, ads)
        val first = async { gate.requestDraw() }
        val second = async { gate.requestDraw() }
        runCurrent()
        ads.finish()
        assertEquals(DrawOutcome.Ready, first.await())
        assertEquals(DrawOutcome.Ready, second.await())
        assertEquals(1, ads.shown)
        assertEquals(1, usage(settings).used)
    }
}
