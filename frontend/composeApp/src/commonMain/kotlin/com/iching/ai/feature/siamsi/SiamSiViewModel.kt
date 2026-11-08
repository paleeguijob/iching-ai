package com.iching.ai.feature.siamsi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.FollowUpTurn
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.ProductKind
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.repository.DivinationEvent
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.JournalRepository
import com.iching.ai.domain.repository.LimitReason
import com.iching.ai.domain.repository.OracleApi
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.domain.repository.SiamSiRepository
import com.iching.ai.domain.safety.crisisMessage
import com.iching.ai.domain.siamsi.PuaProgress
import com.iching.ai.domain.siamsi.PuaResult
import com.iching.ai.domain.siamsi.PuaRound
import com.iching.ai.domain.siamsi.SiamSiPick
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiStick
import com.iching.ai.domain.siamsi.consumePending
import com.iching.ai.domain.siamsi.drawStick
import com.iching.ai.domain.siamsi.tossPua
import com.iching.ai.feature.divination.FollowUpUi
import com.iching.ai.feature.home.PreCastOffer
import com.iching.ai.feature.home.PreCastSheetKind
import com.iching.ai.feature.home.PreCastSheetUi
import com.iching.ai.feature.home.preCastOffer
import com.iching.ai.feature.paywall.AWAITING_PAYMENT_MESSAGE
import com.iching.ai.feature.paywall.PENDING_MESSAGE
import com.iching.ai.navigation.SiamSiRoute
import com.iching.ai.session.AdResult
import com.iching.ai.session.AppSession
import com.iching.ai.session.PurchaseResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random
import kotlin.time.Clock

sealed interface SiamSiStage {
    data object Loading : SiamSiStage

    /** No draw was paid for (the screen was restored after its slip was spent): the screen goes back. */
    data object Unpaid : SiamSiStage
    data object Shaking : SiamSiStage

    /** A stick has fallen; the pua toss decides whether it stands. */
    data class Fallen(val number: Int) : SiamSiStage

    /** The last toss did not confirm its stick: shake again. */
    data class Pua(val progress: PuaProgress) : SiamSiStage
    data class Slip(val stick: SiamSiStick, val autoConfirmed: Boolean) : SiamSiStage

    /** The question signals a crisis: the hotlines instead of a draw, with no ad and no quota. */
    data class Safety(val message: String) : SiamSiStage
}

data class InterpretUi(
    val text: String = "",
    val streaming: Boolean = false,
    val sheet: PreCastSheetUi? = null,
    val message: String? = null,
    /** A crisis question: this caring message shows instead of an interpretation. */
    val safety: String? = null,
    val followUps: List<FollowUpUi> = emptyList(),
    val followUpBusy: Boolean = false,
    /** The interpretation was reported as unsuitable. */
    val reported: Boolean = false,
) {
    val done: Boolean get() = text.isNotEmpty() && !streaming
}

const val OFFLINE_INTERPRET_MESSAGE = "ต้องเชื่อมต่ออินเทอร์เน็ตเพื่อแปลความหมาย"

