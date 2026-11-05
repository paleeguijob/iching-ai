package com.iching.ai.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.ProductKind
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.StoreProduct
import com.iching.ai.domain.model.currentOn
import com.iching.ai.domain.oracle.bangkokEpochDay
import com.iching.ai.domain.usecase.GetDailyHexagramUseCase
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.feature.paywall.AWAITING_PAYMENT_MESSAGE
import com.iching.ai.feature.paywall.PENDING_MESSAGE
import com.iching.ai.session.AdResult
import com.iching.ai.session.AppSession
import com.iching.ai.session.PurchaseResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock

/** A cast the home screen should open, with the question and category as they were when Start was tapped. */
data class CastRequest(val question: String, val category: QuestionCategory?)

enum class PreCastSheetKind { AD, UPSELL, READY }

/** The sheet shown before casting; [coins] and [premiumTrial] stay null until the store answers. */
data class PreCastSheetUi(
    val kind: PreCastSheetKind,
    val question: String,
    val adsLeft: Int = 0,
    val coins: StoreProduct? = null,
    val premiumTrial: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

private const val STATUS_WAIT_MILLIS = 1_500L
private const val PRODUCTS_FAILED_MESSAGE = "ยังโหลดแพ็กเกจจากร้านค้าไม่ได้ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่"

class HomeViewModel(
    private val getDaily: GetDailyHexagramUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val session: AppSession,
) : ViewModel() {

    private val _dailyHexagram = MutableStateFlow<Hexagram?>(null)
    private val today = MutableStateFlow(bangkokEpochDay(nowMillis()))

    /** A contemplation for today, the same for everyone on a given date. */
    val dailyHexagram: StateFlow<Hexagram?> = _dailyHexagram.asStateFlow()

    val streakDays: StateFlow<Int> = combine(observeSettings(), today) { settings, day -> settings.streak.currentOn(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val status: StateFlow<AccountStatus> = session.status

    private val _sheet = MutableStateFlow<PreCastSheetUi?>(null)

    /** The pre-cast offer on screen, or null. Lives here so it survives a trip to the paywall. */
    val sheet: StateFlow<PreCastSheetUi?> = _sheet.asStateFlow()

    private val _deciding = MutableStateFlow(false)

    /** True while the status is refreshed after a tap on Start; the button waits meanwhile. */
    val deciding: StateFlow<Boolean> = _deciding.asStateFlow()

    private val casts = Channel<CastRequest>(Channel.BUFFERED)

    /** One-shot: each request opens the casting screen once. */
    val castRequests: Flow<CastRequest> = casts.receiveAsFlow()

    /** The cast the open sheet is about. */
    private var pending: CastRequest? = null
    private var premiumTapped = false

    init {
        viewModelScope.launch { session.refreshStatus() }
        viewModelScope.launch {
            session.status.collect { s ->
                if (premiumTapped && s.premium) {
                    premiumTapped = false
                    session.log("quota_upsell_purchase", mapOf("option" to "premium", "placement" to "precast"))
                }
                // Bought or restored elsewhere (the paywall) while the sheet waited: one tap now starts the cast.
                _sheet.update { sh ->
                    if (sh != null && !sh.busy && sh.kind != PreCastSheetKind.READY && s.readingsLeft > 0) {
                        sh.copy(kind = PreCastSheetKind.READY, message = null)
                    } else {
                        sh
                    }
                }
            }
        }
    }

    // ---------- before casting ----------

    fun onStart(question: String, category: QuestionCategory?) {
        if (_deciding.value || _sheet.value != null) return
        _deciding.value = true
        viewModelScope.launch {
            try {
                // A stale status (yesterday's, or another device's) would offer the wrong thing.
                withTimeoutOrNull(STATUS_WAIT_MILLIS) { session.refreshStatus() }
                val request = CastRequest(question, category)
                when (preCastOffer(session.status.value, session.online.value)) {
                    PreCastOffer.NONE -> casts.send(request)
                    PreCastOffer.AD -> openSheet(request, PreCastSheetKind.AD)
                    PreCastOffer.UPSELL -> openSheet(request, PreCastSheetKind.UPSELL)
                }
            } finally {
                _deciding.value = false
            }
        }
    }

    private fun openSheet(request: CastRequest, kind: PreCastSheetKind) {
        pending = request
        _sheet.value = PreCastSheetUi(kind, request.question, adsLeft = session.status.value.adsWatchableLeft)
        session.log("precast_sheet_shown", mapOf("variant" to if (kind == PreCastSheetKind.AD) "ad" else "upsell"))
        if (kind == PreCastSheetKind.UPSELL) onUpsellShown()
    }

    private fun onUpsellShown() {
        session.log("quota_upsell_shown", mapOf("placement" to "precast"))
        viewModelScope.launch {
            val products = runCatching { session.products() }.getOrElse { emptyList() }
            _sheet.update {
                it?.copy(
                    coins = products.firstOrNull { p -> p.kind == ProductKind.COINS },
                    premiumTrial = products.firstOrNull { p -> p.kind == ProductKind.YEARLY }?.trialTh,
                )
            }
        }
    }

    fun onWatchAd() {
        val sh = _sheet.value ?: return
        if (sh.kind != PreCastSheetKind.AD || sh.busy) return
        session.log("precast_choice", mapOf("option" to "ad"))
        _sheet.value = sh.copy(busy = true, message = null)
        viewModelScope.launch {
            when (session.watchAdForReading()) {
                is AdResult.Granted -> castFromSheet()
                AdResult.NotCompleted -> settle("ต้องชมโฆษณาจนจบจึงจะได้รับสิทธิ์")
                is AdResult.Error -> settle("ยังไม่มีโฆษณาพร้อมแสดง โปรดลองใหม่อีกครู่ หรือเสี่ยงทายอ่านตัวบทไปก่อน")
                AdResult.LimitReached -> {
                    _sheet.update { it?.copy(kind = PreCastSheetKind.UPSELL, busy = false, message = "วันนี้ใช้สิทธิ์ชมโฆษณาครบแล้ว") }
                    if (_sheet.value != null) onUpsellShown()
                }
            }
        }
    }

    fun onBuyCoins() {
        val sh = _sheet.value ?: return
        if (sh.kind != PreCastSheetKind.UPSELL || sh.busy) return
        session.log("precast_choice", mapOf("option" to "coins"))
        session.log("quota_upsell_tap", mapOf("option" to "coins", "placement" to "precast"))
        _sheet.value = sh.copy(busy = true, message = null)
        viewModelScope.launch {
            // The price may not have loaded yet; the button works anyway, so look the pack up now.
            val coins = sh.coins ?: runCatching { session.products() }.getOrNull()?.firstOrNull { it.kind == ProductKind.COINS }
            if (coins == null) {
                settle(PRODUCTS_FAILED_MESSAGE)
                return@launch
            }
            when (session.purchase(coins)) {
                is PurchaseResult.Success -> {
                    session.log("quota_upsell_purchase", mapOf("option" to "coins", "placement" to "precast"))
                    castFromSheet()
                }
                PurchaseResult.Cancelled -> settle(null)
                PurchaseResult.Pending -> settle(PENDING_MESSAGE)
                PurchaseResult.AwaitingPayment -> settle(AWAITING_PAYMENT_MESSAGE)
                is PurchaseResult.Error -> settle("การชำระเงินไม่สำเร็จ โปรดลองใหม่อีกครั้ง")
            }
        }
    }

    fun onScriptureOnly() {
        val sh = _sheet.value ?: return
        if (sh.busy) return
        session.log("precast_choice", mapOf("option" to "scripture"))
        viewModelScope.launch { castFromSheet() }
    }

    /** Only logs: the screen opens the paywall, and the sheet waits underneath for READY. */
    fun onPremium() {
        val sh = _sheet.value ?: return
        if (sh.busy) return
        session.log("precast_choice", mapOf("option" to "premium"))
        if (sh.kind == PreCastSheetKind.UPSELL) {
            premiumTapped = true
            session.log("quota_upsell_tap", mapOf("option" to "premium", "placement" to "precast"))
        }
    }

    fun onCastFromReady() {
        if (_sheet.value?.kind != PreCastSheetKind.READY) return
        viewModelScope.launch { castFromSheet() }
    }

    /** Closing never casts, even if an ad or a purchase still finishes afterwards. */
    fun onSheetDismissed() {
        if (_sheet.value == null) return
        session.log("precast_choice", mapOf("option" to "dismiss"))
        _sheet.value = null
        pending = null
    }

    private fun settle(message: String?) = _sheet.update { it?.copy(busy = false, message = message) }

    private suspend fun castFromSheet() {
        val request = pending ?: return
        if (_sheet.value == null) return
        _sheet.value = null
        pending = null
        casts.send(request)
    }

    /** Runs on every resume too: the app may have stayed open past midnight. */
    fun refreshDay() {
        viewModelScope.launch {
            val now = nowMillis()
            today.value = bangkokEpochDay(now)
            _dailyHexagram.value = getDaily(now)
        }
    }

    private fun nowMillis() = Clock.System.now().toEpochMilliseconds()
}
