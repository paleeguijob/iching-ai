package com.iching.ai.feature.siamsi

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.domain.siamsi.drawsLeft
import com.iching.ai.domain.siamsi.grant
import com.iching.ai.platform.NativeAds
import com.iching.ai.platform.NativeException
import com.iching.ai.platform.awaitNative
import com.iching.ai.ui.toThaiNumerals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class DrawButton { WATCH_AD, SHAKE, PAYWALL, DISABLED }

data class DrawGateUi(val button: DrawButton, val line: String)

sealed interface DrawOutcome {
    data object Ready : DrawOutcome
    data object Dismissed : DrawOutcome
    data object LimitReached : DrawOutcome
}

private const val LIMIT_LINE = "วันนี้เสี่ยงครบแล้ว กลับมาใหม่พรุ่งนี้"
private const val PLACEMENT = "siamsi_draw"

/** The quota line and main button of the Siam Si home, per spec table 2.2. */
fun drawGateUi(premium: Boolean, left: Int, pending: Boolean, adUnavailable: Boolean, lastAdDismissed: Boolean): DrawGateUi = when {
    premium && (pending || left > 0) -> DrawGateUi(DrawButton.SHAKE, "วันนี้เหลือ ${left.toThaiNumerals()} ครั้ง")
    premium -> DrawGateUi(DrawButton.DISABLED, LIMIT_LINE)
    pending -> DrawGateUi(DrawButton.SHAKE, "ดูโฆษณาไว้แล้ว เสี่ยงได้เลย")
    left <= 0 -> DrawGateUi(DrawButton.PAYWALL, LIMIT_LINE)
    adUnavailable -> DrawGateUi(DrawButton.SHAKE, "ยังไม่มีโฆษณาในตอนนี้ เสี่ยงได้เลย")
    lastAdDismissed -> DrawGateUi(DrawButton.WATCH_AD, "ต้องดูโฆษณาจนจบจึงจะเสี่ยงได้")
    else -> DrawGateUi(DrawButton.WATCH_AD, "ดูโฆษณาสั้น ๆ ก่อนเสี่ยง · วันนี้เหลือ ${left.toThaiNumerals()} ครั้ง")
}

/**
 * Pays for each Siam Si draw: a rewarded ad for free users, nothing for Premium. Counted on the device,
 * apart from the I Ching ad credits (it never calls claimAdReward). A paid draw stays pending until its slip is confirmed.
 */
class SiamSiDrawGate(
    private val settings: SettingsRepository,
    private val ads: NativeAds,
    private val status: StateFlow<AccountStatus>,
    private val online: StateFlow<Boolean>,
    private val log: (String, Map<String, String>) -> Unit,
    private val today: () -> Long,
) {
    private val lastAdUnavailable = MutableStateFlow(false)
    private val lastAdDismissed = MutableStateFlow(false)
    private val mutex = Mutex()

    val ui: Flow<DrawGateUi> = combine(settings.settings, status, online, lastAdUnavailable, lastAdDismissed) { s, st, on, unavailable, dismissed ->
        val day = today()
        val usage = s.siamsiUsage.on(day)
        drawGateUi(st.premium, drawsLeft(usage, day, st.siamsi.drawsPerDay), usage.pendingDraw, unavailable || !on, dismissed)
    }

    suspend fun requestDraw(): DrawOutcome = mutex.withLock {
        val day = today()
        val usage = settings.settings.first().siamsiUsage.on(day)
        if (usage.pendingDraw) return DrawOutcome.Ready
        val st = status.value
        if (drawsLeft(usage, day, st.siamsi.drawsPerDay) <= 0) {
            log("siamsi_limit_reached", mapOf("tier" to if (st.premium) "premium" else "free"))
            return DrawOutcome.LimitReached
        }
        if (!st.premium && !watchAd()) return DrawOutcome.Dismissed
        settings.update { it.copy(siamsiUsage = it.siamsiUsage.grant(day)) }
        DrawOutcome.Ready
    }

    /** True when the draw may go ahead: the ad was watched to the end, or none could be shown. */
    private suspend fun watchAd(): Boolean {
        log("siamsi_ad_start", mapOf("ad_placement" to PLACEMENT))
        return try {
            val outcome = awaitNative { ads.showRewarded(it) }
            val rewarded = outcome == "rewarded"
            lastAdUnavailable.value = false
            lastAdDismissed.value = !rewarded
            log("siamsi_ad_result", mapOf("ad_placement" to PLACEMENT, "result" to if (rewarded) "rewarded" else "dismissed"))
            rewarded
        } catch (e: NativeException) {
            lastAdUnavailable.value = true
            lastAdDismissed.value = false
            log("siamsi_ad_unavailable", mapOf("ad_placement" to PLACEMENT, "result" to "unavailable"))
            true
        } finally {
            ads.preload()
        }
    }
}