/** Shake, toss the pua, read the slip, and (for one reading credit) ask the hermit to interpret it. */
class SiamSiViewModel(
    private val route: SiamSiRoute,
    private val repository: SiamSiRepository,
    private val api: OracleApi,
    private val session: AppSession,
    private val journal: JournalRepository,
    private val settings: SettingsRepository,
    private val random: Random,
    private val today: () -> Long,
) : ViewModel() {
    val category: QuestionCategory? = QuestionCategory.fromWire(route.category)

    private val _stage = MutableStateFlow<SiamSiStage>(SiamSiStage.Loading)
    val stage: StateFlow<SiamSiStage> = _stage.asStateFlow()

    private val _interpret = MutableStateFlow(InterpretUi())
    val interpret: StateFlow<InterpretUi> = _interpret.asStateFlow()

    val status: StateFlow<AccountStatus> = session.status

    val soundEnabled: StateFlow<Boolean> = settings.settings.map { it.soundEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    var set: SiamSiSet? = null
        private set
    val question: String get() = route.question

    private var rounds = emptyList<PuaRound>()

    /** How the blocks landed on the last toss, for the moment before the slip opens. */
    val lastToss: PuaResult? get() = rounds.lastOrNull()?.result
    private var saved: Reading? = null
    private val saveLock = Mutex()
    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            crisisMessage(route.question)?.let {
                _stage.value = SiamSiStage.Safety(it)
                return@launch
            }
            val loaded = repository.set(route.setId)
            set = loaded
            route.journalId?.let { id ->
                _stage.value = reopen(id, loaded) ?: SiamSiStage.Unpaid
                return@launch
            }
            val paid = settings.settings.first().siamsiUsage.on(today()).pendingDraw
            _stage.value = if (loaded != null && paid) SiamSiStage.Shaking else SiamSiStage.Unpaid
        }
        // Bought or restored on the paywall while the sheet waited: one tap now interprets.
        viewModelScope.launch {
            session.status.collect { s ->
                _interpret.update { ui ->
                    val sh = ui.sheet
                    if (sh != null && !sh.busy && sh.kind != PreCastSheetKind.READY && s.readingsLeft > 0) {
                        ui.copy(sheet = sh.copy(kind = PreCastSheetKind.READY, message = null))
                    } else {
                        ui
                    }
                }
            }
        }
    }

    /** A slip saved earlier, opened from the journal to be interpreted (resuming on its server reading). */
    private suspend fun reopen(id: Long, loaded: SiamSiSet?): SiamSiStage.Slip? {
        val reading = journal.get(id) ?: return null
        val pick = reading.siamsi ?: return null
        val stick = loaded?.sticks?.firstOrNull { it.number == pick.number } ?: return null
        saved = reading
        return SiamSiStage.Slip(stick, autoConfirmed = false)
    }

    // ---------- shake and pua ----------

    fun onShakeStarted() {
        if (_stage.value == SiamSiStage.Shaking) session.log("siamsi_shake_start", mapOf("set" to route.setId))
    }

    /** Enough shaking: the stick is chosen now, before the falling animation plays. */
    fun onShaken() {
        val s = set ?: return
        if (_stage.value != SiamSiStage.Shaking) return
        val number = pickStick(s)
        rounds = rounds + PuaRound(number, null)
        _stage.value = SiamSiStage.Fallen(number)
        session.log("siamsi_stick_drawn", mapOf("set" to s.id, "number" to number.toString(), "round" to rounds.size.toString()))
    }

    /** Uniform over the set; a set file still being written may hold fewer sticks than its count. */
    private fun pickStick(s: SiamSiSet): Int =
        if (s.sticks.size == s.count) drawStick(s.count, random) else s.sticks[random.nextInt(s.sticks.size)].number

    fun onTossPua() {
        val s = set ?: return
        val fallen = _stage.value as? SiamSiStage.Fallen ?: return
        val result = tossPua(random)
        rounds = rounds.dropLast(1) + PuaRound(fallen.number, result)
        val progress = PuaProgress(rounds)
        session.log("siamsi_pua", mapOf("result" to result.name.lowercase(), "round" to rounds.size.toString()))
        val confirmed = progress.confirmedStick
        if (confirmed == null) {
            _stage.value = SiamSiStage.Pua(progress)
            return
        }
        val stick = s.stick(confirmed)
        viewModelScope.launch {
            // Check and spend the paid draw in one step: a second screen on the same draw gets no slip.
            var paid = false
            settings.update {
                val usage = it.siamsiUsage.on(today())
                paid = usage.pendingDraw
                if (paid) it.copy(siamsiUsage = usage.consumePending()) else it
            }
            if (!paid) {
                _stage.value = SiamSiStage.Unpaid
                return@launch
            }
            _stage.value = SiamSiStage.Slip(stick, progress.autoConfirmed)
            session.log(
                "siamsi_slip_view",
                mapOf("set" to s.id, "number" to confirmed.toString(), "level" to stick.level.wire, "auto_confirmed" to progress.autoConfirmed.toString()),
            )
            persist()
        }
    }

    /** A failed pua: shake again, with no new ad and no new draw. */
    fun onReshake() {
        val pua = _stage.value as? SiamSiStage.Pua ?: return
        if (pua.progress.needsReshake) _stage.value = SiamSiStage.Shaking
    }

    // ---------- interpretation ----------

    fun onInterpret() {
        val slip = _stage.value as? SiamSiStage.Slip ?: return
        if (_interpret.value.streaming || _interpret.value.sheet != null) return
        val status = session.status.value
        session.log("siamsi_interpret_tap", mapOf("set" to route.setId, "source" to paidBy(status)))
        if (!session.online.value) {
            _interpret.update { it.copy(message = OFFLINE_INTERPRET_MESSAGE) }
            return
        }
        when (preCastOffer(status, online = true)) {
            PreCastOffer.NONE -> stream(slip.stick)
            PreCastOffer.AD -> openSheet(PreCastSheetKind.AD)
            PreCastOffer.UPSELL -> openSheet(PreCastSheetKind.UPSELL)
        }
    }

    private fun paidBy(s: AccountStatus) = when {
        s.premium -> "premium"
        s.includedLeft > 0 -> "daily"
        s.adCreditsLeft > 0 -> "ad"
        s.coins > 0 -> "coin"
        s.adsWatchableLeft > 0 -> "ad_offer"
        else -> "upsell"
    }

    private fun stream(stick: SiamSiStick) {
        if (streamJob?.isActive == true) return
        _interpret.update { it.copy(text = "", streaming = true, message = null, safety = null, sheet = null) }
        val pick = SiamSiPick(route.setId, stick.number)
        streamJob = viewModelScope.launch {
            val text = StringBuilder()
            try {
                api.interpretSiamSi(pick, route.question, category, saved?.serverId).collect { event -> onEvent(event, text) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _interpret.update { it.copy(message = failureMessage(FailureReason.NETWORK)) }
            }
            _interpret.update { it.copy(streaming = false) }
        }
    }

    private suspend fun onEvent(event: DivinationEvent, text: StringBuilder) {
        when (event) {
            is DivinationEvent.Created -> persist { it.copy(serverId = event.readingId) }
            is DivinationEvent.Delta -> {
                text.append(event.text)
                _interpret.update { it.copy(text = text.toString()) }
            }
            is DivinationEvent.Done -> {
                session.updateStatus(event.status)
                val words = text.toString().trim()
                _interpret.update { it.copy(text = words, streaming = false) }
                persist { it.copy(interpretation = words, source = InterpretationSource.AI) }
            }
            is DivinationEvent.Limited -> {
                session.updateStatus(event.status)
                if (event.reason == LimitReason.QUOTA) {
                    _interpret.update { it.copy(text = "", streaming = false) }
                    openSheet(if (session.status.value.adsWatchableLeft > 0) PreCastSheetKind.AD else PreCastSheetKind.UPSELL)
                } else {
                    _interpret.update { it.copy(text = "", message = limitMessage(event.reason, event.retryAfterSeconds)) }
                }
            }
            is DivinationEvent.Safety -> _interpret.update { it.copy(text = "", safety = event.message) }
            is DivinationEvent.Failed -> _interpret.update { it.copy(text = "", message = failureMessage(event.reason)) }
            is DivinationEvent.Cast -> Unit
        }
    }

    private fun limitMessage(reason: LimitReason, retryAfter: Int?) = when (reason) {
        LimitReason.RATE_LIMITED -> "โปรดเว้นจังหวะหายใจสักครู่${retryAfter?.let { " (${it} วินาที)" } ?: ""} แล้วจึงขอให้นักพรตแปลอีกครั้ง"
        LimitReason.AI_DISABLED -> "นักพรตปลีกวิเวกชั่วคราว ท่านยังอ่านใบเซียมซีด้านบนได้ โปรดกลับมาใหม่ภายหลัง"
        else -> "คำแปลจากนักพรตสำหรับวันนี้ครบแล้ว ท่านยังอ่านใบเซียมซีด้านบนได้เสมอ"
    }

    private fun failureMessage(reason: FailureReason) = when (reason) {
        FailureReason.NETWORK -> "การเชื่อมต่อขาดหาย แตะแปลความหมายอีกครั้งได้ โดยไม่เสียสิทธิ์ซ้ำ"
        FailureReason.BLOCKED -> "คำถามนี้ไม่อาจแปลความได้ สิทธิ์ของท่านยังไม่ถูกใช้"
        FailureReason.UNAUTHENTICATED -> "ยืนยันตัวตนกับระบบไม่สำเร็จ โปรดลองใหม่อีกครั้ง"
        else -> "การติดต่อนักพรตขัดข้องชั่วคราว สิทธิ์ของท่านยังไม่ถูกใช้ โปรดลองใหม่อีกครั้ง"
    }

    // ---------- the sheet that pays for an interpretation ----------

    private fun openSheet(kind: PreCastSheetKind) {
        _interpret.update { it.copy(sheet = PreCastSheetUi(kind, route.question, adsLeft = session.status.value.adsWatchableLeft)) }
        session.log("precast_sheet_shown", mapOf("variant" to if (kind == PreCastSheetKind.AD) "ad" else "upsell", "placement" to "siamsi"))
        if (kind == PreCastSheetKind.UPSELL) {
            viewModelScope.launch {
                val products = runCatching { session.products() }.getOrElse { emptyList() }
                updateSheet {
                    it.copy(
                        coins = products.firstOrNull { p -> p.kind == ProductKind.COINS },
                        premiumTrial = products.firstOrNull { p -> p.kind == ProductKind.YEARLY }?.trialTh,
                    )
                }
            }
        }
    }

    private fun updateSheet(change: (PreCastSheetUi) -> PreCastSheetUi) = _interpret.update { ui -> ui.copy(sheet = ui.sheet?.let(change)) }

    private fun streamFromSheet() {
        val slip = _stage.value as? SiamSiStage.Slip ?: return
        _interpret.update { it.copy(sheet = null) }
        stream(slip.stick)
    }

    fun onWatchAd() {
        val sh = _interpret.value.sheet ?: return
        if (sh.kind != PreCastSheetKind.AD || sh.busy) return
        updateSheet { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            when (session.watchAdForReading()) {
                is AdResult.Granted -> streamFromSheet()
                AdResult.NotCompleted -> updateSheet { it.copy(busy = false, message = "ต้องชมโฆษณาจนจบจึงจะได้รับสิทธิ์") }
                is AdResult.Error -> updateSheet { it.copy(busy = false, message = "ยังไม่มีโฆษณาพร้อมแสดง โปรดลองใหม่อีกครู่") }
                AdResult.LimitReached -> {
                    _interpret.update { it.copy(sheet = null) }
                    openSheet(PreCastSheetKind.UPSELL)
                    updateSheet { it.copy(message = "วันนี้ใช้สิทธิ์ชมโฆษณาครบแล้ว") }
                }
            }
        }
    }

    fun onBuyCoins() {
        val sh = _interpret.value.sheet ?: return
        if (sh.kind != PreCastSheetKind.UPSELL || sh.busy) return
        session.log("quota_upsell_tap", mapOf("option" to "coins", "placement" to "siamsi"))
        updateSheet { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val coins = sh.coins ?: runCatching { session.products() }.getOrNull()?.firstOrNull { it.kind == ProductKind.COINS }
            if (coins == null) {
                updateSheet { it.copy(busy = false, message = "ยังโหลดแพ็กเกจจากร้านค้าไม่ได้ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่") }
                return@launch
            }
            when (session.purchase(coins)) {
                is PurchaseResult.Success -> streamFromSheet()
                PurchaseResult.Cancelled -> updateSheet { it.copy(busy = false) }
                PurchaseResult.Pending -> updateSheet { it.copy(busy = false, message = PENDING_MESSAGE) }
                PurchaseResult.AwaitingPayment -> updateSheet { it.copy(busy = false, message = AWAITING_PAYMENT_MESSAGE) }
                is PurchaseResult.Error -> updateSheet { it.copy(busy = false, message = "การชำระเงินไม่สำเร็จ โปรดลองใหม่อีกครั้ง") }
            }
        }
    }

    /** Only logs: the screen opens the paywall, and the sheet waits underneath for READY. */
    fun onPremium() {
        session.log("quota_upsell_tap", mapOf("option" to "premium", "placement" to "siamsi"))
    }

    fun onInterpretFromReady() {
        if (_interpret.value.sheet?.kind == PreCastSheetKind.READY) streamFromSheet()
    }

    fun onSheetDismissed() = _interpret.update { it.copy(sheet = null) }

    fun onMessageShown() = _interpret.update { it.copy(message = null) }

    // ---------- follow-up (Premium) ----------

    fun onSendFollowUp(message: String) {
        val id = saved?.serverId ?: return
        val ui = _interpret.value
        val text = message.trim().take(300)
        val left = session.status.value.followUpsPerReading - ui.followUps.size
        if (text.isEmpty() || ui.followUpBusy || !ui.done || left <= 0) return
        _interpret.update { it.copy(followUpBusy = true, followUps = it.followUps + FollowUpUi(text, "", streaming = true), message = null) }
        viewModelScope.launch {
            val answer = StringBuilder()
            fun setAnswer(value: String, streaming: Boolean) =
                _interpret.update { it.copy(followUps = it.followUps.dropLast(1) + FollowUpUi(text, value, streaming)) }
            fun drop(reason: String) = _interpret.update { it.copy(followUps = it.followUps.dropLast(1), message = reason) }
            try {
                api.followUp(id, text).collect { event ->
                    when (event) {
                        is DivinationEvent.Delta -> {
                            answer.append(event.text)
                            setAnswer(answer.toString(), true)
                        }
                        is DivinationEvent.Done -> setAnswer(answer.toString().trim(), false)
                        is DivinationEvent.Safety -> setAnswer(event.message, false)
                        is DivinationEvent.Limited -> drop("ถามต่อครบจำนวนครั้งของคำแปลนี้แล้ว")
                        is DivinationEvent.Failed -> drop(
                            if (event.reason == FailureReason.PREMIUM_REQUIRED) "การถามต่อเป็นสิทธิ์ของสมาชิก Premium" else "นักพรตยังไม่อาจตอบได้ในตอนนี้ โปรดลองใหม่อีกครั้ง",
                        )
                        is DivinationEvent.Created, is DivinationEvent.Cast -> Unit
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                drop("ไม่สามารถเชื่อมต่ออินเทอร์เน็ตได้")
            }
            _interpret.update { st -> st.copy(followUpBusy = false, followUps = st.followUps.map { if (it.streaming) it.copy(streaming = false) else it }) }
            persist { r ->
                r.copy(followUps = _interpret.value.followUps.filter { it.answer.isNotBlank() }.map { FollowUpTurn(it.question, it.answer) })
            }
        }
    }

    /** Reports the AI interpretation (store rules for generated content). */
    fun onReport(reason: String) {
        val id = saved?.serverId ?: return
        if (_interpret.value.reported) return
        viewModelScope.launch {
            val ok = runCatching { api.report(id, reason, "") }.isSuccess
            _interpret.update {
                it.copy(reported = ok, message = if (ok) "ขอบคุณ เราได้รับรายงานแล้วและจะตรวจสอบ" else "ส่งรายงานไม่สำเร็จ โปรดลองใหม่")
            }
            if (ok) session.log("report_sent", mapOf("reason" to reason, "placement" to "siamsi"))
        }
    }

    fun logEvent(name: String, params: Map<String, String> = emptyMap()) = session.log(name, params)

    // ---------- journal ----------

    /** The slip goes into the journal once, the moment it is confirmed; later changes update that row. */
    private suspend fun persist(change: (Reading) -> Reading = { it }) = saveLock.withLock {
        val slip = _stage.value as? SiamSiStage.Slip ?: return@withLock
        val base = saved ?: Reading(
            question = route.question,
            createdAtMillis = Clock.System.now().toEpochMilliseconds(),
            lines = emptyList(),
            primaryId = 0,
            relatingId = null,
            interpretation = "",
            source = InterpretationSource.SCRIPTURE,
            category = category,
            siamsi = SiamSiPick(route.setId, slip.stick.number),
        )
        val next = change(base)
        if (saved != null && next == saved) return@withLock
        val id = journal.save(next)
        saved = next.copy(id = id)
    }
}
